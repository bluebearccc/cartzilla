package com.cartzilla.order.domain.repository;

import com.cartzilla.order.domain.entity.CartItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository {
    CartItem save(CartItem item);
    Optional<CartItem> findByUserIdAndSku(UUID userId, String sku);
    List<CartItem> findByUserId(UUID userId);
    void deleteByUserIdAndSku(UUID userId, String sku);
    void deleteByUserId(UUID userId);
}
