package com.cartzilla.order.infrastructure.adapter;

import com.cartzilla.order.domain.entity.CartItem;
import com.cartzilla.order.domain.repository.CartRepository;
import com.cartzilla.order.infrastructure.persistence.CartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CartRepositoryAdapter implements CartRepository {

    private final CartJpaRepository jpa;

    @Override public CartItem save(CartItem item) { return jpa.save(item); }

    @Override public Optional<CartItem> findByUserIdAndSku(UUID userId, String sku) {
        return jpa.findByUserIdAndSkuIgnoreCase(userId, sku);
    }

    @Override public Optional<CartItem> findByIdAndUserId(UUID id, UUID userId) {
        return jpa.findByIdAndUserId(id, userId);
    }

    @Override public List<CartItem> findByUserId(UUID userId) { return jpa.findByUserId(userId); }

    @Override public void remove(CartItem item) { jpa.delete(item); }
}
