package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariant, UUID> {

    /** PV-01: SKU unique */
    Optional<ProductVariant> findBySkuIgnoreCase(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE UPPER(v.sku) = UPPER(:sku)")
    Optional<ProductVariant> findBySkuForUpdate(@Param("sku") String sku);

    /** PV-01: check trước khi thêm variant mới */
    boolean existsBySkuIgnoreCase(String sku);

    /** Lấy variants của một product, eager load product để lấy snapshot */
    @Query("SELECT v FROM ProductVariant v JOIN FETCH v.product WHERE v.product.id = :productId")
    java.util.List<ProductVariant> findByProductIdWithProduct(@Param("productId") UUID productId);
}
