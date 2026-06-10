package com.cartzilla.notification.domain.repository;

import com.cartzilla.notification.domain.entity.EmailLog;

public interface EmailLogRepository {
    EmailLog save(EmailLog emailLog);
}
