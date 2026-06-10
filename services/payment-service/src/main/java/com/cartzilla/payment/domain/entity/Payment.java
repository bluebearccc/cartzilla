package com.cartzilla.payment.domain.entity;

import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.payment.domain.vo.PaymentStatus;
import com.cartzilla.payment.domain.vo.TransactionType;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Aggregate root — PaymentAggregate.
 * Root + List<PaymentTransaction>.
 * Rules: PYA-01..PYA-06, PY-01..PY-03, BR-PY01..BR-PY07.
 */
@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** PYA-01: UNIQUE — một payment per order */
    @Column(name = "order_id", nullable = false, unique = true)
    private UUID orderId;

    /** PY-01 */
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** PY-02/BR-PY03 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentMethod method;

    /** PYA-02: phải khớp order.totalAmount */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** PY-03/BR-PY04: default PENDING */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /** PYA-05: chỉ populate khi method = VNPAY */
    @Column(name = "vnpay_txn_ref", length = 100)
    private String vnpayTxnRef;

    @Column(name = "vnpay_response", columnDefinition = "jsonb")
    private String vnpayResponse;

    /** PYA-04: set khi PAID, ≥ createdAt */
    @Column(name = "paid_at")
    private Instant paidAt;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<PaymentTransaction> transactions = new ArrayList<>();

    /** Factory — PYA-02 guard (amount > 0) */
    public static Payment create(UUID orderId, UUID userId, PaymentMethod method, BigDecimal amount) {
        if (orderId == null) throw new BusinessException("orderId must not be null (PY-01)");
        if (userId == null) throw new BusinessException("userId must not be null (PY-01)");
        if (method == null) throw new BusinessException("method must not be null (PY-02)");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("Payment amount must be > 0 (PYA-02)");
        Payment p = new Payment();
        p.orderId = orderId;
        p.userId = userId;
        p.method = method;
        p.amount = amount;
        p.status = PaymentStatus.PENDING;
        p.addTransaction(TransactionType.INIT, amount, null, null);
        return p;
    }

    /** PYA-04: PAID — PYA-06: append transaction */
    public void markPaid(String txnRef, String vnpayResponse) {
        if (status == PaymentStatus.PAID)
            throw new BusinessException("Payment is already PAID");
        this.status = PaymentStatus.PAID;
        this.vnpayTxnRef = txnRef;
        this.vnpayResponse = vnpayResponse;
        Instant now = Instant.now();
        if (getCreatedAt() != null && now.isBefore(getCreatedAt()))
            throw new BusinessException("paidAt cannot be before createdAt (PYA-04/BR-G06)");
        this.paidAt = now;
        addTransaction(TransactionType.PAY, amount, txnRef, "SUCCESS");
    }

    public void markFailed(String errorMessage) {
        this.status = PaymentStatus.FAILED;
        addTransaction(TransactionType.CALLBACK, amount, null, "FAILED: " + errorMessage);
    }

    public void markRefunded(String txnRef) {
        if (status != PaymentStatus.PAID)
            throw new BusinessException("Can only refund a PAID payment");
        this.status = PaymentStatus.REFUNDED;
        addTransaction(TransactionType.REFUND, amount, txnRef, "REFUNDED");
    }

    /** PYA-06: append-only */
    public void addTransaction(TransactionType type, BigDecimal txnAmount,
                                 String providerTxnRef, String rawStatus) {
        transactions.add(PaymentTransaction.create(this, orderId, type, method.name(),
                providerTxnRef, txnAmount, rawStatus));
    }
}
