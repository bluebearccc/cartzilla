package com.cartzilla.notification.infrastructure.persistence;

import com.cartzilla.notification.domain.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationLogJpaRepository extends JpaRepository<NotificationLog, UUID> {
}
