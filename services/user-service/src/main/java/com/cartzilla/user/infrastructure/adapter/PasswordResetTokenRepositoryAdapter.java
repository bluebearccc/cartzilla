package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.PasswordResetToken;
import com.cartzilla.user.domain.repository.PasswordResetTokenRepository;
import com.cartzilla.user.infrastructure.persistence.PasswordResetTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {
    private final PasswordResetTokenJpaRepository jpa;

    @Override public PasswordResetToken save(PasswordResetToken token) { return jpa.save(token); }
    @Override public Optional<PasswordResetToken> findByToken(String token) { return jpa.findByToken(token); }

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
