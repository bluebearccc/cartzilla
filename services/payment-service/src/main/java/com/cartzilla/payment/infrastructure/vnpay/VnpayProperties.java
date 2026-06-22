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
    private String tmnCode = "CARTZILLA";
    private String hashSecret = "CARTZILLAVNPAYHASHSECRET1234567890";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String returnUrl = "http://localhost:8080/api/payments/vnpay/callback";
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String locale = "vn";
    private String currCode = "VND";
}
