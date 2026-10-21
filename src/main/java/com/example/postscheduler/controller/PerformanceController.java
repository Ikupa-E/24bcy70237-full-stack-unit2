package com.example.postscheduler.controller;

import com.example.postscheduler.dto.PerformanceReport;
import com.example.postscheduler.model.PostStatus;
import com.example.postscheduler.response.ApiResponse;
import com.example.postscheduler.service.PerformanceService;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Endpoints used to compare query strategies and to reset the caches between benchmark runs. */
@RestController
@RequestMapping("/api/v1/performance")
@PreAuthorize("hasRole('ADMIN')")
public class PerformanceController {

    private final PerformanceService service;
    private final CacheManager cacheManager;

    public PerformanceController(PerformanceService service, CacheManager cacheManager) {
        this.service = service;
        this.cacheManager = cacheManager;
    }

    @GetMapping("/posts-with-comments/naive")
    public ResponseEntity<ApiResponse<PerformanceReport>> naive(
            @RequestParam(required = false) PostStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Loaded with the N+1 strategy", service.naive(status)));
    }

    @GetMapping("/posts-with-comments/optimized")
    public ResponseEntity<ApiResponse<PerformanceReport>> optimized(
            @RequestParam(required = false) PostStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Loaded with JOIN FETCH", service.optimized(status)));
    }

    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Void>> clearCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        return ResponseEntity.ok(ApiResponse.ok("All caches cleared", null));
    }
}
