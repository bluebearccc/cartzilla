package com.cartzilla.notification.infrastructure.persistence;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.vo.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByRecipientUserId(UUID recipientUserId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId);

    long countByRecipientUserIdAndStatus(UUID recipientUserId, NotificationStatus status);
}
