package com.cartzilla.user.application.usecase;

import com.cartzilla.security.JwtTokenProvider;
import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenUseCase {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenGenerator tokenGenerator;

    @Value("${jwt.refresh-ttl-ms:604800000}")
    private long refreshTtlMs;

    public record Result(String accessToken, String refreshToken, String email, String role) {}

    @Transactional
    public Result execute(String token) {
        RefreshToken current = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));
        if (current.isExpired()) {
            current.revoke();
            refreshTokenRepository.save(current);
            throw new BusinessException("Refresh token expired");
        }
        User user = userRepository.findById(current.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.requireActive();
        current.revoke();
        refreshTokenRepository.save(current);
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String newRefreshToken = tokenGenerator.generateUrlSafeToken();
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(), newRefreshToken, Instant.now().plus(Duration.ofMillis(refreshTtlMs))));
        return new Result(accessToken, newRefreshToken, user.getEmail(), user.getRole().name());
    }
}
