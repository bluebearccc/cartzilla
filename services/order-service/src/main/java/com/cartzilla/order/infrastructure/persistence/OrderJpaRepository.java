package com.cartzilla.order.infrastructure.persistence;

import com.cartzilla.order.domain.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface OrderJpaRepository extends JpaRepository<Order, UUID>,
        JpaSpecificationExecutor<Order> {
    List<Order> findByUserId(UUID userId);
}
