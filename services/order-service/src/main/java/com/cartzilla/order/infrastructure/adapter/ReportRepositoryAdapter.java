package com.cartzilla.order.infrastructure.adapter;

import com.cartzilla.order.domain.repository.ReportRepository;
import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.infrastructure.persistence.OrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReportRepositoryAdapter implements ReportRepository {

    /** Đơn đã chốt được tính doanh thu (loại PENDING/CANCELLED). */
    private static final Set<OrderStatus> REVENUE_STATUSES =
            EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.SHIPPING, OrderStatus.DELIVERED);

    /** Biên thời gian mặc định khi filter null (tránh untyped-null param trên Postgres). */
    private static final Instant MIN = Instant.EPOCH;
    private static final Instant MAX = Instant.parse("2999-12-31T23:59:59Z");

    private final OrderJpaRepository jpa;

    private static Instant from(Instant f) { return f != null ? f : MIN; }
    private static Instant to(Instant t) { return t != null ? t : MAX; }

    @Override
    public BigDecimal totalRevenue(Instant from, Instant to) {
        BigDecimal v = jpa.sumRevenue(REVENUE_STATUSES, from(from), to(to));
        return v == null ? BigDecimal.ZERO : v;
    }

    @Override
    public long totalOrders(Instant from, Instant to) {
        return jpa.countOrders(from(from), to(to));
    }

    @Override
    public List<CountByKey> countByStatus(Instant from, Instant to) {
        return jpa.groupByStatus(from(from), to(to)).stream()
                .map(r -> new CountByKey(String.valueOf(r[0]), ((Number) r[1]).longValue()))
                .toList();
    }

    @Override
    public List<CountByKey> countByPaymentStatus(Instant from, Instant to) {
        return jpa.groupByPaymentStatus(from(from), to(to)).stream()
                .map(r -> new CountByKey(String.valueOf(r[0]), ((Number) r[1]).longValue()))
                .toList();
    }

    @Override
    public List<MethodRevenue> revenueByMethod(Instant from, Instant to) {
        return jpa.groupByMethod(from(from), to(to)).stream()
                .map(r -> new MethodRevenue(
                        String.valueOf(r[0]),
                        ((Number) r[1]).longValue(),
                        (BigDecimal) r[2]))
                .toList();
    }

    @Override
    public List<TopProduct> topProducts(Instant from, Instant to, int limit) {
        int safeLimit = limit <= 0 ? 10 : Math.min(limit, 100);
        return jpa.topProducts(REVENUE_STATUSES, from(from), to(to), PageRequest.of(0, safeLimit)).stream()
                .map(r -> new TopProduct(
                        (UUID) r[0],
                        String.valueOf(r[1]),
                        String.valueOf(r[2]),
                        ((Number) r[3]).longValue(),
                        (BigDecimal) r[4]))
                .toList();
    }
}
