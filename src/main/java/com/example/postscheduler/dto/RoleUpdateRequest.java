package com.example.postscheduler.dto;

import com.example.postscheduler.model.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull(message = "Role is required") Role role) {}
