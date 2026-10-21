package com.example.postscheduler.service;

import com.example.postscheduler.dto.ConnectionStatusResponse;
import com.example.postscheduler.dto.SocialAccountRequest;
import com.example.postscheduler.dto.SocialAccountResponse;
import com.example.postscheduler.dto.TokenUpdateRequest;
import com.example.postscheduler.exception.DuplicateResourceException;
import com.example.postscheduler.exception.ResourceNotFoundException;
import com.example.postscheduler.model.AppUser;
import com.example.postscheduler.model.SocialAccount;
import com.example.postscheduler.repository.AppUserRepository;
import com.example.postscheduler.repository.SocialAccountRepository;
import com.example.postscheduler.security.AesEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Stores third-party credentials. Tokens are encrypted BEFORE they reach the database, decrypted only
 * when they are really needed, are never returned by the API and are never written to the logs.
 */
@Service
@Transactional
public class SocialAccountService {

    private static final Logger log = LoggerFactory.getLogger(SocialAccountService.class);

    private final SocialAccountRepository repository;
    private final AppUserRepository userRepository;
    private final AesEncryptionService encryption;

    public SocialAccountService(SocialAccountRepository repository, AppUserRepository userRepository,
                                AesEncryptionService encryption) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.encryption = encryption;
    }

    public SocialAccountResponse create(String username, SocialAccountRequest req) {
        AppUser owner = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (repository.countDuplicates(username, req.platform(), req.accountHandle()) > 0) {
            throw new DuplicateResourceException(
                    req.platform() + " account '" + req.accountHandle() + "' is already linked");
        }

        SocialAccount account = new SocialAccount();
        account.setOwner(owner);
        account.setPlatform(req.platform());
        account.setAccountHandle(req.accountHandle());
        applyTokens(account, req.accessToken(), req.refreshToken(), req.expiresInSeconds(), true);

        SocialAccount saved = repository.save(account);
        log.info("Social account linked: id={}, platform={}, owner={}", saved.getId(), saved.getPlatform(), username);
        return SocialAccountResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<SocialAccountResponse> list(String username) {
        return repository.findAllByOwner(username).stream().map(SocialAccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SocialAccountResponse get(String username, Long id) {
        return SocialAccountResponse.from(find(username, id));
    }

    /** Replaces the stored tokens, for example after the third party issued new ones. */
    public SocialAccountResponse updateTokens(String username, Long id, TokenUpdateRequest req) {
        SocialAccount account = find(username, id);
        // refresh token omitted = keep the existing one
        applyTokens(account, req.accessToken(), req.refreshToken(), req.expiresInSeconds(), false);
        SocialAccount saved = repository.save(account);
        log.info("Social account tokens replaced: id={}, owner={}", id, username);
        return SocialAccountResponse.from(saved);
    }

    public void delete(String username, Long id) {
        repository.delete(find(username, id));
        log.info("Social account removed: id={}, owner={}", id, username);
    }

    /**
     * Simulates "using" the credential (e.g. before publishing a post): the token is decrypted in memory
     * and its expiry is checked. The token value itself is never returned or logged.
     */
    @Transactional(readOnly = true)
    public ConnectionStatusResponse checkConnection(String username, Long id) {
        SocialAccount account = find(username, id);
        encryption.decrypt(account.getAccessTokenEncrypted());   // throws if the data was tampered with or the key is wrong
        log.info("Access token decrypted for account id={} (value not logged)", id);

        LocalDateTime expiresAt = account.getTokenExpiresAt();
        boolean expired = expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
        String message = expired
                ? "Token decrypted successfully but it has expired. Re-authorise the account."
                : "Token decrypted successfully and can be used.";
        return new ConnectionStatusResponse(account.getId(), account.getPlatform(), account.getAccountHandle(),
                true, !expired, message);
    }

    // ---- helpers ----
    private SocialAccount find(String username, Long id) {
        // Same 404 whether the account does not exist or belongs to someone else (no information leak)
        return repository.findByIdAndOwner(id, username)
                .orElseThrow(() -> new ResourceNotFoundException("Social account", id));
    }

    private void applyTokens(SocialAccount account, String accessToken, String refreshToken,
                             Long expiresInSeconds, boolean replaceRefresh) {
        account.setAccessTokenEncrypted(encryption.encrypt(accessToken));
        account.setTokenHint(hint(accessToken));
        if (refreshToken != null && !refreshToken.isBlank()) {
            account.setRefreshTokenEncrypted(encryption.encrypt(refreshToken));
        } else if (replaceRefresh) {
            account.setRefreshTokenEncrypted(null);
        }
        account.setTokenExpiresAt(expiresInSeconds == null ? null : LocalDateTime.now().plusSeconds(expiresInSeconds));
    }

    private String hint(String token) {
        return token.length() > 8 ? "****" + token.substring(token.length() - 4) : "****";
    }
}
