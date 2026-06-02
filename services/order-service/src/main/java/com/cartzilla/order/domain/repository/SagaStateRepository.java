package com.cartzilla.order.domain.repository;

import com.cartzilla.order.domain.entity.SagaState;

import java.util.Optional;
import java.util.UUID;

public interface SagaStateRepository {
    SagaState save(SagaState saga);
    Optional<SagaState> findByOrderId(UUID orderId);
}
