package com.cartzilla.product.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root — ProductAggregate.
 * Root + List<ProductVariant> + List<ProductImage>.
 * Rules: PA-01..PA-07, P-01..P-05.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** P-01: FK → categories.id — category phải active khi tạo */
    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    /** P-04: optional; vendor phải active nếu set */
    @Column(name = "vendor_id")
    private UUID vendorId;

    /** P-03: NOT NULL, max 200 */
    @Column(nullable = false, length = 200)
    private String name;

    /** P-03/PA-04: unique, max 220 */
    @Column(nullable = false, length = 220, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** P-02: ≥ 0 */
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(columnDefinition = "TEXT[]")
    private String tags;

    @Column(nullable = false)
    private boolean active = true;

    /** P-05: featured chỉ khi active */
    @Column(nullable = false)
    private boolean featured = false;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> variants = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ProductImage> images = new ArrayList<>();

    public static Product create(UUID categoryId, String name, String slug,
                                  String description, BigDecimal basePrice,
                                  UUID vendorId, String tags) {
        if (categoryId == null)
            throw new BusinessException("categoryId must not be null (P-01)");
        if (name == null || name.isBlank())
            throw new BusinessException("Product name must not be blank (P-03)");
        if (slug == null || slug.isBlank())
            throw new BusinessException("Product slug must not be blank (P-03)");
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("basePrice must be >= 0 (P-02)");
        Product p = new Product();
        p.categoryId = categoryId;
        p.name = name.trim();
        p.slug = slug.trim().toLowerCase();
        p.description = description;
        p.basePrice = basePrice;
        p.vendorId = vendorId;
        p.tags = tags;
        p.active = true;
        p.featured = false;
        return p;
    }

    /** PA-05: chỉ một primary image */
    public void addImage(ProductImage image) {
        if (image.isPrimary()) {
            images.forEach(i -> i.unsetPrimary());
        }
        image.attachTo(this);
        images.add(image);
    }

    /** PV-04: guard — không xóa variant cuối nếu còn active */
    public void addVariant(ProductVariant variant) {
        variant.attachTo(this);
        variants.add(variant);
    }

    /** PA-01: sellable = active + ≥1 active variant + ≥1 image */
    public boolean isSellable() {
        boolean hasActiveVariant = variants.stream().anyMatch(v -> !v.isDeleted());
        return active && hasActiveVariant && !images.isEmpty();
    }

    /** P-05 */
    public void setFeatured(boolean featured) {
        if (featured && !active)
            throw new BusinessException("Cannot feature an inactive product (P-05)");
        this.featured = featured;
    }

    public void deactivate() {
        this.active = false;
        this.featured = false;
    }

    public void activate() { this.active = true; }
}
