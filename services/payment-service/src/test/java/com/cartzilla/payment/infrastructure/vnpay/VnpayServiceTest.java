package com.cartzilla.payment.infrastructure.vnpay;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VnpayServiceTest {

    private final VnpayProperties props = new VnpayProperties();
    private final VnpayService service = new VnpayService(props);

    @Test
    void buildPaymentUrl_containsPayUrlAmountAndSignature() {
        String url = service.buildPaymentUrl("VNP123", new BigDecimal("100000"),
                "Thanh toan don VNP123", "127.0.0.1");

        assertTrue(url.startsWith(props.getPayUrl() + "?"));
        assertTrue(url.contains("vnp_SecureHash="));
        // 100000 VND → vnp_Amount = 10000000
        assertTrue(url.contains("vnp_Amount=10000000"));
        assertTrue(url.contains("vnp_TxnRef=VNP123"));
    }

    @Test
    void signThenVerify_roundTripIsValid() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNP999");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_TransactionNo", "TXN-1");
        params.put("vnp_TmnCode", props.getTmnCode());

        params.put("vnp_SecureHash", service.sign(params));

        assertTrue(service.verifySignature(params));
    }

    @Test
    void verify_failsWhenTampered() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNP999");
        params.put("vnp_Amount", "10000000");
        params.put("vnp_ResponseCode", "00");
        params.put("vnp_SecureHash", service.sign(params));

        params.put("vnp_Amount", "20000000"); // tamper sau khi ký

        assertFalse(service.verifySignature(params));
    }

    @Test
    void verify_failsWhenSecureHashMissing() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "VNP999");
        assertFalse(service.verifySignature(params));
    }
}
