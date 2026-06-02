package com.cartzilla.order.api.controller;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.application.usecase.CheckoutUseCase;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.web.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CheckoutUseCase checkoutUseCase;
    private final OrderRepository orderRepository;

    @PostMapping("/checkout")
    public ApiResponse<UUID> checkout(@RequestBody OrderCommand.Checkout cmd) {
        return ApiResponse.ok("Đơn hàng đã tạo, đang xử lý", checkoutUseCase.execute(cmd));
    }

    @GetMapping
    public List<Order> myOrders(@RequestHeader("X-User-Id") UUID userId) {
        return orderRepository.findByUserId(userId);
    }

    @GetMapping("/{id}")
    public Order detail(@PathVariable UUID id) {
        return orderRepository.findById(id).orElseThrow();
    }
}
