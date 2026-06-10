package com.cartzilla.product.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

/**
 * Aggregate root — CategoryAggregate (cây category tự tham chiếu).
 * Rules: CA-01..CA-05, C-01..C-02.
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** CA-02: không tự trỏ về chính mình */
    @Column(name = "parent_id")
    private UUID parentId;

    /** C-01: NOT NULL, max 100 */
    @Column(nullable = false, length = 100)
    private String name;

    /** CA-01: unique, NOT NULL */
    @Column(nullable = false, length = 120, unique = true)
    private String slug;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private boolean active = true;

    /** CA-05: ≥ 0 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public static Category create(String name, String slug, UUID parentId,
                                   String imageUrl, int sortOrder) {
        if (name == null || name.isBlank())
            throw new BusinessException("Category name must not be blank (C-01)");
        if (slug == null || slug.isBlank())
            throw new BusinessException("Category slug must not be blank (CA-01)");
        if (sortOrder < 0) throw new BusinessException("sortOrder must be >= 0 (CA-05)");
        Category c = new Category();
        c.name = name.trim();
        c.slug = slug.trim().toLowerCase();
        c.parentId = parentId;
        c.imageUrl = imageUrl;
        c.sortOrder = sortOrder;
        c.active = true;
        return c;
    }

    /** CA-02: guard — không set parentId = self */
    public void setParent(UUID parentId) {
        if (id != null && id.equals(parentId))
            throw new BusinessException("Category cannot reference itself (CA-02)");
        this.parentId = parentId;
    }

    public void deactivate() { this.active = false; }
    public void activate() { this.active = true; }
}
