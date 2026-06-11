package com.cartzilla.order.infrastructure.persistence;

import com.cartzilla.order.domain.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemJpaRepository extends JpaRepository<CartItem, UUID> {
    Optional<CartItem> findByUserIdAndSku(UUID userId, String sku);
    List<CartItem> findByUserId(UUID userId);
    void deleteByUserIdAndSku(UUID userId, String sku);
    void deleteByUserId(UUID userId);
}
