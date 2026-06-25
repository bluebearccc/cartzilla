package com.cartzilla.payment.api.controller;

import com.cartzilla.payment.api.dto.PaymentDtos;
import com.cartzilla.payment.application.usecase.CreateVnpayPaymentUseCase;
import com.cartzilla.payment.application.usecase.RefundPaymentUseCase;
import com.cartzilla.payment.application.usecase.VnpayCallbackUseCase;
import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.infrastructure.vnpay.VnpayProperties;
import com.cartzilla.web.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
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
    private final VnpayProperties vnpayProperties;

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

    /**
     * GET /api/payments/vnpay/callback — VNPay redirect trình duyệt về (không có JWT).
     * Verify + settle payment ngay, rồi 302 redirect khách về trang kết quả của SPA
     * (kèm orderId + vnp_ResponseCode) để hiển thị, tránh dí khách vào trang JSON.
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<Void> vnpayCallback(@RequestParam Map<String, String> params) {
        VnpayCallbackUseCase.Result r = vnpayCallbackUseCase.execute(params);
        Payment p = r.payment();

        UriComponentsBuilder redirect = UriComponentsBuilder
                .fromUriString(vnpayProperties.getFrontendReturnUrl())
                .queryParam("status", r.success() ? "success" : "failed")
                .queryParam("vnp_ResponseCode", r.code() == null ? "99" : r.code());
        if (p != null) {
            redirect.queryParam("orderId", p.getOrderId());
        }

        URI location = redirect.encode().build().toUri();
        return ResponseEntity.status(HttpStatus.FOUND).location(location).build();
    }

    /**
     * GET /api/payments/{orderId} — trạng thái payment + audit transactions.
     * Chỉ chủ đơn (X-User-Id khớp payment.userId) hoặc STAFF/ADMIN mới xem được,
     * tránh lộ số tiền/mã giao dịch của đơn người khác qua orderId.
     */
    @GetMapping("/{orderId}")
    @Transactional(readOnly = true)
    public ApiResponse<PaymentDtos.PaymentResponse> status(
            @PathVariable UUID orderId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for order: " + orderId));
        if (!isStaff(role) && !payment.getUserId().equals(userId)) {
            throw new SecurityException("Bạn không có quyền xem thanh toán của đơn này");
        }
        return ApiResponse.ok(PaymentDtos.PaymentResponse.from(payment));
    }

    /** POST /api/payments/{orderId}/refund — refund (UC-07 A2). Chỉ STAFF/ADMIN. */
    @PostMapping("/{orderId}/refund")
    public ApiResponse<PaymentDtos.PaymentResponse> refund(
            @PathVariable UUID orderId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!isStaff(role)) {
            throw new SecurityException("Chỉ STAFF/ADMIN được phép hoàn tiền");
        }
        return ApiResponse.ok("Refunded", PaymentDtos.PaymentResponse.from(
                refundPaymentUseCase.execute(orderId)));
    }

    /** STAFF/ADMIN bỏ qua kiểm tra ownership (defense-in-depth, role do gateway inject). */
    private boolean isStaff(String role) {
        return "STAFF".equals(role) || "ADMIN".equals(role);
    }

    private String clientIp(HttpServletRequest http) {
        String xff = http.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return http.getRemoteAddr();
    }
}
