package com.cartzilla.payment.application.usecase;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.payment.domain.vo.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** F08 — BR-PY07/TC-26: order DELIVERED → COD payment chuyển PAID. Idempotent. */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarkCodPaidUseCase {

    private final PaymentRepository paymentRepository;

    @Transactional
    public void execute(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            log.warn("order.delivered nhưng không tìm thấy payment cho order={}", orderId);
            return;
        }
        if (payment.getMethod() == PaymentMethod.COD && payment.getStatus() == PaymentStatus.PENDING) {
            payment.markCodPaidOnDelivery();
            paymentRepository.save(payment);
            log.info("COD payment PAID khi delivered order={}", orderId);
        }
    }
}
