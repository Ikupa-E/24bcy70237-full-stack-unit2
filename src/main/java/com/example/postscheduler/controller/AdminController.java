package com.example.postscheduler.controller;

import com.example.postscheduler.dto.RoleUpdateRequest;
import com.example.postscheduler.dto.UserResponse;
import com.example.postscheduler.response.ApiResponse;
import com.example.postscheduler.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin-only. Protected twice: by the URL rule in SecurityConfig (/api/v1/admin/**)
 * and by method-level security (@PreAuthorize) below.
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService service;

    public AdminController(UserService service) {
        this.service = service;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok("Users fetched successfully", service.listUsers()));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> changeRole(@PathVariable Long id,
                                                                @Valid @RequestBody RoleUpdateRequest request,
                                                                Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Role updated successfully",
                service.changeRole(id, request.role(), authentication.getName())));
    }
}
