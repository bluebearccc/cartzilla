package com.cartzilla.product.infrastructure.adapter;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.domain.repository.ProductSearchCriteria;
import com.cartzilla.product.infrastructure.persistence.ProductJpaRepository;
import com.cartzilla.product.infrastructure.persistence.ProductVariantJpaRepository;
import com.cartzilla.product.infrastructure.specification.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;
    private final ProductVariantJpaRepository variantJpa;

    @Override public Product save(Product p) { return jpa.save(p); }

    @Override public Optional<Product> findById(UUID id) { return jpa.findById(id); }

    @Override public Optional<Product> findBySlug(String slug) { return jpa.findBySlug(slug); }

    @Override public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) {
        return jpa.findAll(ProductSpecifications.from(criteria), pageable);
    }

    @Override public boolean existsBySlug(String slug) { return jpa.existsBySlug(slug); }

    @Override public boolean existsBySku(String sku) {
        return variantJpa.existsBySkuIgnoreCase(sku);
    }

    @Override public boolean existsActiveByCategoryId(UUID categoryId) {
        return jpa.existsByCategoryIdAndActiveTrue(categoryId);
    }
}
