package com.example.postscheduler.controller;

import com.example.postscheduler.dto.PageResponse;
import com.example.postscheduler.dto.PostRequest;
import com.example.postscheduler.dto.PostResponse;
import com.example.postscheduler.dto.PostStatsResponse;
import com.example.postscheduler.dto.PostSummaryResponse;
import com.example.postscheduler.dto.ScheduleRequest;
import com.example.postscheduler.model.PostStatus;
import com.example.postscheduler.response.ApiResponse;
import com.example.postscheduler.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/posts")
public class PostController {

    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostResponse>> create(@Valid @RequestBody PostRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Post created successfully", service.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<PostSummaryResponse>>> getAll(
            @RequestParam(required = false) PostStatus status,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String direction) {
        return ResponseEntity.ok(ApiResponse.ok("Posts fetched successfully",
                service.search(status, author, keyword, page, size, sortBy, direction)));
    }

    /** Aggregated counts per author and status (native SQL, cached). */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<PostStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok("Post statistics fetched successfully", service.getStats()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Post fetched successfully", service.getById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody PostRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Post updated successfully", service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")   // only admins may delete
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Post deleted successfully", null));
    }

    // ---- scheduling ----
    @PatchMapping("/{id}/schedule")
    public ResponseEntity<ApiResponse<PostResponse>> schedule(@PathVariable Long id,
                                                              @Valid @RequestBody ScheduleRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Post scheduled successfully", service.schedule(id, request)));
    }

    @PatchMapping("/{id}/unschedule")
    public ResponseEntity<ApiResponse<PostResponse>> unschedule(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Post moved back to draft", service.unschedule(id)));
    }
}
