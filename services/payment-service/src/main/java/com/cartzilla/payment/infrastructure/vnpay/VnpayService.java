package com.cartzilla.payment.infrastructure.vnpay;

import lombok.RequiredArgsConstructor;
import com.cartzilla.web.exception.BusinessException;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * VNPay signing/verification theo chuẩn HMAC-SHA512 của VNPay (F13, UC-07, BR-PY06).
 * Dùng được với sandbox thật hoặc làm mock provider tự ký (cùng hashSecret).
 */
@Service
@RequiredArgsConstructor
public class VnpayService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final VnpayProperties props;

    /** Xây redirect URL VNPay đã ký. amount là số tiền VND (chưa ×100). */
    public String buildPaymentUrl(String txnRef, BigDecimal amount, String orderInfo, String ipAddr) {
        requireConfigured();
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", props.getVersion());
        params.put("vnp_Command", props.getCommand());
        params.put("vnp_TmnCode", props.getTmnCode());
        // VNPay yêu cầu amount ×100, không phần thập phân
        params.put("vnp_Amount", amount.multiply(BigDecimal.valueOf(100)).toBigInteger().toString());
        params.put("vnp_CurrCode", props.getCurrCode());
        params.put("vnp_TxnRef", txnRef);
        params.put("vnp_OrderInfo", orderInfo == null ? ("Thanh toan don " + txnRef) : orderInfo);
        params.put("vnp_OrderType", props.getOrderType());
        params.put("vnp_Locale", props.getLocale());
        params.put("vnp_ReturnUrl", props.getReturnUrl());
        params.put("vnp_IpAddr", ipAddr == null ? "127.0.0.1" : ipAddr);
        ZonedDateTime now = ZonedDateTime.now(VN_ZONE);
        params.put("vnp_CreateDate", now.format(FMT));
        params.put("vnp_ExpireDate", now.plusMinutes(15).format(FMT));

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String name = it.next();
            String value = params.get(name);
            if (value == null || value.isEmpty()) continue;
            String encoded = URLEncoder.encode(value, StandardCharsets.US_ASCII);
            hashData.append(name).append('=').append(encoded);
            query.append(URLEncoder.encode(name, StandardCharsets.US_ASCII)).append('=').append(encoded);
            if (it.hasNext()) {
                hashData.append('&');
                query.append('&');
            }
        }
        String secureHash = hmacSHA512(props.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);
        return props.getPayUrl() + "?" + query;
    }

    /** Verify chữ ký callback (BR-PY06): rebuild hash từ params (trừ vnp_SecureHash*). */
    public boolean verifySignature(Map<String, String> allParams) {
        if (props.getHashSecret() == null || props.getHashSecret().isBlank()) return false;
        Map<String, String> params = new HashMap<>(allParams);
        String received = params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");
        if (received == null || received.isBlank()) return false;

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String name = it.next();
            String value = params.get(name);
            if (value == null || value.isEmpty()) continue;
            hashData.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            if (it.hasNext()) hashData.append('&');
        }
        String expected = hmacSHA512(props.getHashSecret(), hashData.toString());
        return expected.equalsIgnoreCase(received);
    }

    /** Sinh chữ ký cho một bộ params (dùng cho mock provider / test tự ký). */
    public String sign(Map<String, String> params) {
        requireConfigured();
        Map<String, String> copy = new HashMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");
        List<String> fieldNames = new ArrayList<>(copy.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
            String name = it.next();
            String value = copy.get(name);
            if (value == null || value.isEmpty()) continue;
            hashData.append(name).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
            if (it.hasNext()) hashData.append('&');
        }
        return hmacSHA512(props.getHashSecret(), hashData.toString());
    }

    /** HMAC helper for VNPay Query/Refund's pipe-delimited checksum. */
    public String signPipe(String data) {
        requireConfigured();
        return hmacSHA512(props.getHashSecret(), data);
    }

    private void requireConfigured() {
        if (props.getTmnCode() == null || props.getTmnCode().isBlank()
                || props.getHashSecret() == null || props.getHashSecret().isBlank()) {
            throw new BusinessException("VNPay is not configured: set VNPAY_TMN_CODE and VNPAY_HASH_SECRET");
        }
    }

    private static String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * bytes.length);
            for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute HMAC-SHA512", e);
        }
    }
}
