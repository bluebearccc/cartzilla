package com.cartzilla.order.application.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderCommand {
    private OrderCommand() {}

    public record Line(String productId, String sku, String name, String image,
                       String size, String color, BigDecimal unitPrice, int quantity) {}

    public record Checkout(UUID userId, List<Line> lines, String shippingAddress,
                           String paymentMethod, String voucherCode) {}
}
