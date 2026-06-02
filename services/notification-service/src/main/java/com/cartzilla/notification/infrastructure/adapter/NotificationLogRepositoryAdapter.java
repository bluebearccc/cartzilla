package com.cartzilla.notification.infrastructure.adapter;

import com.cartzilla.notification.domain.entity.NotificationLog;
import com.cartzilla.notification.domain.repository.NotificationLogRepository;
import com.cartzilla.notification.infrastructure.persistence.NotificationLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationLogRepositoryAdapter implements NotificationLogRepository {
    private final NotificationLogJpaRepository jpa;
    @Override public NotificationLog save(NotificationLog log) { return jpa.save(log); }
}
