package com.example.postscheduler.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CryptoTextRequest(
        @NotBlank(message = "Text is required")
        @Size(max = 1500, message = "Text must not exceed 1500 characters")
        String text
) {}
