package org.statisticservice.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class CountryStatisticsDto {
    private String country;
    private String mostPopularPage;
    private Long views;
}
