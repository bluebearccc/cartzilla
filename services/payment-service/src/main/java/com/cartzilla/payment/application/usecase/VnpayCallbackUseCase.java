package com.cartzilla.payment.application.usecase;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.payment.domain.entity.Payment;
import com.cartzilla.payment.domain.repository.PaymentRepository;
import com.cartzilla.payment.domain.vo.PaymentStatus;
import com.cartzilla.payment.infrastructure.vnpay.VnpayService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * F13 — UC-07 VNPay callback: verify chữ ký (BR-PY06), idempotent theo providerTxnRef (TC-21),
 * khớp amount (BR-PY02/TC-22), settle PAID/FAILED và publish payment.result cho Saga (TC-20).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VnpayCallbackUseCase {

    private static final String PROVIDER = "VNPAY";

    private final PaymentRepository paymentRepository;
    private final VnpayService vnpayService;
    private final RabbitTemplate rabbit;
    private final ObjectMapper objectMapper;

    @Transactional
    public Result execute(Map<String, String> params) {
        if (!vnpayService.verifySignature(params)) {
            log.warn("VNPay callback invalid signature: txnRef={}", params.get("vnp_TxnRef"));
            return new Result(false, "97", "Sai chữ ký VNPay", null, false);
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String providerTxnNo = params.get("vnp_TransactionNo");

        // Idempotency (BR-PY06/TC-21): cùng providerTxnRef đã xử lý → trả kết quả idempotent.
        if (providerTxnNo != null
                && paymentRepository.existsTransactionByProviderTxnRef(PROVIDER, providerTxnNo)) {
            Payment existing = paymentRepository.findByVnpayTxnRef(txnRef).orElse(null);
            boolean paid = existing != null && existing.getStatus() == PaymentStatus.PAID;
            return new Result(paid, responseCode, "Callback đã được xử lý trước đó", existing, true);
        }

        Payment payment = paymentRepository.findByVnpayTxnRef(txnRef)
                .orElseThrow(() -> new NoSuchElementException("Payment not found for vnp_TxnRef: " + txnRef));

        String responseJson = toJson(params);

        // BR-PY02 / TC-22: amount callback phải khớp payment.amount.
        BigDecimal callbackAmount;
        try {
            callbackAmount = new BigDecimal(params.get("vnp_Amount"))
                    .divide(BigDecimal.valueOf(100));
        } catch (Exception ex) {
            payment.settleVnpayFailed(providerTxnNo, "INVALID_AMOUNT", responseJson);
            paymentRepository.save(payment);
            publishResult(payment.getOrderId(), false, providerTxnNo);
            return new Result(false, "04", "Số tiền không hợp lệ", payment, false);
        }
        if (callbackAmount.compareTo(payment.getAmount()) != 0) {
            payment.settleVnpayFailed(providerTxnNo, "AMOUNT_MISMATCH", responseJson);
            paymentRepository.save(payment);
            publishResult(payment.getOrderId(), false, providerTxnNo);
            return new Result(false, "04", "Số tiền không khớp đơn hàng", payment, false);
        }

        boolean success = "00".equals(responseCode);
        if (success) {
            payment.settleVnpaySuccess(providerTxnNo, responseJson);
        } else {
            payment.settleVnpayFailed(providerTxnNo, "VNPAY_CODE_" + responseCode, responseJson);
        }
        paymentRepository.save(payment);
        publishResult(payment.getOrderId(), success, providerTxnNo);

        return new Result(success, responseCode,
                success ? "Thanh toán VNPay thành công" : "Thanh toán VNPay thất bại",
                payment, false);
    }

    private void publishResult(UUID orderId, boolean success, String txnId) {
        rabbit.convertAndSend(RabbitTopics.PAYMENT_EXCHANGE, RabbitTopics.RK_PAYMENT_RESULT,
                new PaymentEvents.PaymentResultEvent(orderId, success, txnId));
    }

    private String toJson(Map<String, String> params) {
        try {
            return objectMapper.writeValueAsString(params);
        } catch (Exception e) {
            return "{}";
        }
    }

    public record Result(boolean success, String code, String message, Payment payment, boolean idempotent) {}
}
