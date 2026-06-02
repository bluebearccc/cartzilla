package com.cartzilla.notification.domain.repository;

import com.cartzilla.notification.domain.entity.NotificationLog;

public interface NotificationLogRepository {
    NotificationLog save(NotificationLog log);
}
