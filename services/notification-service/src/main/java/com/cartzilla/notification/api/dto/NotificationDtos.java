package com.cartzilla.notification.api.dto;

import com.cartzilla.notification.domain.entity.Notification;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO cho API in-app notification (F12). */
public final class NotificationDtos {
    private NotificationDtos() {}

    public record NotificationResponse(
            UUID id,
            UUID orderId,
            String type,
            String title,
            String message,
            String status,
            String priority,
            Instant readAt,
            Instant createdAt) {

        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(
                    n.getId(), n.getOrderId(),
                    n.getType() == null ? null : n.getType().name(),
                    n.getTitle(), n.getMessage(),
                    n.getStatus() == null ? null : n.getStatus().name(),
                    n.getPriority() == null ? null : n.getPriority().name(),
                    n.getReadAt(), n.getCreatedAt());
        }
    }

    public record NotificationPage(
            List<NotificationResponse> items,
            int page,
            int size,
            long totalItems,
            int totalPages,
            long unreadCount) {

        public static NotificationPage from(Page<Notification> page, long unreadCount) {
            return new NotificationPage(
                    page.getContent().stream().map(NotificationResponse::from).toList(),
                    page.getNumber(), page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), unreadCount);
        }
    }
}
