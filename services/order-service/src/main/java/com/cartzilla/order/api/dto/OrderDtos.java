package com.cartzilla.order.api.dto;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.OrderItem;
import com.cartzilla.order.domain.entity.OrderStatusLog;
import com.cartzilla.order.domain.entity.SagaState;
import com.cartzilla.order.domain.vo.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderDtos {
    private OrderDtos() {}

    public record CheckoutBySkuRequest(
            @NotNull UUID userId,
            @NotEmpty @Valid List<SkuLineRequest> lines,
            @NotBlank String shippingAddress,
            @NotNull PaymentMethod paymentMethod,
            String voucherCode) {
        public OrderCommand.CheckoutBySku toCommand() {
            return new OrderCommand.CheckoutBySku(
                    userId,
                    lines.stream()
                            .map(line -> new OrderCommand.SkuLine(line.sku(), line.quantity()))
                            .toList(),
                    shippingAddress,
                    paymentMethod,
                    voucherCode);
        }
    }

    public record SkuLineRequest(@NotBlank String sku, @Min(1) int quantity) {}

    /** F10 — staff cập nhật trạng thái đơn. reason bắt buộc khi status = CANCELLED (OA-07). */
    public record UpdateStatusRequest(@NotBlank String status, String reason) {}

    /** F09 — customer hủy đơn của mình. */
    public record CancelOrderRequest(String reason) {}

    public record CheckoutResponse(
            UUID orderId,
            String status,
            String paymentStatus,
            BigDecimal totalAmount,
            SagaResponse saga) {
        public static CheckoutResponse from(Order order, SagaState saga) {
            return new CheckoutResponse(
                    order.getId(),
                    order.getStatus().name(),
                    order.getPaymentStatus().name(),
                    order.getTotalAmount(),
                    SagaResponse.from(saga));
        }
    }

    public record OrderSummaryResponse(
            UUID id,
            UUID userId,
            String status,
            String paymentMethod,
            String paymentStatus,
            BigDecimal totalAmount,
            Instant createdAt) {
        public static OrderSummaryResponse from(Order order) {
            return new OrderSummaryResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getStatus().name(),
                    order.getPaymentMethod().name(),
                    order.getPaymentStatus().name(),
                    order.getTotalAmount(),
                    order.getCreatedAt());
        }
    }

    public record OrderDetailResponse(
            UUID id,
            UUID userId,
            String status,
            String paymentMethod,
            String paymentStatus,
            BigDecimal subtotal,
            BigDecimal discount,
            BigDecimal totalAmount,
            String voucherCode,
            String shippingAddress,
            String cancelledReason,
            Instant confirmedAt,
            Instant createdAt,
            List<OrderItemResponse> items,
            List<OrderStatusLogResponse> statusLogs,
            SagaResponse saga) {
        public static OrderDetailResponse from(Order order, SagaState saga) {
            return new OrderDetailResponse(
                    order.getId(),
                    order.getUserId(),
                    order.getStatus().name(),
                    order.getPaymentMethod().name(),
                    order.getPaymentStatus().name(),
                    order.getSubtotal(),
                    order.getDiscount(),
                    order.getTotalAmount(),
                    order.getVoucherCode(),
                    order.getShippingAddress(),
                    order.getCancelledReason(),
                    order.getConfirmedAt(),
                    order.getCreatedAt(),
                    order.getItems().stream().map(OrderItemResponse::from).toList(),
                    order.getStatusLogs().stream().map(OrderStatusLogResponse::from).toList(),
                    SagaResponse.from(saga));
        }
    }

    public record OrderItemResponse(
            UUID id,
            UUID productId,
            String sku,
            String name,
            String image,
            String size,
            String color,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subtotal) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getProductId(),
                    item.getSku(),
                    item.getName(),
                    item.getImage(),
                    item.getSize(),
                    item.getColor(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getSubtotal());
        }
    }

    public record OrderStatusLogResponse(
            UUID id,
            String fromStatus,
            String toStatus,
            UUID changedBy,
            String note,
            Instant createdAt) {
        public static OrderStatusLogResponse from(OrderStatusLog log) {
            return new OrderStatusLogResponse(
                    log.getId(),
                    log.getFromStatus() == null ? null : log.getFromStatus().name(),
                    log.getToStatus().name(),
                    log.getChangedBy(),
                    log.getNote(),
                    log.getCreatedAt());
        }
    }

    public record SagaResponse(
            String currentStep,
            String status,
            int retryCount,
            String errorMessage) {
        public static SagaResponse from(SagaState saga) {
            if (saga == null) {
                return null;
            }
            return new SagaResponse(
                    saga.getCurrentStep().name(),
                    saga.getStatus().name(),
                    saga.getRetryCount(),
                    saga.getErrorMessage());
        }
    }
}
