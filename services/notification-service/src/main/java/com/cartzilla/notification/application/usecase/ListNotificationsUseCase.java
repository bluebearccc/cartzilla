package com.cartzilla.notification.application.usecase;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import com.cartzilla.notification.domain.vo.NotificationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** F12 — UC-08: customer xem danh sách notification in-app của chính mình. */
@Service
@RequiredArgsConstructor
public class ListNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public Page<Notification> execute(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        return notificationRepository.findByRecipientUserId(userId,
                PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        return notificationRepository.countByRecipientUserIdAndStatus(userId, NotificationStatus.UNREAD);
    }
}
