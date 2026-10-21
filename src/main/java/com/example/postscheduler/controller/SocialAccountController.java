package com.example.postscheduler.controller;

import com.example.postscheduler.dto.ConnectionStatusResponse;
import com.example.postscheduler.dto.SocialAccountRequest;
import com.example.postscheduler.dto.SocialAccountResponse;
import com.example.postscheduler.dto.TokenUpdateRequest;
import com.example.postscheduler.response.ApiResponse;
import com.example.postscheduler.service.SocialAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Every user manages only their own linked accounts (the owner comes from the access token). */
@RestController
@RequestMapping("/api/v1/social-accounts")
public class SocialAccountController {

    private final SocialAccountService service;

    public SocialAccountController(SocialAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SocialAccountResponse>> create(
            @Valid @RequestBody SocialAccountRequest request, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Social account linked successfully", service.create(auth.getName(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SocialAccountResponse>>> list(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Social accounts fetched successfully", service.list(auth.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SocialAccountResponse>> get(@PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Social account fetched successfully", service.get(auth.getName(), id)));
    }

    @PutMapping("/{id}/tokens")
    public ResponseEntity<ApiResponse<SocialAccountResponse>> updateTokens(
            @PathVariable Long id, @Valid @RequestBody TokenUpdateRequest request, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Tokens replaced successfully",
                service.updateTokens(auth.getName(), id, request)));
    }

    @PostMapping("/{id}/test-connection")
    public ResponseEntity<ApiResponse<ConnectionStatusResponse>> testConnection(
            @PathVariable Long id, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok("Connection checked", service.checkConnection(auth.getName(), id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication auth) {
        service.delete(auth.getName(), id);
        return ResponseEntity.ok(ApiResponse.ok("Social account removed successfully", null));
    }
}
