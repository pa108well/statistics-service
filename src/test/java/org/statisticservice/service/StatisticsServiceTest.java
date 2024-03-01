package org.statisticservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.statisticservice.model.PageViewUpdateRequest;
import org.statisticservice.repository.StatisticsRepository;
import org.statisticservice.repository.dto.CountryStatisticsDto;
import org.statisticservice.repository.exceptions.DataAccessException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {
    @Mock
    private StatisticsRepository statisticsRepository;

    private StatisticsService statisticsService;
    private final String countryCode = "US";
    private final PageViewUpdateRequest validRequest = new PageViewUpdateRequest("pageName", 10L);
    private final CountryStatisticsDto currentStats = new CountryStatisticsDto(countryCode, "pageName", 20L);
    private final CountryStatisticsDto topStats = new CountryStatisticsDto(countryCode, "otherPage", 15L);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        statisticsService = new StatisticsService(statisticsRepository);
        commonMockSetup();
    }

    private void commonMockSetup() {
        when(statisticsRepository.findStatisticsByKey(anyString()))
                .thenReturn(Mono.just(currentStats), Mono.just(topStats));
    }

    @Test
    void updateStatisticsWithValidRequestAndSuccessfulUpdate() {
        when(statisticsRepository.updateStatisticsByKey(anyString(), any(CountryStatisticsDto.class)))
                .thenReturn(Mono.just(true));

        Mono<Void> result = statisticsService.updateStatistics(countryCode, validRequest);

        StepVerifier.create(result).verifyComplete();

        Mono<Void> result2 = statisticsService.updateStatistics(countryCode, validRequest);

        StepVerifier.create(result2).verifyComplete();
        verify(statisticsRepository, times(4))
                .updateStatisticsByKey(anyString(), any(CountryStatisticsDto.class));
    }

    @ParameterizedTest
    @MethodSource("invalidInputProvider")
    void updateStatisticsWithValidationErrors(String countryCode, PageViewUpdateRequest request, String expectedMessage) {
        Mono<Void> result = statisticsService.updateStatistics(countryCode, request);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().contains(expectedMessage))
                .verify();
    }

    @Test
    void updateStatisticsWithRepositoryError() {
        when(statisticsRepository.updateStatisticsByKey(anyString(), any(CountryStatisticsDto.class)))
                .thenReturn(Mono.error(new DataAccessException("Database error", new Throwable())));

        Mono<Void> result = statisticsService.updateStatistics(countryCode, validRequest);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof DataAccessException &&
                        throwable.getMessage().contains("Database error"))
                .verify();
    }

    private static Stream<Arguments> invalidInputProvider() {
        return Stream.of(
                Arguments.of("invalid", new PageViewUpdateRequest("pageName", 10L), "Invalid country code: "),
                Arguments.of("US", new PageViewUpdateRequest("", 10L), "Invalid page name: "),
                Arguments.of("US", new PageViewUpdateRequest("pageName", -1L), "Views to add must be greater than 0: ")
        );
    }
}
