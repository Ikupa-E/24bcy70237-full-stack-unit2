package com.example.postscheduler.dto;

import java.time.LocalDateTime;

public record CommentResponse(Long id, String commenter, String body, LocalDateTime createdAt) {
}
