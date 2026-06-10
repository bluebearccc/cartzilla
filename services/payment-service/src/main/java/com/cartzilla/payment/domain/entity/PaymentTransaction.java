package com.cartzilla.payment.domain.entity;

import com.cartzilla.payment.domain.vo.TransactionType;
import com.cartzilla.web.base.BaseEntity;
import com.cartzilla.web.exception.BusinessException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Child entity của PaymentAggregate — append-only audit log.
 * Rules: PT-01..PT-08, PYA-06, BR-PY05..BR-PY06.
 * UNIQUE (provider, providerTxnRef) WHERE providerTxnRef IS NOT NULL — PT-06.
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    /** ref orders.id — reference-only */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** PT-01 */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private TransactionType transactionType;

    /** PT-02: COD | VNPAY */
    @Column(nullable = false, length = 20)
    private String provider;

    /** PT-06: UNIQUE conditional per DB */
    @Column(name = "provider_txn_ref", length = 100)
    private String providerTxnRef;

    /** PT-03: ≥ 0 */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** PT-04: default VND */
    @Column(nullable = false, length = 3)
    private String currency = "VND";

    /** PT-05 */
    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** PT-07: processedAt ≥ createdAt */
    @Column(name = "processed_at")
    private Instant processedAt;

    static PaymentTransaction create(Payment payment, UUID orderId,
                                      TransactionType type, String provider,
                                      String providerTxnRef, BigDecimal amount,
                                      String rawStatus) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0)
            throw new BusinessException("Transaction amount must be >= 0 (PT-03)");
        if (provider == null || provider.isBlank())
            throw new BusinessException("provider must not be blank (PT-02)");
        PaymentTransaction tx = new PaymentTransaction();
        tx.payment = payment;
        tx.orderId = orderId;
        tx.transactionType = type;
        tx.provider = provider;
        tx.providerTxnRef = providerTxnRef;
        tx.amount = amount;
        tx.currency = "VND";
        tx.status = rawStatus != null ? rawStatus : "PENDING";
        tx.processedAt = Instant.now();
        return tx;
    }
}
