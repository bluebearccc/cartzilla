package com.cartzilla.payment.application.usecase;

import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Xử lý thanh toán. COD → mock thành công ngay.
 * Có thể bật env ALWAYS_FAIL=true để demo saga rollback.
 */
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Value("${payment.always-fail:false}")
    private boolean alwaysFail;

    @Transactional
    public PaymentEvents.PaymentResultEvent execute(PaymentEvents.PaymentProcessEvent event) {
        Payment payment = Payment.create(event.orderId(), event.userId(), event.method(), event.amount());

        boolean success = !alwaysFail;   // COD mock; VNPay xử lý qua callback riêng
        if (success) {
            String txn = "TXN-" + UUID.randomUUID();
            payment.markPaid(txn);
            paymentRepository.save(payment);
            return new PaymentEvents.PaymentResultEvent(event.orderId(), true, txn);
        } else {
            payment.markFailed();
            paymentRepository.save(payment);
            return new PaymentEvents.PaymentResultEvent(event.orderId(), false, null);
        }
    }
}
