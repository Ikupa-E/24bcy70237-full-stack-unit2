package com.example.postscheduler.controller;

import com.example.postscheduler.dto.CryptoTextRequest;
import com.example.postscheduler.exception.CryptoException;
import com.example.postscheduler.exception.InvalidQueryParameterException;
import com.example.postscheduler.response.ApiResponse;
import com.example.postscheduler.security.AesEncryptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** Demo only (admin): shows AES-GCM in action. Remove before production. */
@RestController
@RequestMapping("/api/v1/admin/crypto")
@PreAuthorize("hasRole('ADMIN')")
public class CryptoDemoController {

    private final AesEncryptionService encryption;

    public CryptoDemoController(AesEncryptionService encryption) {
        this.encryption = encryption;
    }

    @PostMapping("/encrypt")
    public ResponseEntity<ApiResponse<Map<String, String>>> encrypt(@Valid @RequestBody CryptoTextRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("algorithm", "AES-256-GCM");
        result.put("plaintext", request.text());
        result.put("ciphertext", encryption.encrypt(request.text()));
        return ResponseEntity.ok(ApiResponse.ok("Text encrypted", result));
    }

    /** The request field is called "text" but here it must contain a ciphertext produced by /encrypt. */
    @PostMapping("/decrypt")
    public ResponseEntity<ApiResponse<Map<String, String>>> decrypt(@Valid @RequestBody CryptoTextRequest request) {
        try {
            Map<String, String> result = new LinkedHashMap<>();
            result.put("ciphertext", request.text());
            result.put("plaintext", encryption.decrypt(request.text()));
            return ResponseEntity.ok(ApiResponse.ok("Text decrypted", result));
        } catch (CryptoException ex) {
            throw new InvalidQueryParameterException("Ciphertext is invalid, was tampered with, or was encrypted with a different key");
        }
    }
}
