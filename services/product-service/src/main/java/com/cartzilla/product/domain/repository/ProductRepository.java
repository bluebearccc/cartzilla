package com.cartzilla.product.domain.repository;

import com.cartzilla.product.domain.entity.Product;

import java.util.List;
import java.util.Optional;

/** PORT. */
public interface ProductRepository {
    Product save(Product product);
    Optional<Product> findById(String id);
    List<Product> findByCategory(String categoryId);
    List<Product> findAllActive();
}
