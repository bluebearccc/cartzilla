package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID>,
        JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    /** PA-04: slug unique */
    boolean existsBySlug(String slug);

    /** CA-04/BR-P07: còn product active thuộc category? */
    boolean existsByCategoryIdAndActiveTrue(UUID categoryId);
}
