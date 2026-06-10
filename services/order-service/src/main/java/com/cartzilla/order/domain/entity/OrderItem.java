package com.cartzilla.order.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Child entity của OrderAggregate.
 * Rules: OI-01..OI-05.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /** OI-05: ref product-service — không FK cross-DB */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** OI-04: immutable snapshot */
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

    /** OI-02: ≥ 0 */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** OI-01: > 0 */
    @Column(nullable = false)
    private int quantity;

    /** OI-03: unitPrice × quantity */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    public static OrderItem create(UUID productId, String sku, String name, String image,
                                    String size, String color, BigDecimal unitPrice, int quantity) {
        if (productId == null) throw new BusinessException("productId must not be null (OI-05)");
        if (quantity <= 0) throw new BusinessException("quantity must be > 0 (OI-01)");
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("unitPrice must be >= 0 (OI-02)");
        OrderItem i = new OrderItem();
        i.productId = productId;
        i.sku = sku;
        i.name = name;
        i.image = image;
        i.size = size;
        i.color = color;
        i.unitPrice = unitPrice;
        i.quantity = quantity;
        // OI-03: domain computes subtotal
        i.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return i;
    }

    void attachTo(Order order) { this.order = order; }
}
