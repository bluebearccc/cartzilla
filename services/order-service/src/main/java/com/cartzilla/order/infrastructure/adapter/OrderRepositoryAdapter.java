package com.cartzilla.order.infrastructure.adapter;

import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.SagaState;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.OrderSearchCriteria;
import com.cartzilla.order.domain.repository.SagaStateRepository;
import com.cartzilla.order.infrastructure.persistence.OrderJpaRepository;
import com.cartzilla.order.infrastructure.persistence.SagaStateJpaRepository;
import com.cartzilla.order.infrastructure.specification.OrderSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Gộp 2 adapter cho gọn (Order + SagaState). */
@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository, SagaStateRepository {

    private final OrderJpaRepository orderJpa;
    private final SagaStateJpaRepository sagaJpa;

    @Override public Order save(Order order) { return orderJpa.save(order); }
    @Override public Optional<Order> findById(UUID id) { return orderJpa.findById(id); }
    @Override public List<Order> findByUserId(UUID userId) { return orderJpa.findByUserId(userId); }

    @Override public Page<Order> search(OrderSearchCriteria criteria, Pageable pageable) {
        return orderJpa.findAll(OrderSpecifications.from(criteria), pageable);
    }

    @Override
    public com.cartzilla.order.domain.vo.UserOrderStats getUserOrderStats(UUID userId, UUID excludeOrderId) {
        long nonCancelledCount = orderJpa.countNonCancelledOrders(userId, excludeOrderId);
        List<Object[]> completedStats = orderJpa.getCompletedOrderStats(userId, excludeOrderId);
        long completedCount = 0;
        java.math.BigDecimal totalSpent = java.math.BigDecimal.ZERO;
        if (completedStats != null && !completedStats.isEmpty() && completedStats.get(0) != null) {
            Object[] row = completedStats.get(0);
            completedCount = ((Number) row[0]).longValue();
            totalSpent = (java.math.BigDecimal) row[1];
        }
        return new com.cartzilla.order.domain.vo.UserOrderStats(nonCancelledCount, completedCount, totalSpent);
    }

    @Override public SagaState save(SagaState saga) { return sagaJpa.save(saga); }
    @Override public Optional<SagaState> findByOrderId(UUID orderId) { return sagaJpa.findByOrderId(orderId); }
}
