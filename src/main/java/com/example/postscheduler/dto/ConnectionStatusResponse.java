package com.example.postscheduler.dto;

import com.example.postscheduler.model.Platform;

public record ConnectionStatusResponse(
        Long id,
        Platform platform,
        String accountHandle,
        boolean decryptionSucceeded,
        boolean tokenUsable,
        String message
) {}
