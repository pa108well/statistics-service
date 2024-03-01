package org.statisticservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.statisticservice.model.CountryStatistics;
import org.statisticservice.model.PageViewUpdateRequest;
import org.statisticservice.service.StatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.statisticservice.repository.exceptions.DataAccessException;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@Tag(name = "StatisticsController", description = "Manages the aggregation of statistics for the most viewed pages")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @PostMapping
    @Operation(summary = "Increment page views and update the top pages' statistics accordingly.")
    public Mono<ResponseEntity<Object>> updateStatistics(
            @RequestParam String countryCode,
            @RequestBody PageViewUpdateRequest request
    ) {
        return statisticsService.updateStatistics(countryCode, request)
                .then(Mono.just(ResponseEntity.ok().build()))
                .onErrorResume(IllegalArgumentException.class, ex ->
                        Mono.just(ResponseEntity.badRequest().body("Bad request: " + ex.getMessage())))
                .onErrorResume(DataAccessException.class, ex ->
                        Mono.just(ResponseEntity.internalServerError().body("Internal server error: " + ex.getMessage())));

    }

    @GetMapping
    @Operation(summary = "Retrieve statistics for the top viewed pages by country.")
    public Mono<ResponseEntity<Map<String, CountryStatistics>>> getTopViewedPagesStatistics() {
        return statisticsService.getTopViewsStatistics()
                .map(ResponseEntity::ok)
                .onErrorResume(DataAccessException.class, ex ->
                        Mono.just(ResponseEntity.internalServerError().body(Map.of(ex.getMessage(), null )))); // :))
    }                           //todo тут тоже не хватило времени залогировать и правлиьно обработать ошибки

}
