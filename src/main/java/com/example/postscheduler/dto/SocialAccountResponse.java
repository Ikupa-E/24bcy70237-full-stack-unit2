package com.example.postscheduler.dto;

import com.example.postscheduler.model.Platform;
import com.example.postscheduler.model.SocialAccount;
import java.time.LocalDateTime;

/** Deliberately contains NO token values, only a masked hint such as ****f3a9. */
public record SocialAccountResponse(
        Long id,
        Platform platform,
        String accountHandle,
        String tokenHint,
        boolean hasRefreshToken,
        LocalDateTime tokenExpiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SocialAccountResponse from(SocialAccount s) {
        return new SocialAccountResponse(s.getId(), s.getPlatform(), s.getAccountHandle(), s.getTokenHint(),
                s.getRefreshTokenEncrypted() != null, s.getTokenExpiresAt(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
