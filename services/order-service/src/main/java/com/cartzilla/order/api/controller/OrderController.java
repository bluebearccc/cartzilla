package com.cartzilla.order.api.controller;

import com.cartzilla.order.api.dto.OrderDtos;
import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.application.usecase.CancelOrderUseCase;
import com.cartzilla.order.application.usecase.CheckoutUseCase;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.SagaStateRepository;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

/** F06 checkout · F09 my orders + cancel. Khách chỉ thao tác trên đơn của chính mình. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutUseCase checkoutUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;

    @PostMapping("/checkout")
    public ApiResponse<UUID> checkout(@RequestBody OrderCommand.Checkout cmd) {
        return ApiResponse.ok("Order created and Saga started", checkoutUseCase.execute(cmd));
    }

    @PostMapping("/checkout/by-sku")
    public ApiResponse<OrderDtos.CheckoutResponse> checkoutBySku(
            @Valid @RequestBody OrderDtos.CheckoutBySkuRequest request) {
        UUID orderId = checkoutUseCase.executeFromSkus(request.toCommand());
        Order order = getOrder(orderId);
        return ApiResponse.ok("Order created and Saga started", OrderDtos.CheckoutResponse.from(
                order, sagaStateRepository.findByOrderId(orderId).orElse(null)));
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderDtos.OrderSummaryResponse>> myOrders(@RequestHeader("X-User-Id") UUID userId) {
        return ApiResponse.ok(orderRepository.findByUserId(userId).stream()
                .map(OrderDtos.OrderSummaryResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<OrderDtos.OrderDetailResponse> detail(
            @PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        Order order = getOwnedOrder(id, userId);
        return ApiResponse.ok(OrderDtos.OrderDetailResponse.from(
                order, sagaStateRepository.findByOrderId(id).orElse(null)));
    }

    @GetMapping("/{id}/status")
    @Transactional(readOnly = true)
    public ApiResponse<OrderDtos.CheckoutResponse> status(
            @PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId) {
        Order order = getOwnedOrder(id, userId);
        return ApiResponse.ok(OrderDtos.CheckoutResponse.from(
                order, sagaStateRepository.findByOrderId(id).orElse(null)));
    }

    /** F09 / SRS §2.3: customer hủy đơn của mình khi còn PENDING. */
    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderDtos.OrderDetailResponse> cancel(
            @PathVariable UUID id, @RequestHeader("X-User-Id") UUID userId,
            @RequestBody(required = false) OrderDtos.CancelOrderRequest req) {
        Order order = cancelOrderUseCase.execute(new OrderCommand.CancelByCustomer(
                id, userId, req == null ? null : req.reason()));
        return ApiResponse.ok("Đã hủy đơn hàng", OrderDtos.OrderDetailResponse.from(
                order, sagaStateRepository.findByOrderId(id).orElse(null)));
    }

    private Order getOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
    }

    /** Ownership guard — không tiết lộ đơn của user khác (404 thay vì 403). */
    private Order getOwnedOrder(UUID id, UUID userId) {
        Order order = getOrder(id);
        if (!order.getUserId().equals(userId))
            throw new NoSuchElementException("Order not found: " + id);
        return order;
    }
}
