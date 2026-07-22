package com.cartzilla.payment.infrastructure.vnpay;

import com.cartzilla.payment.domain.entity.Payment;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** VNPay merchant_webapi refund adapter (full refund, transaction type 02). */
@Component
@RequiredArgsConstructor
public class VnpayRefundClient {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties properties;
    private final VnpayService vnpayService;
    private final ObjectMapper objectMapper;

    public Result refund(Payment payment, String reason, String clientIp) {
        String requestId = "RF" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8);
        if (!properties.isRefundEnabled()) {
            return new Result(true, "00", "local-refund-disabled", "{\"mode\":\"local\"}", requestId);
        }
        String providerTxnRef = payment.latestProviderTxnRef();
        if (providerTxnRef == null || providerTxnRef.isBlank()) {
            throw new IllegalStateException("VNPay provider transaction number is missing");
        }
        String createDate = ZonedDateTime.now(VN_ZONE).format(DATE);
        String transactionDate = extractTransactionDate(payment);
        String orderInfo = reason == null || reason.isBlank() ? "Hoan tien don " + payment.getOrderId() : reason;
        String amount = payment.getAmount().multiply(BigDecimal.valueOf(100)).toBigInteger().toString();
        String ip = clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp;
        String checksum = String.join("|", requestId, properties.getVersion(), "refund",
                properties.getTmnCode(), "02", payment.getVnpayTxnRef(), amount, providerTxnRef,
                transactionDate, "SYSTEM", createDate, ip, orderInfo);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", properties.getVersion());
        body.put("vnp_Command", "refund");
        body.put("vnp_TmnCode", properties.getTmnCode());
        body.put("vnp_TransactionType", "02");
        body.put("vnp_TxnRef", payment.getVnpayTxnRef());
        body.put("vnp_Amount", amount);
        body.put("vnp_TransactionNo", providerTxnRef);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateBy", "SYSTEM");
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ip);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_SecureHash", vnpayService.signPipe(checksum));
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getRefundUrl()))
                    .timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                    HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("VNPay refund HTTP " + response.statusCode());
            }
            Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
            String code = String.valueOf(result.getOrDefault("vnp_ResponseCode", "99"));
            boolean accepted = "00".equals(code) || "94".equals(code); // 94 = duplicate/in progress
            return new Result(accepted, code, String.valueOf(result.getOrDefault("vnp_Message", "")), response.body(), requestId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("VNPay refund interrupted", e);
        } catch (Exception e) {
            throw new IllegalStateException("VNPay refund request failed", e);
        }
    }

    private String extractTransactionDate(Payment payment) {
        try {
            Map<String, String> params = objectMapper.readValue(payment.getVnpayResponse(), new TypeReference<>() {});
            String value = params.get("vnp_PayDate");
            if (value == null || value.isBlank()) value = params.get("vnp_TransactionDate");
            return value == null || value.isBlank() ? ZonedDateTime.now(VN_ZONE).format(DATE) : value;
        } catch (Exception ignored) {
            return ZonedDateTime.now(VN_ZONE).format(DATE);
        }
    }

    public record Result(boolean accepted, String code, String message, String responseJson, String requestId) {}
}
