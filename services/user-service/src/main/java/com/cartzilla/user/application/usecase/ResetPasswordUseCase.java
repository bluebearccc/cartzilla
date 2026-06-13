package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.repository.PasswordResetTokenRepository;
import com.cartzilla.user.domain.repository.RefreshTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResetPasswordUseCase {
    private final PasswordResetTokenRepository resetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void execute(String token, String newPassword) {
        var resetToken = resetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid password reset token"));
        if (resetToken.isUsed() || resetToken.isExpired()) {
            throw new BusinessException("Password reset token expired or used");
        }
        var user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
        user.requireActive();
        user.changePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        resetToken.markUsed();
        resetTokenRepository.save(resetToken);
        refreshTokenRepository.findByUserId(user.getId()).forEach(refreshToken -> {
            refreshToken.revoke();
            refreshTokenRepository.save(refreshToken);
        });
    }
}
