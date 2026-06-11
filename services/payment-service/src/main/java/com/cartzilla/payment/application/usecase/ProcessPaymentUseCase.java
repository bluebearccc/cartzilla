package com.cartzilla.payment.application.usecase;

import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Value("${payment.always-fail:false}")
    private boolean alwaysFail;

    @Transactional
    public PaymentEvents.PaymentResultEvent execute(PaymentEvents.PaymentProcessEvent event) {
        Payment payment = Payment.create(
                event.orderId(),
                event.userId(),
                parsePaymentMethod(event.method()),
                event.amount());

        if (!alwaysFail) {
            String txn = "TXN-" + UUID.randomUUID();
            payment.markPaid(txn, null);
            paymentRepository.save(payment);
            return new PaymentEvents.PaymentResultEvent(event.orderId(), true, txn);
        }

        payment.markFailed("Payment forced to fail for demo");
        paymentRepository.save(payment);
        return new PaymentEvents.PaymentResultEvent(event.orderId(), false, null);
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw == null ? "" : raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unsupported payment method: " + raw);
        }
    }
}
