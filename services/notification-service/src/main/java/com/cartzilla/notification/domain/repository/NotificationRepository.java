package com.cartzilla.notification.domain.repository;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.vo.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);

    /** F12: in-app list cho customer, phân trang, mới nhất trước. */
    Page<Notification> findByRecipientUserId(UUID recipientUserId, Pageable pageable);

    /** F12: đọc một notification của chính user (ownership guard). */
    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    /** F12 badge: đếm số chưa đọc. */
    long countByRecipientUserIdAndStatus(UUID recipientUserId, NotificationStatus status);
}
