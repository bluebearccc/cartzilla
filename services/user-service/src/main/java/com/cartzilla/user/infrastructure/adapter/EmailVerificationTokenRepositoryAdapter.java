package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.EmailVerificationToken;
import com.cartzilla.user.domain.repository.EmailVerificationTokenRepository;
import com.cartzilla.user.infrastructure.persistence.EmailVerificationTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryAdapter implements EmailVerificationTokenRepository {
    private final EmailVerificationTokenJpaRepository jpa;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        return jpa.save(token);
    }

    @Override
    public Optional<EmailVerificationToken> findByToken(String token) {
        return jpa.findByToken(token);
    }

    @Override
    public void revokeActiveByUserId(UUID userId) {
        jpa.findByUserId(userId).stream()
                .filter(token -> !token.isUsed() && !token.isExpired())
                .forEach(token -> {
                    token.markUsed();
                    jpa.save(token);
                });
    }
}
