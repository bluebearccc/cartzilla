package com.cartzilla.order.infrastructure.persistence;

import com.cartzilla.order.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID> {
    List<Order> findByUserId(UUID userId);
}
