package com.cartzilla.product.domain.repository;

import com.cartzilla.product.domain.entity.ProductVariant;

import java.util.Optional;

/**
 * PORT — domain định nghĩa, infrastructure implement.
 * Tách riêng để lookup/giữ tồn kho theo SKU (Saga reserve/release, internal snapshot)
 * không phải phụ thuộc trực tiếp vào JpaRepository (Hexagonal — Architecture §3).
 */
public interface ProductVariantRepository {

    /** PV-01: SKU unique toàn hệ thống — bỏ qua bản ghi soft-deleted. */
    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);
}
