package com.cartzilla.order.infrastructure.persistence;

import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.vo.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID>,
        JpaSpecificationExecutor<Order> {
    List<Order> findByUserId(UUID userId);

    // ─── F18 Reports — aggregate read-only (UC-09) ───────────────────────────

    @Query("""
            select coalesce(sum(o.totalAmount), 0) from Order o
            where o.status in :statuses
              and o.createdAt >= :from and o.createdAt <= :to
            """)
    BigDecimal sumRevenue(@Param("statuses") Collection<OrderStatus> statuses,
                          @Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select count(o) from Order o
            where o.createdAt >= :from and o.createdAt <= :to
            """)
    long countOrders(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select o.status, count(o) from Order o
            where o.createdAt >= :from and o.createdAt <= :to
            group by o.status
            """)
    List<Object[]> groupByStatus(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select o.paymentStatus, count(o) from Order o
            where o.createdAt >= :from and o.createdAt <= :to
            group by o.paymentStatus
            """)
    List<Object[]> groupByPaymentStatus(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select o.paymentMethod, count(o), coalesce(sum(o.totalAmount), 0) from Order o
            where o.createdAt >= :from and o.createdAt <= :to
            group by o.paymentMethod
            """)
    List<Object[]> groupByMethod(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            select i.productId, i.name, i.sku, sum(i.quantity), coalesce(sum(i.subtotal), 0)
            from Order o join o.items i
            where o.status in :statuses
              and o.createdAt >= :from and o.createdAt <= :to
            group by i.productId, i.name, i.sku
            order by sum(i.quantity) desc
            """)
    List<Object[]> topProducts(@Param("statuses") Collection<OrderStatus> statuses,
                               @Param("from") Instant from, @Param("to") Instant to,
                               Pageable pageable);
}
