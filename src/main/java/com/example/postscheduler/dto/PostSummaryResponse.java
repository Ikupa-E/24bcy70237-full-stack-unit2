package com.example.postscheduler.dto;

import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import java.time.LocalDateTime;

/**
 * Lightweight representation used in list endpoints. It carries a short content preview
 * instead of the full body, which keeps each page small. Use GET /{id} for the full post.
 */
public record PostSummaryResponse(
        Long id,
        String title,
        String author,
        PostStatus status,
        String contentPreview,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt
) {
    private static final int PREVIEW_LENGTH = 100;

    public static PostSummaryResponse from(Post p) {
        String content = p.getContent();
        String preview = content.length() > PREVIEW_LENGTH
                ? content.substring(0, PREVIEW_LENGTH) + "..."
                : content;
        return new PostSummaryResponse(p.getId(), p.getTitle(), p.getAuthor(), p.getStatus(),
                preview, p.getScheduledAt(), p.getCreatedAt());
    }
}
