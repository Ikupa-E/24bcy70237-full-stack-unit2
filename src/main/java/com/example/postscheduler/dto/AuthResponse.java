package com.example.postscheduler.dto;

import com.example.postscheduler.model.Role;

/** Returned by register, login and refresh. The refresh token is shown only here, never again. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,          // lifetime of the access token
        long refreshExpiresInSeconds,   // lifetime of the refresh token
        String username,
        Role role
) {}
