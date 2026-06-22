package com.cartzilla.payment.application.usecase;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

/** UC-07 A2 — refund: ghi PaymentTransaction REFUND, payment status REFUNDED (BR-PY04/BR-PY05). */
@Service
@RequiredArgsConstructor
public class RefundPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional
    public Payment execute(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for order: " + orderId));
        payment.markRefunded("REFUND-" + UUID.randomUUID());
        return paymentRepository.save(payment);
    }
}
