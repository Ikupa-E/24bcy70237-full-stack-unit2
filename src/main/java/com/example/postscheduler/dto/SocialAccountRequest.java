package com.example.postscheduler.dto;

import com.example.postscheduler.model.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record SocialAccountRequest(
        @NotNull(message = "Platform is required (TWITTER, LINKEDIN, FACEBOOK or INSTAGRAM)")
        Platform platform,

        @NotBlank(message = "Account handle is required")
        @Size(max = 100, message = "Account handle must not exceed 100 characters")
        String accountHandle,

        @NotBlank(message = "Access token is required")
        @Size(max = 1500, message = "Access token must not exceed 1500 characters")
        String accessToken,

        @Size(max = 1500, message = "Refresh token must not exceed 1500 characters")
        String refreshToken,            // optional

        @Positive(message = "expiresInSeconds must be positive")
        Long expiresInSeconds           // optional: lifetime of the access token
) {}
