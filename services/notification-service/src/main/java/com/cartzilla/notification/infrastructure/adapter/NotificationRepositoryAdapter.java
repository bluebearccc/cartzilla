package com.cartzilla.notification.infrastructure.adapter;

import com.cartzilla.notification.domain.entity.Notification;
import com.cartzilla.notification.domain.repository.NotificationRepository;
import com.cartzilla.notification.infrastructure.persistence.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {
    private final NotificationJpaRepository jpa;
    @Override public Notification save(Notification n) { return jpa.save(n); }
}
