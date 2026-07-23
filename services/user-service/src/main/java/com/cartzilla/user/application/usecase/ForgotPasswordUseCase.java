package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.PasswordResetToken;
import com.cartzilla.user.domain.repository.PasswordResetTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.infrastructure.feign.NotificationFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForgotPasswordUseCase {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final NotificationFeignClient notificationFeignClient;
    private final TokenGenerator tokenGenerator;

    @Value("${app.reset-password.base-url:http://localhost:5173/reset-password}")
    private String resetPasswordBaseUrl;

    @Transactional
    public void execute(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            if (!user.isActive()) return;
            resetTokenRepository.revokeActiveByUserId(user.getId());
            String token = tokenGenerator.generateUrlSafeToken();
            resetTokenRepository.save(PasswordResetToken.create(
                    user.getId(), token, Instant.now().plusSeconds(30 * 60L)));

            try {
                notificationFeignClient.sendResetPasswordEmail(new NotificationFeignClient.ResetPasswordEmailRequest(
                        user.getEmail(), user.getFullName(), resetPasswordBaseUrl + "?token=" + token, 30));
            } catch (Exception ex) {
                log.warn("Failed to queue reset-password email for {}: {}", user.getEmail(), ex.getMessage());
            }
        });
    }
}
