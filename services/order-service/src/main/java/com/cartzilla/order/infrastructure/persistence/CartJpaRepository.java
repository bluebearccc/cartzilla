package com.cartzilla.order.infrastructure.persistence;

import com.cartzilla.order.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartJpaRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByUserIdAndSkuIgnoreCase(UUID userId, String sku);
    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);
    List<CartItem> findByUserId(UUID userId);
}
