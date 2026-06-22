package com.cartzilla.payment.application.usecase;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentMethod;
import com.cartzilla.payment.domain.vo.PaymentStatus;
import com.cartzilla.payment.infrastructure.vnpay.VnpayService;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * F13 — UC-07 VNPay: tạo (hoặc tái sử dụng) Payment VNPAY PENDING và sinh redirect URL đã ký.
 * BR-PY01: một payment/order — tái sử dụng nếu đã tồn tại.
 */
@Service
@RequiredArgsConstructor
public class CreateVnpayPaymentUseCase {

    private final PaymentRepository paymentRepository;
    private final VnpayService vnpayService;

    @Transactional
    public Result execute(UUID orderId, UUID userId, BigDecimal amount, String orderInfo, String ipAddr) {
        if (orderId == null) throw new BusinessException("orderId is required");
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException("amount must be > 0");

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment == null) {
            payment = Payment.create(orderId, userId, PaymentMethod.VNPAY, amount);
            payment.attachVnpayRef(generateTxnRef());
            payment = paymentRepository.save(payment);
        } else {
            if (payment.getStatus() == PaymentStatus.PAID)
                throw new BusinessException("Đơn hàng đã được thanh toán");
            if (payment.getMethod() != PaymentMethod.VNPAY)
                throw new BusinessException("Đơn hàng này không dùng phương thức VNPAY");
            if (payment.getVnpayTxnRef() == null) {
                payment.attachVnpayRef(generateTxnRef());
                payment = paymentRepository.save(payment);
            }
        }

        String url = vnpayService.buildPaymentUrl(
                payment.getVnpayTxnRef(), payment.getAmount(), orderInfo, ipAddr);
        return new Result(url, payment.getVnpayTxnRef(), payment.getOrderId());
    }

    private String generateTxnRef() {
        return "VNP" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    public record Result(String paymentUrl, String txnRef, UUID orderId) {}
}
