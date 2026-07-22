package com.cartzilla.payment.application.usecase;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.payment.infrastructure.vnpay.VnpayRefundClient;
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
    private final VnpayRefundClient vnpayRefundClient;

    @Transactional
    public Payment execute(UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for order: " + orderId));
        if (payment.getStatus() == com.cartzilla.payment.domain.vo.PaymentStatus.REFUNDED) return payment;
        String refundRef = "REFUND-" + UUID.randomUUID();
        if (payment.getMethod() == PaymentMethod.VNPAY) {
            VnpayRefundClient.Result result = vnpayRefundClient.refund(payment, "Hoan tien don " + orderId, "127.0.0.1");
            if (!result.accepted()) {
                throw new IllegalStateException("VNPay refund rejected: " + result.code());
            }
            refundRef = result.requestId();
        }
        payment.markRefunded(refundRef);
        return paymentRepository.save(payment);
    }
}
