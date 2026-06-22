package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/** F12 — UC-08: customer đọc notification → status READ, set readAt (BR-N04). */
@Service
@RequiredArgsConstructor
public class MarkNotificationReadUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification execute(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository
                .findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found: " + notificationId));
        notification.markRead();
        return notificationRepository.save(notification);
    }
}
