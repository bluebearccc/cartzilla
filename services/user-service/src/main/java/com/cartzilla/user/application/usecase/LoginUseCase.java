package com.cartzilla.user.application.usecase;

import com.cartzilla.security.JwtTokenProvider;
import com.cartzilla.user.application.command.AuthCommand;
import com.cartzilla.user.domain.entity.RefreshToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenGenerator tokenGenerator;

    @Value("${jwt.refresh-ttl-ms:604800000}")
    private long refreshTtlMs;

    public record Result(String accessToken, String refreshToken, String email, String role) {}

    @Transactional
    public Result execute(AuthCommand.Login cmd) {
        User user = userRepository.findByEmail(cmd.email())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("Invalid email or password");
        }
        if (!passwordEncoder.matches(cmd.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }
        user.requireActive();
        if (!user.isEmailVerified()) {
            throw new BusinessException("Email is not verified");
        }
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole().name());
        String refreshToken = tokenGenerator.generateUrlSafeToken();
        refreshTokenRepository.save(RefreshToken.create(
                user.getId(), refreshToken, Instant.now().plus(Duration.ofMillis(refreshTtlMs))));
        return new Result(accessToken, refreshToken, user.getEmail(), user.getRole().name());
    }
}
