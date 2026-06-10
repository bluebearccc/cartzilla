package com.cartzilla.notification.infrastructure.persistence;

import com.cartzilla.notification.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<Notification, UUID> {
}
