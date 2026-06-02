package com.cartzilla.events.payment;

import java.math.BigDecimal;
import java.util.UUID;

/** Event DTO liên quan thanh toán. */
public class PaymentEvents {
    private PaymentEvents() {}

    public record PaymentProcessEvent(UUID orderId, UUID userId, BigDecimal amount, String method) {}

    public record PaymentResultEvent(UUID orderId, boolean success, String transactionId) {}
}
