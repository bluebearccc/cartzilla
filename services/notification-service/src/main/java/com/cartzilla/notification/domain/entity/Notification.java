package com.cartzilla.notification.domain.entity;

import com.cartzilla.notification.domain.vo.NotificationPriority;
import com.cartzilla.notification.domain.vo.NotificationStatus;
import com.cartzilla.notification.domain.vo.NotificationType;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root — NotificationAggregate (in-app UI layer).
 * Rules: NA-01..NA-05, N-01..N-06, BR-N01..BR-N04.
 */
@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** N-05: nullable cho broadcast system */
    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    /** NA-04: ref orders — reference-only */
    @Column(name = "order_id")
    private UUID orderId;

    /** N-01 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    /** N-02: NOT NULL */
    @Column(nullable = false, length = 255)
    private String title;

    /** N-02: NOT NULL */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** N-03: default UNREAD */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status;

    /** N-04: default NORMAL */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationPriority priority;

    /** NA-03: set khi READ; readAt ≥ createdAt */
    @Column(name = "read_at")
    private Instant readAt;

    /** N-06: metadata bổ sung JSONB */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    public static Notification create(UUID recipientUserId, UUID orderId,
                                       NotificationType type, String title, String message,
                                       NotificationPriority priority, String data) {
        if (type == null) throw new BusinessException("type must not be null (N-01)");
        if (title == null || title.isBlank()) throw new BusinessException("title must not be blank (N-02)");
        if (message == null || message.isBlank()) throw new BusinessException("message must not be blank (N-02)");
        Notification n = new Notification();
        n.recipientUserId = recipientUserId;
        n.orderId = orderId;
        n.type = type;
        n.title = title.trim();
        n.message = message.trim();
        n.status = NotificationStatus.UNREAD;
        n.priority = priority != null ? priority : NotificationPriority.NORMAL;
        n.data = data;
        return n;
    }

    /** NA-03/BR-N04: READ → set readAt ≥ createdAt */
    public void markRead() {
        if (status == NotificationStatus.READ) return;
        Instant now = Instant.now();
        if (getCreatedAt() != null && now.isBefore(getCreatedAt()))
            throw new BusinessException("readAt cannot be before createdAt (NA-03)");
        this.status = NotificationStatus.READ;
        this.readAt = now;
    }

    public void archive() {
        this.status = NotificationStatus.ARCHIVED;
    }
}
