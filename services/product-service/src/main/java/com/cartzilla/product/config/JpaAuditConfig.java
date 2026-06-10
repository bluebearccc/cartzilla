package com.cartzilla.product.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/** AuditorAware cho BaseEntity.createdBy/updatedBy — lấy X-User-Id gateway inject. */
@Configuration
public class JpaAuditConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                String userId = attrs.getRequest().getHeader("X-User-Id");
                if (userId != null && !userId.isBlank()) {
                    return Optional.of(userId);
                }
            }
            return Optional.of("system"); // consumer RabbitMQ / nội bộ
        };
    }
}
