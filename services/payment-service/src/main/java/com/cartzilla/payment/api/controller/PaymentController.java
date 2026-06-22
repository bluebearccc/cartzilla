package com.cartzilla.payment.api.controller;

import com.cartzilla.payment.api.dto.PaymentDtos;
import com.cartzilla.payment.application.usecase.CreateVnpayPaymentUseCase;
import com.cartzilla.payment.application.usecase.RefundPaymentUseCase;
import com.cartzilla.payment.application.usecase.VnpayCallbackUseCase;
import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * F13 — UC-07 VNPay: tạo redirect, nhận callback (verify + idempotent + amount-match),
 * tra cứu trạng thái payment và refund. COD do Saga/MQ xử lý (PaymentConsumer).
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final CreateVnpayPaymentUseCase createVnpayPaymentUseCase;
    private final VnpayCallbackUseCase vnpayCallbackUseCase;
    private final RefundPaymentUseCase refundPaymentUseCase;
    private final PaymentRepository paymentRepository;

    /** POST /api/payments/vnpay/create — tạo URL thanh toán VNPay (cần JWT, X-User-Id từ gateway). */
    @PostMapping("/vnpay/create")
    public ApiResponse<PaymentDtos.CreateVnpayResponse> createVnpay(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @Valid @RequestBody PaymentDtos.CreateVnpayRequest req,
            HttpServletRequest http) {
        CreateVnpayPaymentUseCase.Result r = createVnpayPaymentUseCase.execute(
                req.orderId(), userId, req.amount(), req.orderInfo(), clientIp(http));
        return ApiResponse.ok("VNPay payment URL created",
                new PaymentDtos.CreateVnpayResponse(r.paymentUrl(), r.txnRef(), r.orderId()));
    }

    /** GET /api/payments/vnpay/callback — VNPay redirect về (không có JWT). */
    @GetMapping("/vnpay/callback")
    public ApiResponse<PaymentDtos.CallbackResult> vnpayCallback(@RequestParam Map<String, String> params) {
        VnpayCallbackUseCase.Result r = vnpayCallbackUseCase.execute(params);
        Payment p = r.payment();
        PaymentDtos.CallbackResult body = new PaymentDtos.CallbackResult(
                r.success(), r.code(), r.message(),
                p == null ? null : p.getOrderId(),
                p == null || p.getStatus() == null ? null : p.getStatus().name(),
                r.idempotent());
        return ApiResponse.ok(r.message(), body);
    }

    /** GET /api/payments/{orderId} — trạng thái payment + audit transactions. */
    @GetMapping("/{orderId}")
    @Transactional(readOnly = true)
    public ApiResponse<PaymentDtos.PaymentResponse> status(@PathVariable UUID orderId) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for order: " + orderId));
        return ApiResponse.ok(PaymentDtos.PaymentResponse.from(payment));
    }

    /** POST /api/payments/{orderId}/refund — refund (UC-07 A2). */
    @PostMapping("/{orderId}/refund")
    public ApiResponse<PaymentDtos.PaymentResponse> refund(@PathVariable UUID orderId) {
        return ApiResponse.ok("Refunded", PaymentDtos.PaymentResponse.from(
                refundPaymentUseCase.execute(orderId)));
    }

    private String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
