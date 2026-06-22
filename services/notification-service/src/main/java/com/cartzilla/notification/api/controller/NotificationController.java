package com.cartzilla.notification.api.controller;

import com.cartzilla.notification.api.dto.NotificationDtos;
import com.cartzilla.notification.application.usecase.ListNotificationsUseCase;
import com.cartzilla.notification.application.usecase.MarkNotificationReadUseCase;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * F12 — UC-08: API in-app notification cho customer.
 * Gateway validate JWT + inject X-User-Id; chỉ trả notification của chính user.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final MarkNotificationReadUseCase markNotificationReadUseCase;

    /** GET /api/notifications?page=&size= — danh sách notification của user, mới nhất trước. */
    @GetMapping
    public ApiResponse<NotificationDtos.NotificationPage> list(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(NotificationDtos.NotificationPage.from(
                listNotificationsUseCase.execute(userId, page, size),
                listNotificationsUseCase.unreadCount(userId)));
    }

    /** PUT /api/notifications/{id}/read — đánh dấu đã đọc (BR-N04). */
    @PutMapping("/{id}/read")
    public ApiResponse<NotificationDtos.NotificationResponse> markRead(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID id) {
        return ApiResponse.ok("Đã đánh dấu đã đọc",
                NotificationDtos.NotificationResponse.from(
                        markNotificationReadUseCase.execute(id, userId)));
    }
}
