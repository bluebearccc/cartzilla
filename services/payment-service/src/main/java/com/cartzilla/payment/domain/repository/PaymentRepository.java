package com.cartzilla.payment.domain.repository;

import com.cartzilla.payment.domain.entity.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findByOrderId(UUID orderId);

    /** VNPay callback tra payment theo vnp_TxnRef (ref của ta). */
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);

    /** F13/BR-PY06: idempotency — đã ghi transaction cho providerTxnRef này chưa. */
    boolean existsTransactionByProviderTxnRef(String provider, String providerTxnRef);
}
