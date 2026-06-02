package com.cartzilla.order.infrastructure.adapter;

import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.SagaState;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.SagaStateRepository;
import com.cartzilla.order.infrastructure.persistence.OrderJpaRepository;
import com.cartzilla.order.infrastructure.persistence.SagaStateJpaRepository;
import lombok.RequiredArgsConstructor;
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

    @Override public SagaState save(SagaState saga) { return sagaJpa.save(saga); }
    @Override public Optional<SagaState> findByOrderId(UUID orderId) { return sagaJpa.findByOrderId(orderId); }
}
