package com.cartzilla.payment.infrastructure.adapter;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.infrastructure.persistence.PaymentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final PaymentJpaRepository jpa;
    @Override public Payment save(Payment payment) { return jpa.save(payment); }
    @Override public Optional<Payment> findByOrderId(UUID orderId) { return jpa.findByOrderId(orderId); }
}
