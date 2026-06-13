package com.cartzilla.user.infrastructure.adapter;

import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.infrastructure.persistence.RefreshTokenJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
    private final RefreshTokenJpaRepository jpa;

    @Override public RefreshToken save(RefreshToken refreshToken) { return jpa.save(refreshToken); }
    @Override public Optional<RefreshToken> findByToken(String token) { return jpa.findByToken(token); }
    @Override public List<RefreshToken> findByUserId(UUID userId) { return jpa.findByUserId(userId); }
}
