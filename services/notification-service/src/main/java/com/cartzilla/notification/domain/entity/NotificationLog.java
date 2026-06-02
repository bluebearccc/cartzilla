package com.cartzilla.notification.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id")
    private UUID orderId;
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;
    private String type;       // ORDER_CONFIRMED | ORDER_CANCELLED | ...
    private String status;     // SENT | FAILED | PENDING
    private String subject;
    @Column(name = "sent_at")
    private Instant sentAt;
    private String error;

    public static NotificationLog of(UUID orderId, String email, String type, String subject) {
        NotificationLog n = new NotificationLog();
        n.orderId = orderId;
        n.recipientEmail = email;
        n.type = type;
        n.subject = subject;
        n.status = "PENDING";
        return n;
    }

    public void markSent() { this.status = "SENT"; this.sentAt = Instant.now(); }
    public void markFailed(String err) { this.status = "FAILED"; this.error = err; }
}
