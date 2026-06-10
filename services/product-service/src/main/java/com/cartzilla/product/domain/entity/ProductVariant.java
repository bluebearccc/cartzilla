package com.cartzilla.product.domain.entity;

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
 * Child entity của ProductAggregate.
 * Rules: PV-01..PV-05, PA-02a, PA-07.
 */
@Entity
@Table(name = "product_variants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class ProductVariant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** PV-01: unique toàn hệ thống */
    @Column(nullable = false, length = 50, unique = true)
    private String sku;

    @Column(length = 20)
    private String size;

    @Column(length = 50)
    private String color;

    @Column(name = "color_hex", length = 10)
    private String colorHex;

    /** PV-02: ≥ 0 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    /** PV-03: ≥ 0 — CHECK DB */
    @Column(nullable = false)
    private int stock = 0;

    public static ProductVariant create(String sku, String size, String color,
                                         String colorHex, BigDecimal price, int stock) {
        if (sku == null || sku.isBlank())
            throw new BusinessException("SKU must not be blank (PV-01)");
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("price must be >= 0 (PV-02)");
        if (stock < 0) throw new BusinessException("stock must be >= 0 (PV-03)");
        ProductVariant pv = new ProductVariant();
        pv.sku = sku.trim().toUpperCase();
        pv.size = size;
        pv.color = color;
        pv.colorHex = colorHex;
        pv.price = price;
        pv.stock = stock;
        return pv;
    }

    /** PV-02/PV-03: admin cập nhật variant — SKU immutable sau khi tạo */
    public void update(String size, String color, String colorHex, BigDecimal price, int stock) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("price must be >= 0 (PV-02)");
        if (stock < 0) throw new BusinessException("stock must be >= 0 (PV-03)");
        this.size = size;
        this.color = color;
        this.colorHex = colorHex;
        this.price = price;
        this.stock = stock;
    }

    /** PA-07: giảm stock; từ chối nếu không đủ */
    public void reserveStock(int quantity) {
        if (quantity <= 0) throw new BusinessException("Quantity to reserve must be > 0");
        if (stock < quantity)
            throw new BusinessException("Insufficient stock for SKU " + sku +
                    " (available: " + stock + ", requested: " + quantity + ") (PA-07)");
        this.stock -= quantity;
    }

    /** Compensation: hoàn trả stock khi cancel */
    public void releaseStock(int quantity) {
        if (quantity <= 0) throw new BusinessException("Quantity to release must be > 0");
        this.stock += quantity;
    }

    void attachTo(Product product) { this.product = product; }
}
