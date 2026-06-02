package com.cartzilla.order.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

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

    @Column(name = "product_id")
    private String productId;
    private String sku;
    private String name;
    private String image;
    private String size;
    private String color;
    @Column(name = "unit_price")
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;

    public static OrderItem create(String productId, String sku, String name, String image,
                                   String size, String color, BigDecimal unitPrice, int quantity) {
        OrderItem i = new OrderItem();
        i.productId = productId;
        i.sku = sku;
        i.name = name;
        i.image = image;
        i.size = size;
        i.color = color;
        i.unitPrice = unitPrice;
        i.quantity = quantity;
        i.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return i;
    }

    void attachTo(Order order) { this.order = order; }
}
