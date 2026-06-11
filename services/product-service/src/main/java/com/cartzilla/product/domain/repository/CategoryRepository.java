package com.cartzilla.product.domain.repository;

import com.cartzilla.product.domain.entity.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** PORT — domain định nghĩa, infrastructure implement. */
public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    Optional<Category> findBySlug(String slug);
    List<Category> findAllActive();
    List<Category> findAll();
    boolean existsBySlug(String slug);
    /** Guard xóa category cha khi còn category con */
    boolean existsByParentId(UUID parentId);
}
