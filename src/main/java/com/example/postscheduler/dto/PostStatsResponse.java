package com.example.postscheduler.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Cached statistics. generatedAt stays the same while the cached copy is served. */
public record PostStatsResponse(
        long totalPosts,
        Map<String, Long> byStatus,
        List<AuthorStatusCount> breakdown,
        LocalDateTime generatedAt
) implements Serializable {
}
