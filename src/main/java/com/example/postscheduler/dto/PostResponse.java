package com.example.postscheduler.dto;

import com.example.postscheduler.model.Post;
import com.example.postscheduler.model.PostStatus;
import java.io.Serializable;
import java.time.LocalDateTime;

/** Serializable because instances are stored in the cache. */
public record PostResponse(
        Long id,
        String title,
        String content,
        String author,
        PostStatus status,
        LocalDateTime scheduledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {
    public static PostResponse from(Post p) {
        return new PostResponse(p.getId(), p.getTitle(), p.getContent(), p.getAuthor(),
                p.getStatus(), p.getScheduledAt(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
