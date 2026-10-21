package com.example.postscheduler.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A third-party (social media) account linked by a user. The OAuth tokens are NEVER stored in plain
 * text: only the AES-GCM ciphertext is saved in the database.
 */
@Entity
@Table(name = "social_accounts", uniqueConstraints = @UniqueConstraint(
        name = "uk_social_owner_platform_handle",
        columnNames = {"owner_id", "platform", "account_handle"}))
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private AppUser owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 20)
    private Platform platform;

    @Column(name = "account_handle", nullable = false, length = 100)
    private String accountHandle;

    // Ciphertext is longer than the plaintext (IV + authentication tag + Base64), hence the generous length
    @Column(name = "access_token_encrypted", nullable = false, length = 4000)
    private String accessTokenEncrypted;

    @Column(name = "refresh_token_encrypted", length = 4000)
    private String refreshTokenEncrypted;

    // "****" + last 4 characters, saved at write time so the token never has to be decrypted just to display it
    @Column(name = "token_hint", length = 20)
    private String tokenHint;

    private LocalDateTime tokenExpiresAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }

    public AppUser getOwner() { return owner; }
    public void setOwner(AppUser owner) { this.owner = owner; }

    public Platform getPlatform() { return platform; }
    public void setPlatform(Platform platform) { this.platform = platform; }

    public String getAccountHandle() { return accountHandle; }
    public void setAccountHandle(String accountHandle) { this.accountHandle = accountHandle; }

    public String getAccessTokenEncrypted() { return accessTokenEncrypted; }
    public void setAccessTokenEncrypted(String accessTokenEncrypted) { this.accessTokenEncrypted = accessTokenEncrypted; }

    public String getRefreshTokenEncrypted() { return refreshTokenEncrypted; }
    public void setRefreshTokenEncrypted(String refreshTokenEncrypted) { this.refreshTokenEncrypted = refreshTokenEncrypted; }

    public String getTokenHint() { return tokenHint; }
    public void setTokenHint(String tokenHint) { this.tokenHint = tokenHint; }

    public LocalDateTime getTokenExpiresAt() { return tokenExpiresAt; }
    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) { this.tokenExpiresAt = tokenExpiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
