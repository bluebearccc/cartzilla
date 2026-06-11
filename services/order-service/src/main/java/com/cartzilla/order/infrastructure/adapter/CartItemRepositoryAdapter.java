package com.cartzilla.order.infrastructure.adapter;

import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartItemRepository;
import com.cartzilla.order.infrastructure.persistence.CartItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartItemRepositoryAdapter implements CartItemRepository {

    private final CartItemJpaRepository jpa;

    @Override
    public CartItem save(CartItem item) {
        return jpa.save(item);
    }

    @Override
    public Optional<CartItem> findByUserIdAndSku(UUID userId, String sku) {
        return jpa.findByUserIdAndSku(userId, sku);
    }

    @Override
    public List<CartItem> findByUserId(UUID userId) {
        return jpa.findByUserId(userId);
    }

    @Override
    public void deleteByUserIdAndSku(UUID userId, String sku) {
        jpa.deleteByUserIdAndSku(userId, sku);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpa.deleteByUserId(userId);
    }
}
