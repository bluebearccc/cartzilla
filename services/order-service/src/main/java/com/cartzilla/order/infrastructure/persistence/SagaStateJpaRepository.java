package com.cartzilla.order.infrastructure.persistence;

import com.cartzilla.order.domain.entity.SagaState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SagaStateJpaRepository extends JpaRepository<SagaState, UUID> {
    Optional<SagaState> findByOrderId(UUID orderId);
}
