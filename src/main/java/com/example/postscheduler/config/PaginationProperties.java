package com.example.postscheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Default pagination and sorting settings, read from application.properties (app.pagination.*). */
@Component
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {

    private int defaultPageSize = 10;
    private int maxPageSize = 50;
    private String defaultSortBy = "createdAt";
    private String defaultDirection = "desc";

    public int getDefaultPageSize() { return defaultPageSize; }
    public void setDefaultPageSize(int defaultPageSize) { this.defaultPageSize = defaultPageSize; }

    public int getMaxPageSize() { return maxPageSize; }
    public void setMaxPageSize(int maxPageSize) { this.maxPageSize = maxPageSize; }

    public String getDefaultSortBy() { return defaultSortBy; }
    public void setDefaultSortBy(String defaultSortBy) { this.defaultSortBy = defaultSortBy; }

    public String getDefaultDirection() { return defaultDirection; }
    public void setDefaultDirection(String defaultDirection) { this.defaultDirection = defaultDirection; }
}
