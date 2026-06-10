package com.cartzilla.notification.infrastructure.adapter;

import com.cartzilla.notification.domain.entity.EmailLog;
import com.cartzilla.notification.domain.repository.EmailLogRepository;
import com.cartzilla.notification.infrastructure.persistence.EmailLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailLogRepositoryAdapter implements EmailLogRepository {
    private final EmailLogJpaRepository jpa;
    @Override public EmailLog save(EmailLog el) { return jpa.save(el); }
}
