package com.cartzilla.events.order;

import java.util.UUID;

/** Event DTO báo kết quả / chuyển trạng thái đơn hàng cho các service khác. */
public class OrderEvents {
    private OrderEvents() {}

    /** order.confirmed — recipientUserId để notification-service tra email + tạo in-app notification. */
    public record OrderConfirmedEvent(UUID orderId, UUID recipientUserId) {}

    public record OrderCancelledEvent(UUID orderId, UUID recipientUserId, String reason) {}

    /** order.shipped — staff chuyển CONFIRMED → SHIPPING (BR-N02). */
    public record OrderShippedEvent(UUID orderId, UUID recipientUserId) {}

    /** order.delivered — payment-service dùng để chuyển COD payment sang PAID (BR-PY07). */
    public record OrderDeliveredEvent(UUID orderId, UUID recipientUserId) {}
}
