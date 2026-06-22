package com.cartzilla.order.domain.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * F18 — UC-09: aggregate read-only báo cáo admin từ order/order_item.
 * Doanh thu chỉ tính đơn đã chốt (CONFIRMED/SHIPPING/DELIVERED), loại PENDING/CANCELLED.
 */
public interface ReportRepository {

    BigDecimal totalRevenue(Instant from, Instant to);

    long totalOrders(Instant from, Instant to);

    List<CountByKey> countByStatus(Instant from, Instant to);

    List<CountByKey> countByPaymentStatus(Instant from, Instant to);

    List<MethodRevenue> revenueByMethod(Instant from, Instant to);

    List<TopProduct> topProducts(Instant from, Instant to, int limit);

    record CountByKey(String key, long count) {}

    record MethodRevenue(String method, long count, BigDecimal revenue) {}

    record TopProduct(UUID productId, String name, String sku, long quantitySold, BigDecimal revenue) {}
}
