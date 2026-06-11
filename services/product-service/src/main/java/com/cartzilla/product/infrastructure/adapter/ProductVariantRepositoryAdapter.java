package com.cartzilla.product.infrastructure.adapter;

import com.cartzilla.product.domain.entity.ProductVariant;
import com.cartzilla.product.domain.repository.ProductVariantRepository;
import com.cartzilla.product.infrastructure.persistence.ProductVariantJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** ADAPTER — implements ProductVariantRepository port, map sang Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class ProductVariantRepositoryAdapter implements ProductVariantRepository {

    private final ProductVariantJpaRepository jpa;

    @Override
    public Optional<ProductVariant> findBySku(String sku) {
        return jpa.findBySkuIgnoreCase(sku);
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpa.existsBySkuIgnoreCase(sku);
    }
}
