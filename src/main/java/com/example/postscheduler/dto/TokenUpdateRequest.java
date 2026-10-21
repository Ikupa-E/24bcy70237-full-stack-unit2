package com.example.postscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TokenUpdateRequest(
        @NotBlank(message = "Access token is required")
        @Size(max = 1500, message = "Access token must not exceed 1500 characters")
        String accessToken,

        @Size(max = 1500, message = "Refresh token must not exceed 1500 characters")
        String refreshToken,            // optional: if omitted, the stored refresh token is kept

        @Positive(message = "expiresInSeconds must be positive")
        Long expiresInSeconds
) {}
