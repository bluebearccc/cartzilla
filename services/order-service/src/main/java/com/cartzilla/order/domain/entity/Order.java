package com.cartzilla.order.domain.entity;

import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.web.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus;

    private BigDecimal subtotal;
    private BigDecimal discount;
    @Column(name = "total_amount")
    private BigDecimal totalAmount;
    @Column(name = "voucher_code")
    private String voucherCode;

    @Column(name = "shipping_address", columnDefinition = "jsonb")
    private String shippingAddress;   // JSON snapshot (BR05)

    private String note;
    @Column(name = "cancelled_reason")
    private String cancelledReason;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public static Order create(UUID userId, String paymentMethod, String shippingAddress,
                               BigDecimal subtotal, BigDecimal discount, String voucherCode) {
        Order o = new Order();
        o.userId = userId;
        o.status = OrderStatus.PENDING;
        o.paymentMethod = paymentMethod;
        o.paymentStatus = "PENDING";
        o.subtotal = subtotal;
        o.discount = discount == null ? BigDecimal.ZERO : discount;
        o.totalAmount = subtotal.subtract(o.discount);
        o.voucherCode = voucherCode;
        o.shippingAddress = shippingAddress;
        return o;
    }

    public void confirm() {
        this.status = OrderStatus.CONFIRMED;
        this.paymentStatus = "PAID";
        this.confirmedAt = Instant.now();
    }

    public void cancel(String reason) {
        this.status = OrderStatus.CANCELLED;
        this.cancelledReason = reason;
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        this.items.add(item);
    }
}
