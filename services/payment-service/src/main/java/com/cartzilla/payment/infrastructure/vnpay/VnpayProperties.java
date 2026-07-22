package com.cartzilla.payment.infrastructure.vnpay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Cấu hình VNPay (mock/sandbox). Bind từ prefix `vnpay` trong application.yml. */
@Component
@ConfigurationProperties(prefix = "vnpay")
@Getter
@Setter
public class VnpayProperties {
    private String tmnCode = "";
    private String hashSecret = "";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    /** VNPay redirect trình duyệt về đây (backend) để verify + settle, sau đó redirect tiếp về SPA. */
    private String returnUrl = "http://localhost:8080/api/payments/vnpay/callback";
    /** Trang kết quả của SPA — backend 302 redirect khách về đây sau khi settle. */
    private String frontendReturnUrl = "http://localhost:5173/payment/result";
    /** VNPay merchant-configured server-to-server IPN endpoint. */
    private String ipnUrl = "http://localhost:8080/api/payments/vnpay/ipn";
    private String refundUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    /** Keep disabled for local demo unless a real sandbox transaction is available. */
    private boolean refundEnabled = false;
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String locale = "vn";
    private String currCode = "VND";
}
