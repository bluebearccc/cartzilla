package com.cartzilla.product.infrastructure.adapter;

import com.cartzilla.product.domain.entity.Product;
import com.cartzilla.product.domain.repository.ProductRepository;
import com.cartzilla.product.infrastructure.persistence.ProductJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpa;

    @Override public Product save(Product p) { return jpa.save(p); }

    @Override public Optional<Product> findById(UUID id) { return jpa.findById(id); }

    @Override public Optional<Product> findBySlug(String slug) { return jpa.findBySlug(slug); }

    @Override public List<Product> findByCategory(UUID categoryId) {
        return jpa.findByCategoryIdAndActiveTrue(categoryId);
    }

    @Override public List<Product> findAllActive() { return jpa.findAllActive(); }
}
