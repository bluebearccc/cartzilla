package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByToken(String token);
    void revokeActiveByUserId(UUID userId);
}
