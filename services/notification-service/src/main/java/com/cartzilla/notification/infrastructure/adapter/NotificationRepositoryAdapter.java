package com.cartzilla.notification.infrastructure.adapter;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import com.cartzilla.notification.domain.vo.NotificationStatus;
import com.cartzilla.notification.infrastructure.persistence.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {
    private final NotificationJpaRepository jpa;

    @Override public Notification save(Notification n) { return jpa.save(n); }

    @Override
    public Page<Notification> findByRecipientUserId(UUID recipientUserId, Pageable pageable) {
        return jpa.findByRecipientUserId(recipientUserId, pageable);
    }

    @Override
    public Optional<Notification> findByIdAndRecipientUserId(UUID id, UUID recipientUserId) {
        return jpa.findByIdAndRecipientUserId(id, recipientUserId);
    }

    @Override
    public long countByRecipientUserIdAndStatus(UUID recipientUserId, NotificationStatus status) {
        return jpa.countByRecipientUserIdAndStatus(recipientUserId, status);
    }
}
