package com.cartzilla.order.domain.entity;

import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.domain.vo.PaymentStatus;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
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

/**
 * Aggregate root — OrderAggregate.
 * Root + List<OrderItem> + List<OrderStatusLog>.
 * Rules: OA-01..OA-10, O-01..O-05.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("is_deleted = false")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** O-01: ref user-service */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** O-04: default PENDING */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** O-02 */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    /** O-03 */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    /** OA-02: Σ item.subtotal */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    /** OA-03: 0 ≤ discount ≤ subtotal */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    /** OA-02: subtotal - discount */
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "voucher_code", length = 50)
    private String voucherCode;

    /** OA-04: JSONB snapshot — BR05 */
    @Column(name = "shipping_address", nullable = false, columnDefinition = "jsonb")
    private String shippingAddress;

    /** O-05 */
    @Column(columnDefinition = "TEXT")
    private String note;

    /** OA-07: bắt buộc khi CANCELLED */
    @Column(name = "cancelled_reason", columnDefinition = "TEXT")
    private String cancelledReason;

    /** OA-05: set khi CONFIRMED, ≥ createdAt */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<OrderStatusLog> statusLogs = new ArrayList<>();

    /**
     * Factory — enforce OA-01..OA-05.
     * DomainModel section 8 example.
     */
    public static Order create(UUID userId, PaymentMethod paymentMethod, String shippingAddress,
                                List<OrderItem> items, BigDecimal discount,
                                String voucherCode, String note) {
        // OA-01
        if (items == null || items.isEmpty())
            throw new BusinessException("Order must have at least one item (OA-01)");
        // OA-04
        if (shippingAddress == null || shippingAddress.isBlank())
            throw new BusinessException("shippingAddress must not be null (OA-04)");
        if (userId == null) throw new BusinessException("userId must not be null (O-01)");
        if (paymentMethod == null) throw new BusinessException("paymentMethod must not be null (O-02)");

        BigDecimal sub = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disc = discount == null ? BigDecimal.ZERO : discount;
        // OA-03
        if (disc.compareTo(BigDecimal.ZERO) < 0 || disc.compareTo(sub) > 0)
            throw new BusinessException(
                    "discount must be 0 <= discount <= subtotal (OA-03). subtotal=" + sub + ", discount=" + disc);

        Order o = new Order();
        o.userId = userId;
        o.status = OrderStatus.PENDING;
        o.paymentMethod = paymentMethod;
        o.paymentStatus = PaymentStatus.PENDING;
        o.subtotal = sub;
        o.discount = disc;
        o.totalAmount = sub.subtract(disc);   // OA-02
        o.voucherCode = voucherCode;
        o.shippingAddress = shippingAddress;
        o.note = note;
        items.forEach(o::addItem);
        return o;
    }

    /** OA-S1: PENDING → CONFIRMED */
    public void confirm(UUID changedBy) {
        requireStatus(OrderStatus.PENDING, "confirm");
        Instant now = Instant.now();
        // OA-05/BR-G06: confirmedAt ≥ createdAt
        if (getCreatedAt() != null && now.isBefore(getCreatedAt()))
            throw new BusinessException("confirmedAt cannot be before createdAt (OA-05)");
        this.confirmedAt = now;
        transitionTo(OrderStatus.CONFIRMED, changedBy, "Order confirmed");
        this.paymentStatus = PaymentStatus.PAID;
    }

    /** OA-S2: CONFIRMED → SHIPPING (STAFF/ADMIN only — role check ở application layer) */
    public void ship(UUID changedBy) {
        requireStatus(OrderStatus.CONFIRMED, "ship");
        transitionTo(OrderStatus.SHIPPING, changedBy, "Order shipped");
    }

    /** OA-S3: SHIPPING → DELIVERED */
    public void deliver(UUID changedBy) {
        requireStatus(OrderStatus.SHIPPING, "deliver");
        transitionTo(OrderStatus.DELIVERED, changedBy, "Order delivered");
        if (paymentMethod == PaymentMethod.COD) this.paymentStatus = PaymentStatus.PAID;
    }

    /** OA-S4: PENDING/CONFIRMED → CANCELLED — OA-07: cancelledReason bắt buộc */
    public void cancel(String reason, UUID changedBy) {
        if (reason == null || reason.isBlank())
            throw new BusinessException("cancelledReason is required when cancelling (OA-07)");
        if (status == OrderStatus.DELIVERED || status == OrderStatus.CANCELLED)
            throw new BusinessException("Cannot cancel order in terminal state: " + status + " (OA-S5)");
        this.cancelledReason = reason;
        transitionTo(OrderStatus.CANCELLED, changedBy, reason);
    }

    public void addItem(OrderItem item) {
        item.attachTo(this);
        this.items.add(item);
    }

    private void transitionTo(OrderStatus next, UUID changedBy, String note) {
        OrderStatus prev = this.status;
        this.status = next;
        // OA-08: append-only status log
        statusLogs.add(OrderStatusLog.create(this, prev, next, changedBy, note));
    }

    private void requireStatus(OrderStatus expected, String action) {
        if (status != expected)
            throw new BusinessException(
                    "Cannot " + action + " order in status " + status + " (expected " + expected + ")");
    }
}
