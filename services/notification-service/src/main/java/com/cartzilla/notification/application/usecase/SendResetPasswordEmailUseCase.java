package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.EmailLog;
import com.cartzilla.notification.domain.repository.EmailLogRepository;
import com.cartzilla.notification.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SendResetPasswordEmailUseCase {
    private final EmailService emailService;
    private final EmailLogRepository emailLogRepository;

    @Transactional
    public void execute(String email, String fullName, String resetLink, int expiresInMinutes) {
        String subject = "Reset your Cartzilla password";
        String name = fullName == null || fullName.isBlank() ? "customer" : fullName;
        String body = "Hello " + name + ",\n\n"
                + "Use this link to reset your Cartzilla password. The link expires in "
                + expiresInMinutes + " minutes:\n" + resetLink + "\n\n"
                + "If you did not request this, you can ignore this email.";
        EmailLog emailLog = emailLogRepository.save(EmailLog.create(
                null, null, email, subject, "reset_password", "SMTP"));
        try {
            emailService.send(email, subject, body);
            emailLog.markSent(null);
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", email, e.getMessage());
            emailLog.markFailed(e.getMessage());
        }
        emailLogRepository.save(emailLog);
    }
}
