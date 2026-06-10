package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductJpaRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    /** PA-01: chỉ lấy product active và category active */
    @Query("SELECT p FROM Product p WHERE p.active = true")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p WHERE p.categoryId = :categoryId AND p.active = true")
    List<Product> findByCategoryIdAndActiveTrue(@Param("categoryId") UUID categoryId);
}
