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
import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root — ProductAggregate.
 * Root + List&lt;ProductVariant&gt; + List&lt;ProductImage&gt;.
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

    /** Tags phân tách bằng dấu phẩy, vd "ao,thun,basic" */
    @Column(columnDefinition = "TEXT")
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

    /** Admin cập nhật thông tin chung — P-01..P-04 validate lại */
    public void update(UUID categoryId, UUID vendorId, String name,
                       String description, BigDecimal basePrice, String tags) {
        if (categoryId == null)
            throw new BusinessException("categoryId must not be null (P-01)");
        if (name == null || name.isBlank())
            throw new BusinessException("Product name must not be blank (P-03)");
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("basePrice must be >= 0 (P-02)");
        this.categoryId = categoryId;
        this.vendorId = vendorId;
        this.name = name.trim();
        this.description = description;
        this.basePrice = basePrice;
        this.tags = tags;
    }

    /** PA-04: slug unique — usecase chịu trách nhiệm check trùng trước khi đổi */
    public void changeSlug(String slug) {
        if (slug == null || slug.isBlank())
            throw new BusinessException("Product slug must not be blank (P-03)");
        this.slug = slug.trim().toLowerCase();
    }

    /** PA-05: chỉ một primary image */
    public void addImage(ProductImage image) {
        if (image.isPrimary()) {
            images.forEach(ProductImage::unsetPrimary);
        }
        image.attachTo(this);
        images.add(image);
    }

    /** PV-05: combo (size, color) unique trong cùng product */
    public void addVariant(ProductVariant variant) {
        assertComboUnique(null, variant.getSize(), variant.getColor());
        variant.attachTo(this);
        variants.add(variant);
    }

    public ProductVariant findVariant(UUID variantId) {
        return variants.stream()
                .filter(v -> Objects.equals(v.getId(), variantId) && !v.isDeleted())
                .findFirst()
                .orElseThrow(() -> new BusinessException("Variant not found: " + variantId));
    }

    /** PV-02/PV-03/PV-05 — SKU immutable sau khi tạo */
    public void updateVariant(UUID variantId, String size, String color,
                              String colorHex, BigDecimal price, int stock) {
        ProductVariant target = findVariant(variantId);
        assertComboUnique(target, size, color);
        target.update(size, color, colorHex, price, stock);
    }

    /** PV-04: không xóa variant cuối nếu product còn active */
    public void removeVariant(UUID variantId) {
        ProductVariant target = findVariant(variantId);
        long remaining = variants.stream().filter(v -> !v.isDeleted()).count();
        if (active && remaining <= 1)
            throw new BusinessException("Cannot remove the last variant of an active product (PV-04)");
        target.softDelete();
    }

    public ProductImage findImage(UUID imageId) {
        return images.stream()
                .filter(i -> Objects.equals(i.getId(), imageId) && !i.isDeleted())
                .findFirst()
                .orElseThrow(() -> new BusinessException("Image not found: " + imageId));
    }

    /** PA-05: nếu xóa primary image thì promote ảnh còn lại */
    public void removeImage(UUID imageId) {
        ProductImage target = findImage(imageId);
        target.softDelete();
        target.unsetPrimary();
        ensurePrimaryImage();
    }

    /** PI-03: set primary mới → unset toàn bộ primary cũ */
    public void setPrimaryImage(UUID imageId) {
        ProductImage target = findImage(imageId);
        images.forEach(ProductImage::unsetPrimary);
        target.markPrimary();
    }

    /** PA-05: đảm bảo có đúng một primary nếu còn ảnh */
    public void ensurePrimaryImage() {
        List<ProductImage> alive = images.stream().filter(i -> !i.isDeleted()).toList();
        if (alive.isEmpty()) return;
        if (alive.stream().noneMatch(ProductImage::isPrimary)) {
            alive.get(0).markPrimary();
        }
    }

    /** PA-01: sellable = active + ≥1 active variant + ≥1 image */
    public boolean isSellable() {
        boolean hasActiveVariant = variants.stream().anyMatch(v -> !v.isDeleted());
        boolean hasImage = images.stream().anyMatch(i -> !i.isDeleted());
        return active && hasActiveVariant && hasImage;
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

    private void assertComboUnique(ProductVariant exclude, String size, String color) {
        boolean exists = variants.stream().anyMatch(v -> v != exclude && !v.isDeleted()
                && Objects.equals(normalize(v.getSize()), normalize(size))
                && Objects.equals(normalize(v.getColor()), normalize(color)));
        if (exists)
            throw new BusinessException("Variant with same size/color already exists (PV-05)");
    }

    private static String normalize(String s) {
        return s == null ? null : s.trim().toLowerCase();
    }
}
