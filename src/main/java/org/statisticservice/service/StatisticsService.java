package org.statisticservice.service;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.statisticservice.enumeration.CountryCode;
import org.statisticservice.model.CountryStatistics;
import org.statisticservice.model.PageViewUpdateRequest;
import org.statisticservice.repository.StatisticsRepository;
import org.statisticservice.repository.dto.CountryStatisticsDto;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final String STATISTICS_KEY = "statistics:%s:page:%s";
    private static final String TOP_STATISTICS_KEY = "top_statistics:%s";
    private static final Integer CACHE_LIFE_DURATION = 60;
    private static final Integer CACHE_MAX_SIZE = 100000;
    private final Cache<String, CountryStatisticsDto> cache;
    private final StatisticsRepository statisticsRepository;

    public StatisticsService(StatisticsRepository statisticsRepository) {
        this.statisticsRepository = statisticsRepository;
        this.cache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_LIFE_DURATION, TimeUnit.MINUTES)
                .maximumSize(CACHE_MAX_SIZE)
                .build();
    }

    public Mono<Void> updateStatistics(String countryCode, PageViewUpdateRequest request) {
        String upperCaseCountryCode = countryCode.toUpperCase();
        return validateRequest(countryCode, request)
                .then(Mono.zip(
                        findStatisticsByKey(String.format(STATISTICS_KEY, upperCaseCountryCode, request.getPageName()))
                                .defaultIfEmpty(new CountryStatisticsDto(upperCaseCountryCode, request.getPageName(), 0L)),
                        findStatisticsByKey(String.format(TOP_STATISTICS_KEY, upperCaseCountryCode))
                                .defaultIfEmpty(new CountryStatisticsDto(upperCaseCountryCode, request.getPageName(), 0L))
                ))
                .flatMap(tuple -> {
                    CountryStatisticsDto currentStats = tuple.getT1();
                    CountryStatisticsDto topStats = tuple.getT2();

                    boolean shouldUpdateTop = currentStats.getViews() + request.getViewsToAdd() > topStats.getViews();
                    currentStats.setViews(currentStats.getViews() + request.getViewsToAdd());

                    Mono<Void> updateCurrentStatsMono = updateStatistics(
                            String.format(STATISTICS_KEY, upperCaseCountryCode, request.getPageName()), currentStats).then();

                    Mono<Void> updateTopStatsMono = shouldUpdateTop
                            ? updateStatistics(String.format(TOP_STATISTICS_KEY, upperCaseCountryCode), currentStats).then()
                            : Mono.empty();
                    return Mono.when(updateCurrentStatsMono, updateTopStatsMono);
                });
    }

    // todo не успел полностью все сценарии и что в итоге работает быстрее :(
    public Mono<Map<String, CountryStatistics>> getTopViewsStatistics() {
        //   return getTopViewsStatisticsFromCache();
        return statisticsRepository.findTopViewedPagesStatistics()
                .map(list -> list.stream()
                        .collect(Collectors.toMap(
                                CountryStatisticsDto::getCountry,
                                dto -> new CountryStatistics(dto.getViews(), dto.getMostPopularPage()),
                                (existing, replacement) -> existing
                        )));
    }

    private Mono<Map<String, CountryStatistics>> getTopViewsStatisticsFromCache() {
        List<String> keys = Arrays.stream(CountryCode.values())
                .map(code -> String.format(TOP_STATISTICS_KEY, code.name()))
                .collect(Collectors.toList());
        Map<String, CountryStatistics> statisticsMap = keys.stream()
                .map(key -> Optional.ofNullable(cache.getIfPresent(key))
                        .map(dto -> new AbstractMap.SimpleEntry<>(key, new CountryStatistics(dto.getViews(), dto.getMostPopularPage())))
                )
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (statisticsMap.size() > 0) {
            return Mono.just(statisticsMap);
        }
        return statisticsRepository.findTopViewedPagesStatistics()
                .doOnNext(list -> list.forEach(dto -> cache.put(String.format(TOP_STATISTICS_KEY, dto.getCountry()), dto)))
                .map(list -> list.stream()
                        .collect(Collectors.toMap(
                                CountryStatisticsDto::getCountry,
                                dto -> new CountryStatistics(dto.getViews(), dto.getMostPopularPage()),
                                (existing, replacement) -> existing
                        )));
    }

    private Mono<CountryStatisticsDto> findStatisticsByKey(String key) {
        return Mono.justOrEmpty(cache.getIfPresent(key))
                .switchIfEmpty(
                        statisticsRepository.findStatisticsByKey(key)
                                .doOnSuccess(result -> Optional.ofNullable(result)
                                        .ifPresent(value -> cache.put(key, value)))
                );

    }

    private Mono<Boolean> updateStatistics(String key, CountryStatisticsDto statistics) {
        return statisticsRepository.updateStatisticsByKey(key, statistics)
                .doOnSuccess(result -> Optional.ofNullable(result)
                        .filter(Boolean::booleanValue)
                        .ifPresent(r -> cache.put(key, statistics)));
    }

    private Mono<Void> validateRequest(String code, PageViewUpdateRequest request) {
        return validateCountryCode(code)
                .then(validatePageName(request.getPageName()))
                .then(validateViewsToAdd(request.getViewsToAdd()));
    }

    private Mono<Void> validateCountryCode(String code) {
        boolean isValidCode = Arrays.stream(CountryCode.values())
                .anyMatch(countryCode -> countryCode.name().equalsIgnoreCase(code));
        if (!isValidCode) {
            return Mono.error(new IllegalArgumentException("Invalid country code: " + code));
        }
        return Mono.empty();
    }

    private Mono<Void> validatePageName(String pageName) {
        if (StringUtils.isBlank(pageName) || !pageName.matches("[a-zA-Z0-9_-]+")) {
            return Mono.error(new IllegalArgumentException("Invalid page name: " + pageName));
        }
        return Mono.empty();
    }

    private Mono<Void> validateViewsToAdd(long viewsToAdd) {
        if (viewsToAdd <= 0) {
            return Mono.error(new IllegalArgumentException("Views to add must be greater than 0: " + viewsToAdd));
        }
        return Mono.empty();
    }

}
