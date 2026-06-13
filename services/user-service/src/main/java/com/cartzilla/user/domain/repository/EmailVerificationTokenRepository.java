package com.cartzilla.user.domain.repository;

import com.cartzilla.user.domain.entity.EmailVerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository {
    EmailVerificationToken save(EmailVerificationToken token);
    Optional<EmailVerificationToken> findByToken(String token);
    void revokeActiveByUserId(UUID userId);
}
