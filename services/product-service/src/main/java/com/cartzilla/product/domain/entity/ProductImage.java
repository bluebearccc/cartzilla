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
 * Child entity của ProductAggregate.
 * Rules: PI-01..PI-03, PA-05.
 */
@Entity
@Table(name = "product_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class ProductImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** PI-01: NOT NULL */
    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "alt_text", length = 200)
    private String altText;

    /** PI-03/PA-05: chỉ một primary per product */
    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    /** PI-02: ≥ 0 */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public static ProductImage create(String imageUrl, String altText,
                                       boolean isPrimary, int sortOrder) {
        if (imageUrl == null || imageUrl.isBlank())
            throw new BusinessException("imageUrl must not be blank (PI-01)");
        if (sortOrder < 0) throw new BusinessException("sortOrder must be >= 0 (PI-02)");
        ProductImage pi = new ProductImage();
        pi.imageUrl = imageUrl.trim();
        pi.altText = altText;
        pi.isPrimary = isPrimary;
        pi.sortOrder = sortOrder;
        return pi;
    }

    /** PI-03: gọi từ Product.addImage khi có primary conflict */
    void unsetPrimary() { this.isPrimary = false; }

    void attachTo(Product product) { this.product = product; }
}
