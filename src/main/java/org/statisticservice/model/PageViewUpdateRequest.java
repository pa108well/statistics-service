package org.statisticservice.model;

import lombok.Data;

@Data
public class PageViewUpdateRequest {
    private final String pageName;
    private final long viewsToAdd;
}
