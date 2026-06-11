package com.cartzilla.order.api.controller;

import com.cartzilla.order.api.dto.OrderDtos;
import com.cartzilla.order.api.dto.PageResponse;
import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.application.usecase.ListOrdersUseCase;
import com.cartzilla.order.application.usecase.UpdateOrderStatusUseCase;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.OrderSearchCriteria;
import com.cartzilla.order.domain.repository.SagaStateRepository;
import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.web.exception.BusinessException;
import com.cartzilla.web.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * F10 — UC-04: Staff/Admin xử lý đơn hàng. Role STAFF/ADMIN enforce bởi
 * gateway JwtAuth + StaffRoleInterceptor (defense-in-depth, xem WebConfig).
 */
@RestController
@RequestMapping("/api/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final ListOrdersUseCase listOrdersUseCase;
    private final UpdateOrderStatusUseCase updateOrderStatusUseCase;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaStateRepository;

    /** GET /api/staff/orders?status=&paymentMethod=&fromDate=&toDate=&page=&limit= */
    @GetMapping
    public ApiResponse<PageResponse<OrderDtos.OrderSummaryResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        OrderSearchCriteria criteria = new OrderSearchCriteria(
                parseStatus(status), parsePaymentMethod(paymentMethod),
                fromDate == null ? null : fromDate.atStartOfDay().toInstant(ZoneOffset.UTC),
                toDate == null ? null : toDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC));
        return ApiResponse.ok(PageResponse.from(
                listOrdersUseCase.execute(criteria, page, limit)
                        .map(OrderDtos.OrderSummaryResponse::from)));
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ApiResponse<OrderDtos.OrderDetailResponse> detail(@PathVariable UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + id));
        return ApiResponse.ok(OrderDtos.OrderDetailResponse.from(
                order, sagaStateRepository.findByOrderId(id).orElse(null)));
    }

    @PutMapping("/{id}/status")
    public ApiResponse<OrderDtos.OrderDetailResponse> updateStatus(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID staffId,
            @Valid @RequestBody OrderDtos.UpdateStatusRequest req) {
        Order order = updateOrderStatusUseCase.execute(
                new OrderCommand.UpdateStatus(id, req.status(), req.reason(), staffId));
        return ApiResponse.ok("Đã cập nhật trạng thái đơn",
                OrderDtos.OrderDetailResponse.from(order,
                        sagaStateRepository.findByOrderId(id).orElse(null)));
    }

    private OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid status filter: " + raw);
        }
    }

    private PaymentMethod parsePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid paymentMethod filter: " + raw);
        }
    }
}
