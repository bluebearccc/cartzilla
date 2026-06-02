package com.cartzilla.events.stock;

import java.util.List;
import java.util.UUID;

/** Event DTO liên quan tới giữ/trả tồn kho (publisher: order/product). */
public class StockEvents {
    private StockEvents() {}

    public record Item(String sku, int quantity) {}

    public record StockReserveEvent(UUID orderId, List<Item> items) {}

    public record StockReservedEvent(UUID orderId, boolean success, String failedSku) {}

    public record StockReleaseEvent(UUID orderId, List<Item> items) {}
}
