package com.cartzilla.order.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Aggregate root — CartAggregate (nhóm theo userId).
 * Rules: CTA-01..CTA-05, CI-01..CI-03.
 * UNIQUE (user_id, sku) — CTA-01.
 */
@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(name = "uq_cart_user_sku",
                columnNames = {"user_id", "sku"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** CTA-04: ref user-service */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** CTA-05: snapshot từ product-service */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** CI-03: NOT NULL */
    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String image;

    @Column(length = 20)
    private String size;

    @Column(length = 50)
    private String color;

    /** CI-02: ≥ 0 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** CI-01: > 0 CHECK DB */
    @Column(nullable = false)
    private int quantity;

    public static CartItem create(UUID userId, UUID productId, String sku, String name,
                                   String image, String size, String color,
                                   BigDecimal price, int quantity) {
        if (userId == null) throw new BusinessException("userId must not be null");
        if (productId == null) throw new BusinessException("productId must not be null");
        if (sku == null || sku.isBlank()) throw new BusinessException("sku must not be blank (CI-03)");
        if (name == null || name.isBlank()) throw new BusinessException("name must not be blank (CI-03)");
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("price must be >= 0 (CI-02)");
        if (quantity <= 0) throw new BusinessException("quantity must be > 0 (CI-01)");
        CartItem ci = new CartItem();
        ci.userId = userId;
        ci.productId = productId;
        ci.sku = sku.toUpperCase();
        ci.name = name;
        ci.image = image;
        ci.size = size;
        ci.color = color;
        ci.price = price;
        ci.quantity = quantity;
        return ci;
    }

    /** CTA-01: thêm lại cùng SKU → cộng quantity */
    public void addQuantity(int delta) {
        if (delta <= 0) throw new BusinessException("delta must be > 0");
        this.quantity += delta;
    }

    /** CTA-02: update quantity = 0 → caller phải soft-delete */
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 0) throw new BusinessException("quantity must be >= 0 (CTA-02)");
        this.quantity = newQuantity;
    }

    /** CTA-03: refresh price snapshot khi checkout */
    public void refreshPrice(BigDecimal newPrice) {
        if (newPrice == null || newPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("price must be >= 0");
        this.price = newPrice;
    }
}
