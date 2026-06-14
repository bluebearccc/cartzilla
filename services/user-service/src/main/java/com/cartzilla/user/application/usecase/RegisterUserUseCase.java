package com.cartzilla.user.application.usecase;

import com.cartzilla.user.application.command.AuthCommand;
import com.cartzilla.user.domain.entity.EmailVerificationToken;
import com.cartzilla.user.domain.entity.User;
import com.cartzilla.user.domain.exception.DuplicateEmailException;
import com.cartzilla.user.domain.repository.EmailVerificationTokenRepository;
import com.cartzilla.user.domain.repository.UserRepository;
import com.cartzilla.user.infrastructure.feign.NotificationFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterUserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final NotificationFeignClient notificationFeignClient;
    private final TokenGenerator tokenGenerator;

    @Value("${app.verify-email.base-url:http://localhost:5173/verify-email}")
    private String verificationBaseUrl;

    @Value("${app.verify-email.ttl-seconds:86400}")
    private long verificationTtlSeconds;

    @Transactional
    public UUID execute(AuthCommand.Register cmd) {
        if (userRepository.existsByEmail(cmd.email())) {
            throw new DuplicateEmailException(cmd.email());
        }
        User user = User.createCustomer(
                cmd.email(),
                passwordEncoder.encode(cmd.password()),
                cmd.fullName());
        User saved = userRepository.save(user);
        verificationTokenRepository.revokeActiveByUserId(saved.getId());
        String token = tokenGenerator.generateUrlSafeToken();
        verificationTokenRepository.save(EmailVerificationToken.create(
                saved.getId(), token, Instant.now().plusSeconds(verificationTtlSeconds)));
        // Email là best-effort: notification-service lỗi/timeout KHÔNG được rollback việc tạo user.
        // User vẫn được tạo (emailVerified=false) và có thể yêu cầu gửi lại email xác minh sau.
        try {
            notificationFeignClient.sendVerificationEmail(new NotificationFeignClient.VerificationEmailRequest(
                    saved.getEmail(), saved.getFullName(), verificationBaseUrl + "?token=" + token,
                    (int) (verificationTtlSeconds / 60)));
        } catch (Exception ex) {
            log.warn("Failed to queue verification email for {}: {}", saved.getEmail(), ex.getMessage());
        }
        return saved.getId();
    }
}
