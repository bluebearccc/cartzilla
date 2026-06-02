package com.cartzilla.payment.domain.entity;

import com.cartzilla.web.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;
    @Column(name = "user_id")
    private UUID userId;
    private String method;             // COD | VNPAY
    private BigDecimal amount;
    private String status;             // PENDING | PAID | FAILED | REFUNDED
    @Column(name = "vnpay_txn_ref")
    private String vnpayTxnRef;
    @Column(name = "paid_at")
    private Instant paidAt;

    public static Payment create(UUID orderId, UUID userId, String method, BigDecimal amount) {
        Payment p = new Payment();
        p.orderId = orderId;
        p.userId = userId;
        p.method = method;
        p.amount = amount;
        p.status = "PENDING";
        return p;
    }

    public void markPaid(String txnRef) {
        this.status = "PAID";
        this.vnpayTxnRef = txnRef;
        this.paidAt = Instant.now();
    }

    public void markFailed() { this.status = "FAILED"; }
}
