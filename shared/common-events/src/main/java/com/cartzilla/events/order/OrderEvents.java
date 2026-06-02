package com.cartzilla.events.order;

import java.util.UUID;

/** Event DTO báo kết quả đơn hàng cho notification-service. */
public class OrderEvents {
    private OrderEvents() {}

    public record OrderConfirmedEvent(UUID orderId, String recipientEmail) {}

    public record OrderCancelledEvent(UUID orderId, String reason, String recipientEmail) {}
}
