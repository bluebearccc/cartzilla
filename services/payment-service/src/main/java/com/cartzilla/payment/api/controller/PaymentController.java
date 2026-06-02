package com.cartzilla.payment.api.controller;

import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** VNPay redirect/callback mock (F12). */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    @GetMapping("/vnpay/callback")
    public ApiResponse<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> params) {
        // TODO: verify chữ ký VNPay, publish payment.result
        return ApiResponse.ok("VNPay callback nhận", Map.of("params", params));
    }
}
