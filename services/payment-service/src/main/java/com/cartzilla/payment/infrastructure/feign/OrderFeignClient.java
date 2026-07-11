package com.cartzilla.payment.infrastructure.feign;

import com.cartzilla.web.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Feign client gọi order-service (qua Eureka service name "order-service").
 * Dùng khi tạo VNPay để lấy thông tin đơn server-authoritative: số tiền thật, chủ đơn,
 * và phương thức đã chọn (chặn trả VNPay cho đơn COD).
 */
@FeignClient(name = "order-service", url = "${clients.order-service.url:}")
public interface OrderFeignClient {

    @GetMapping("/api/internal/orders/{orderId}/payment-info")
    ApiResponse<OrderPaymentInfo> getPaymentInfo(@PathVariable UUID orderId);

    record OrderPaymentInfo(UUID orderId, UUID userId, String paymentMethod, BigDecimal amount, String status) {}
}
