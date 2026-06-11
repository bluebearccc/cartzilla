package com.cartzilla.order.application.command;

import com.cartzilla.order.domain.vo.PaymentMethod;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderCommand {
    private OrderCommand() {}

    public record Line(String productId, String sku, String name, String image,
                       String size, String color, BigDecimal unitPrice, int quantity) {}

    public record Checkout(UUID userId, List<Line> lines, String shippingAddress,
                           String paymentMethod, String voucherCode) {}

    public record SkuLine(String sku, int quantity) {}

    public record CheckoutBySku(UUID userId, List<SkuLine> lines, String shippingAddress,
                                PaymentMethod paymentMethod, String voucherCode) {}

    /** F10 — staff đổi trạng thái đơn (reason bắt buộc khi CANCELLED — OA-07). */
    public record UpdateStatus(UUID orderId, String targetStatus, String reason, UUID changedBy) {}

    /** F09 — customer hủy đơn của mình ở trạng thái PENDING. */
    public record CancelByCustomer(UUID orderId, UUID userId, String reason) {}
}
