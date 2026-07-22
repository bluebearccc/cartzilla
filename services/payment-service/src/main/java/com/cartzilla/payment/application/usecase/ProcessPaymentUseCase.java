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

/**
 * F08 — UC-07 COD qua Saga: tạo Payment COD trạng thái PENDING (BR-PY01) và báo success
 * để Saga đóng thành công (COD accepted). Đơn KHÔNG tự chuyển CONFIRMED — giữ PENDING chờ
 * staff xác nhận (UC-04). Payment chỉ chuyển PAID khi order DELIVERED (BR-PY07,
 * xử lý ở MarkCodPaidUseCase). Idempotent theo orderId khi Saga retry.
 */
@Service
@RequiredArgsConstructor
public class ProcessPaymentUseCase {

    private final PaymentRepository paymentRepository;

    @Value("${payment.always-fail:false}")
    private boolean alwaysFail;

    @Transactional
    public PaymentEvents.PaymentResultEvent execute(PaymentEvents.PaymentProcessEvent event) {
        Payment payment = paymentRepository.findByOrderId(event.orderId())
                .orElseGet(() -> Payment.create(
                        event.orderId(), event.userId(),
                        parsePaymentMethod(event.method()), event.amount()));

        if (alwaysFail) {
            payment.markFailed("Payment forced to fail for demo");
            paymentRepository.save(payment);
            return new PaymentEvents.PaymentResultEvent(event.orderId(), false, null);
        }

        // COD: payment giữ PENDING; Saga đóng nhưng đơn vẫn PENDING chờ staff xác nhận.
        paymentRepository.save(payment);
        return new PaymentEvents.PaymentResultEvent(event.orderId(), true, "COD-" + payment.getOrderId());
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        try {
            return PaymentMethod.valueOf(raw == null ? "" : raw.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Unsupported payment method: " + raw);
        }
    }
}
