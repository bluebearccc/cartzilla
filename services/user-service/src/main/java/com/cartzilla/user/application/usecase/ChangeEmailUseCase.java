package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangeEmailUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public User execute(UUID userId, UserCommand.ChangeEmail command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();
        String normalizedEmail = normalize(command.email());
        userRepository.findByEmail(normalizedEmail)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new BusinessException("Email already exists: " + normalizedEmail);
                });
        user.changeEmail(normalizedEmail);
        revokeRefreshTokens(user.getId());
        return userRepository.save(user);
    }

    private String normalize(String email) {
        if (email == null || email.isBlank()) {
            throw new BusinessException("Email must not be blank");
        }
        return email.trim().toLowerCase();
    }

    private void revokeRefreshTokens(UUID userId) {
        refreshTokenRepository.findByUserId(userId).forEach(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });
    }
}
