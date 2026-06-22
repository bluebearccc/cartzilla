package com.cartzilla.payment.api.dto;

import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.entity.PaymentTransaction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** DTO cho F13 VNPay + payment status (UC-07). */
public final class PaymentDtos {
    private PaymentDtos() {}

    public record CreateVnpayRequest(
            @NotNull UUID orderId,
            @NotNull @Positive BigDecimal amount,
            String orderInfo) {}

    public record CreateVnpayResponse(String paymentUrl, String txnRef, UUID orderId) {}

    public record CallbackResult(boolean success, String code, String message,
                                 UUID orderId, String status, boolean idempotent) {}

    public record TransactionResponse(String transactionType, String provider,
                                      String providerTxnRef, BigDecimal amount,
                                      String status, Instant processedAt) {
        public static TransactionResponse from(PaymentTransaction t) {
            return new TransactionResponse(
                    t.getTransactionType() == null ? null : t.getTransactionType().name(),
                    t.getProvider(), t.getProviderTxnRef(), t.getAmount(),
                    t.getStatus(), t.getProcessedAt());
        }
    }

    public record PaymentResponse(UUID id, UUID orderId, UUID userId, String method,
                                  BigDecimal amount, String status, String vnpayTxnRef,
                                  Instant paidAt, Instant createdAt,
                                  List<TransactionResponse> transactions) {
        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(
                    p.getId(), p.getOrderId(), p.getUserId(),
                    p.getMethod() == null ? null : p.getMethod().name(),
                    p.getAmount(),
                    p.getStatus() == null ? null : p.getStatus().name(),
                    p.getVnpayTxnRef(), p.getPaidAt(), p.getCreatedAt(),
                    p.getTransactions().stream().map(TransactionResponse::from).toList());
        }
    }
}
