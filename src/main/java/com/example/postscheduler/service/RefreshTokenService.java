package com.example.postscheduler.service;

import com.example.postscheduler.exception.InvalidTokenException;
import com.example.postscheduler.model.AppUser;
import com.example.postscheduler.model.RefreshToken;
import com.example.postscheduler.repository.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Refresh token lifecycle: issue, rotate (single use), revoke, purge.
 *  - The raw token is a random 256-bit value shown to the client only once. Only its SHA-256 hash is stored.
 *  - ROTATION: every refresh revokes the old token and issues a new one.
 *  - REUSE DETECTION: presenting an already-used token means it may have been stolen, so the whole
 *    token family (everything from that login) is revoked and the user must log in again.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Result of a rotation: the user the old token belonged to and the new raw refresh token. */
    public record Rotation(AppUser user, String newRefreshToken) {}

    private final RefreshTokenRepository repository;
    private final long expirationDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository,
                               @Value("${app.jwt.refresh-expiration-days:7}") long expirationDays) {
        this.repository = repository;
        this.expirationDays = expirationDays;
    }

    public long getExpirationSeconds() {
        return expirationDays * 24 * 60 * 60;
    }

    /** Starts a new token family (called at login / registration). Returns the raw token. */
    @Transactional
    public String issue(AppUser user) {
        return create(user, UUID.randomUUID().toString());
    }

    /**
     * noRollbackFor: when reuse is detected we revoke the family and THEN throw. Without this setting the
     * exception would roll the revocation back and the stolen token family would stay active.
     */
    @Transactional(noRollbackFor = InvalidTokenException.class)
    public Rotation rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (current.isRevoked()) {
            int revoked = repository.revokeFamily(current.getFamilyId());
            log.warn("Refresh token reuse detected for user '{}'. Revoked {} active token(s) in its family",
                    current.getUser().getUsername(), revoked);
            throw new InvalidTokenException("Refresh token has already been used. Please log in again.");
        }
        if (current.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Refresh token has expired. Please log in again.");
        }

        current.setRevoked(true);                                   // old token can never be used again
        String newToken = create(current.getUser(), current.getFamilyId());
        log.info("Refresh token rotated for user '{}'", current.getUser().getUsername());
        return new Rotation(current.getUser(), newToken);
    }

    /** Logout: revokes the whole family. Unknown tokens are ignored, so logout is safe to repeat. */
    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            int revoked = repository.revokeFamily(token.getFamilyId());
            log.info("Logout: revoked {} refresh token(s) for user '{}'", revoked, token.getUser().getUsername());
        });
    }

    /** Housekeeping: removes expired tokens from the table (hourly by default). */
    @Scheduled(cron = "${app.jwt.cleanup-cron:0 0 * * * *}")
    @Transactional
    public void purgeExpired() {
        int removed = repository.deleteExpired(LocalDateTime.now());
        if (removed > 0) {
            log.info("Purged {} expired refresh token(s)", removed);
        }
    }

    // ---- helpers ----
    private String create(AppUser user, String familyId) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken token = new RefreshToken();
        token.setTokenHash(hash(rawToken));
        token.setUser(user);
        token.setFamilyId(familyId);
        token.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        repository.save(token);
        return rawToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}
