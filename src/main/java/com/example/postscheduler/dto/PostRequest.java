package com.example.postscheduler.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record PostRequest(
        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 2000, message = "Content must not exceed 2000 characters")
        String content,

        @NotBlank(message = "Author is required")
        @Size(min = 2, max = 50, message = "Author must be between 2 and 50 characters")
        String author,

        @Future(message = "Scheduled time must be in the future")
        LocalDateTime scheduledAt   // optional: if present, the post is created as SCHEDULED
) {}
