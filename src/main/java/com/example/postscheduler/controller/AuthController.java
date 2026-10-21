package com.example.postscheduler.controller;

import com.example.postscheduler.dto.AuthResponse;
import com.example.postscheduler.dto.LoginRequest;
import com.example.postscheduler.dto.RefreshRequest;
import com.example.postscheduler.dto.RegisterRequest;
import com.example.postscheduler.dto.UserResponse;
import com.example.postscheduler.response.ApiResponse;
import com.example.postscheduler.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("User registered successfully", service.register(request)));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Login successful", service.login(request)));
    }

    /** Public: the access token may already have expired, so the refresh token is the credential. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed successfully", service.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshRequest request) {
        service.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully", null));
    }

    /** Returns who the access token belongs to. Requires a valid access token. */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok("Current user", service.getProfile(authentication.getName())));
    }
}
