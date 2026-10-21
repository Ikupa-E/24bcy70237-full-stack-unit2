package com.example.postscheduler.dto;

import java.util.List;

/** Result of an N+1 vs JOIN FETCH run, including how many SQL statements Hibernate executed. */
public record PerformanceReport(
        String strategy,
        int postCount,
        int commentCount,
        long sqlStatements,
        long executionTimeMs,
        List<PostWithCommentsResponse> posts
) {
}
