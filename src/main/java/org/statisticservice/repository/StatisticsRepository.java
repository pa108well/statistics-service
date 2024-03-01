package org.statisticservice.repository;

import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import org.statisticservice.repository.dto.CountryStatisticsDto;
import org.statisticservice.repository.exceptions.DataAccessException;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class StatisticsRepository {
    private static final String TOP_STATISTICS_PATTERN = "top_statistics:*";
    private final ReactiveRedisOperations<String, CountryStatisticsDto> redisOperations;

    public StatisticsRepository(ReactiveRedisTemplate<String, CountryStatisticsDto> redisOperations) {
        this.redisOperations = redisOperations;
    }

    public Mono<Boolean> updateStatisticsByKey(String key, CountryStatisticsDto statistics) {
        return redisOperations.opsForValue().set(key, statistics)
                .onErrorMap(ex -> new DataAccessException("Unable to find statistics for key " + key, ex));
    }

    public Mono<CountryStatisticsDto> findStatisticsByKey(String key) {
        return redisOperations.opsForValue().get(key)
                .onErrorMap(ex -> new DataAccessException("Unable to find statistics for key " + key, ex));
    }

    public Mono<List<CountryStatisticsDto>> findTopViewedPagesStatistics() {
        return redisOperations.scan(ScanOptions.scanOptions().match(TOP_STATISTICS_PATTERN).build())
                .flatMap(key -> redisOperations.opsForValue().get(key))
                .collectList()
                .onErrorMap(ex -> new DataAccessException("Unable to find top viewed pages statistics", ex));
    }
}
