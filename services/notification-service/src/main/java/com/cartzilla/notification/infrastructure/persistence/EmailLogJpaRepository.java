package com.cartzilla.notification.infrastructure.persistence;

import com.cartzilla.notification.domain.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EmailLogJpaRepository extends JpaRepository<EmailLog, UUID> {
}
