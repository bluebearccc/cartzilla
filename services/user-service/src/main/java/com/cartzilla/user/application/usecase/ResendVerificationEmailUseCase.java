package com.cartzilla.user.application.usecase;

import com.cartzilla.user.domain.entity.EmailVerificationToken;
import com.cartzilla.user.domain.repository.EmailVerificationTokenRepository;
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
public class ResendVerificationEmailUseCase {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final NotificationFeignClient notificationFeignClient;
    private final TokenGenerator tokenGenerator;

    @Value("${app.verify-email.base-url:http://localhost:5173/verify-email}")
    private String verificationBaseUrl;

    @Value("${app.verify-email.ttl-seconds:86400}")
    private long verificationTtlSeconds;

    @Transactional
    public void execute(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent(user -> {

            if (!user.isActive() || user.isEmailVerified()) {
                return;
            }
            verificationTokenRepository.revokeActiveByUserId(user.getId());
            String token = tokenGenerator.generateUrlSafeToken();
            verificationTokenRepository.save(EmailVerificationToken.create(
                    user.getId(), token, Instant.now().plusSeconds(verificationTtlSeconds)));

            try {
                notificationFeignClient.sendVerificationEmail(new NotificationFeignClient.VerificationEmailRequest(
                        user.getEmail(), user.getFullName(), verificationBaseUrl + "?token=" + token,
                        (int) (verificationTtlSeconds / 60)));
            } catch (Exception ex) {
                log.warn("Failed to queue verification email for {}: {}", user.getEmail(), ex.getMessage());
            }
        });
    }
}
