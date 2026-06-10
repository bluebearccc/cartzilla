package com.cartzilla.notification.domain.repository;

import com.cartzilla.notification.domain.entity.Notification;

public interface NotificationRepository {
    Notification save(Notification notification);
}
