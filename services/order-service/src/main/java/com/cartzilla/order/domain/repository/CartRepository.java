package com.cartzilla.order.domain.repository;

import com.cartzilla.order.domain.entity.CartItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** PORT — giỏ hàng theo user (F05 / UC-03). */
public interface CartRepository {
    CartItem save(CartItem item);

    /** CTA-01: tra cứu dòng cart theo (userId, sku) để cộng dồn. */
    Optional<CartItem> findByUserIdAndSku(UUID userId, String sku);

    /** Lấy 1 dòng cart của đúng user (ownership guard). */
    Optional<CartItem> findByIdAndUserId(UUID id, UUID userId);

    List<CartItem> findByUserId(UUID userId);

    /** Cart là dữ liệu tạm → hard-delete để tránh xung đột UNIQUE(user_id, sku) khi thêm lại. */
    void remove(CartItem item);
}
