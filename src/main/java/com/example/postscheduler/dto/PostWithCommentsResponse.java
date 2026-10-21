package com.example.postscheduler.dto;

import com.example.postscheduler.model.PostStatus;
import java.util.List;

public record PostWithCommentsResponse(
        Long id,
        String title,
        String author,
        PostStatus status,
        List<CommentResponse> comments
) {
}
