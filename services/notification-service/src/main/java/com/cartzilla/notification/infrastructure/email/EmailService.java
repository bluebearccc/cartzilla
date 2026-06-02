package com.cartzilla.notification.infrastructure.email;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Gửi email qua SMTP (MailHog ở dev). Có thể nâng cấp Thymeleaf HTML. */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void send(String to, String subject, String body) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        msg.setFrom("no-reply@cartzilla.local");
        mailSender.send(msg);
    }
}
