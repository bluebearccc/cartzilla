package com.cartzilla.order.api.controller;

import com.cartzilla.order.application.usecase.GetUserOrderStatsUseCase;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderController {

    private final GetUserOrderStatsUseCase getUserOrderStatsUseCase;
    private final OrderRepository orderRepository;

    @GetMapping("/users/{userId}/stats")
    public ApiResponse<UserOrderStatsResponse> getStats(
            @PathVariable UUID userId,
            @RequestParam(required = false) UUID excludeOrderId) {
        var stats = getUserOrderStatsUseCase.execute(userId, excludeOrderId);
        return ApiResponse.ok(new UserOrderStatsResponse(
                stats.nonCancelledOrdersCount(),
                stats.completedOrdersCount(),
                stats.totalSpent()
        ));
    }

    /**
     * Thông tin thanh toán của đơn (server-authoritative) — payment-service gọi khi tạo VNPay để:
     * - KHÔNG tin amount client gửi (lấy tổng đơn thật).
     * - Verify ownership (userId).
     * - Chặn trả VNPay cho đơn đã chọn COD (paymentMethod chốt tại lúc đặt hàng).
     */
    @GetMapping("/{orderId}/payment-info")
    public ApiResponse<OrderPaymentInfoResponse> getPaymentInfo(@PathVariable UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
        return ApiResponse.ok(new OrderPaymentInfoResponse(
                order.getId(), order.getUserId(),
                order.getPaymentMethod() == null ? null : order.getPaymentMethod().name(),
                order.getTotalAmount(),
                order.getStatus() == null ? null : order.getStatus().name()));
    }

    public record UserOrderStatsResponse(
            long nonCancelledOrdersCount,
            long completedOrdersCount,
            BigDecimal totalSpent
    ) {}

    public record OrderPaymentInfoResponse(
            UUID orderId,
            UUID userId,
            String paymentMethod,
            BigDecimal amount,
            String status
    ) {}
}
