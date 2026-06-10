package com.cartzilla.notification.domain.entity;

import com.cartzilla.notification.domain.vo.EmailStatus;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Child entity của NotificationAggregate — outbound email log.
 * Rules: NA-02, EL-01..EL-06, BR-N01/N03/N05.
 */
@Entity
@Table(name = "email_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** NA-02: optional — reset password email có thể không có notificationId */
    @Column(name = "notification_id")
    private UUID notificationId;

    /** NA-04: ref orders — reference-only */
    @Column(name = "order_id")
    private UUID orderId;

    /** EL-01: NOT NULL, validate email format */
    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    /** EL-02: NOT NULL */
    @Column(nullable = false, length = 255)
    private String subject;

    /** EL-06: optional */
    @Column(name = "template_key", length = 100)
    private String templateKey;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "provider_message_id", length = 100)
    private String providerMessageId;

    /** EL-03: default PENDING */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailStatus status;

    /** EL-04: set khi SENT; sentAt ≥ createdAt */
    @Column(name = "sent_at")
    private Instant sentAt;

    /** EL-05: populate khi FAILED */
    @Column(columnDefinition = "TEXT")
    private String error;

    public static EmailLog create(UUID notificationId, UUID orderId, String recipientEmail,
                                   String subject, String templateKey, String provider) {
        if (recipientEmail == null || recipientEmail.isBlank())
            throw new BusinessException("recipientEmail must not be blank (EL-01)");
        if (subject == null || subject.isBlank())
            throw new BusinessException("subject must not be blank (EL-02)");
        EmailLog el = new EmailLog();
        el.notificationId = notificationId;
        el.orderId = orderId;
        el.recipientEmail = recipientEmail.trim().toLowerCase();
        el.subject = subject.trim();
        el.templateKey = templateKey;
        el.provider = provider;
        el.status = EmailStatus.PENDING;
        return el;
    }

    /** EL-04/BR-G06: sentAt ≥ createdAt */
    public void markSent(String providerMessageId) {
        Instant now = Instant.now();
        if (getCreatedAt() != null && now.isBefore(getCreatedAt()))
            throw new BusinessException("sentAt cannot be before createdAt (EL-04/BR-G06)");
        this.status = EmailStatus.SENT;
        this.sentAt = now;
        this.providerMessageId = providerMessageId;
    }

    /** EL-05: lưu error */
    public void markFailed(String error) {
        this.status = EmailStatus.FAILED;
        this.error = error;
    }
}
