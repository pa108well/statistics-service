package org.statisticservice.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryStatistics implements Serializable {

    private static final long serialVersionUID = 1L;

    private long views;
    private String mostPopularPage;
}

