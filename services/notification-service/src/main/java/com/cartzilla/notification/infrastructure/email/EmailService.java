package com.cartzilla.notification.infrastructure.email;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import java.util.Properties;

/** Gửi email qua SMTP (MailHog ở dev và tự động chuyển tiếp qua Gmail thật). */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private JavaMailSenderImpl gmailSender;

    @Value("${app.mail.from:no-reply@cartzilla.local}")
    private String from;

    @Value("${APP_GMAIL_USERNAME:}")
    private String gmailUsername;

    @Value("${APP_GMAIL_PASSWORD:}")
    private String gmailPassword;

    @PostConstruct
    public void init() {
        if (gmailUsername != null && !gmailUsername.isBlank() && gmailPassword != null && !gmailPassword.isBlank()) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost("smtp.gmail.com");
            sender.setPort(587);
            sender.setUsername(gmailUsername);
            sender.setPassword(gmailPassword);

            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.debug", "false");

            this.gmailSender = sender;
        }
    }

    public void send(String to, String subject, String body) {
        // 1. Gửi về MailHog
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        msg.setFrom(from);
        mailSender.send(msg);

        // 2. Tự động gửi một bản sao về Gmail thật nếu được cấu hình
        if (gmailSender != null) {
            try {
                SimpleMailMessage gmailMsg = new SimpleMailMessage();
                gmailMsg.setTo(to);
                gmailMsg.setSubject(subject);
                gmailMsg.setText(body);
                // Gmail yêu cầu setFrom trùng với tài khoản xác thực gửi
                gmailMsg.setFrom(gmailUsername);
                gmailSender.send(gmailMsg);
            } catch (Exception e) {
                System.err.println("Failed to auto-forward email via Gmail: " + e.getMessage());
            }
        }
    }
}
