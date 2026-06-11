package com.cartzilla.product.infrastructure.adapter;

import com.cartzilla.product.domain.entity.Category;
import com.cartzilla.product.domain.repository.CategoryRepository;
import com.cartzilla.product.infrastructure.persistence.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpa;

    @Override public Category save(Category c) { return jpa.save(c); }

    @Override public Optional<Category> findById(UUID id) { return jpa.findById(id); }

    @Override public Optional<Category> findBySlug(String slug) { return jpa.findBySlug(slug); }

    @Override public List<Category> findAllActive() {
        return jpa.findByActiveTrueOrderBySortOrderAscNameAsc();
    }

    @Override public List<Category> findAll() {
        return jpa.findAllByOrderBySortOrderAscNameAsc();
    }

    @Override public boolean existsBySlug(String slug) { return jpa.existsBySlug(slug); }

    @Override public boolean existsByParentId(UUID parentId) { return jpa.existsByParentId(parentId); }
}
