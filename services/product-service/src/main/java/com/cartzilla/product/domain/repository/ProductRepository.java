package com.cartzilla.product.domain.repository;

import com.cartzilla.product.domain.entity.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** PORT — domain định nghĩa, infrastructure implement. */
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(UUID id);
    Optional<Product> findBySlug(String slug);
    List<Product> findByCategory(UUID categoryId);
    List<Product> findAllActive();
}
