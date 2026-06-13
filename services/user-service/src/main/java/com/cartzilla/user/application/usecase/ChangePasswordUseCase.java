package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.UserCommand;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChangePasswordUseCase {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(UUID userId, UserCommand.ChangePassword command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found: " + userId));
        user.requireActive();
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new BusinessException("Password login is not enabled for this account");
        }
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }
        user.changePassword(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);
        revokeRefreshTokens(user.getId());
    }

    private void revokeRefreshTokens(UUID userId) {
        refreshTokenRepository.findByUserId(userId).forEach(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });
    }
}
