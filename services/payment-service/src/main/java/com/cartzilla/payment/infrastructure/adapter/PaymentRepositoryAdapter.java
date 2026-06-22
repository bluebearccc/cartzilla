package com.cartzilla.payment.infrastructure.adapter;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.infrastructure.persistence.PaymentJpaRepository;
import com.cartzilla.payment.infrastructure.persistence.PaymentTransactionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentJpaRepository jpa;
    private final PaymentTransactionJpaRepository txnJpa;

    @Override public Payment save(Payment payment) { return jpa.save(payment); }

    @Override public Optional<Payment> findByOrderId(UUID orderId) { return jpa.findByOrderId(orderId); }

    @Override public Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef) {
        return jpa.findByVnpayTxnRef(vnpayTxnRef);
    }

    @Override public boolean existsTransactionByProviderTxnRef(String provider, String providerTxnRef) {
        if (providerTxnRef == null) return false;
        return txnJpa.existsByProviderAndProviderTxnRef(provider, providerTxnRef);
    }
}
