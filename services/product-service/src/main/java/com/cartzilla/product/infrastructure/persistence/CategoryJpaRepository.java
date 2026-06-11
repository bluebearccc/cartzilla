package com.cartzilla.product.infrastructure.persistence;

import com.cartzilla.product.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<Category, UUID> {

    Optional<Category> findBySlug(String slug);

    /** CA-01: slug unique */
    boolean existsBySlug(String slug);

    boolean existsByParentId(UUID parentId);

    /** CA-05: sibling sort theo sortOrder */
    List<Category> findByActiveTrueOrderBySortOrderAscNameAsc();

    List<Category> findAllByOrderBySortOrderAscNameAsc();
}
