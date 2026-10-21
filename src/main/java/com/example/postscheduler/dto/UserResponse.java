package com.example.postscheduler.dto;

import com.example.postscheduler.model.AppUser;
import com.example.postscheduler.model.Role;
import java.time.LocalDateTime;

/** Never includes the password or its hash. */
public record UserResponse(Long id, String username, Role role, LocalDateTime createdAt) {
    public static UserResponse from(AppUser u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getRole(), u.getCreatedAt());
    }
}
