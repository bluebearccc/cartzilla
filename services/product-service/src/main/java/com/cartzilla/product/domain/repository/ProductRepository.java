package com.cartzilla.product.domain.repository;

import com.cartzilla.product.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/** PORT — domain định nghĩa, infrastructure implement. */
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Optional<Product> findBySlug(String slug);
    Page<Product> search(ProductSearchCriteria criteria, Pageable pageable);
    boolean existsBySlug(String slug);
    /** PV-01: SKU unique toàn hệ thống */
    boolean existsBySku(String sku);
    /** CA-04/BR-P07: guard trước khi deactivate category */
    boolean existsActiveByCategoryId(UUID categoryId);
}
