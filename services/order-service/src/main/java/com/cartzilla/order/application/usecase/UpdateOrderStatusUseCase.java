package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.application.port.OrderEventPort;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.domain.vo.PaymentStatus;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * F10 — UC-04: staff/admin chuyển trạng thái đơn theo state machine.
 * Mọi chuyển trạng thái hợp lệ ghi OrderStatusLog (BR-O08) — enforce trong domain.
 * Mỗi transition phát event tương ứng để customer nhận notification/email (BR-N02, F10).
 * Transition không hợp lệ → BusinessException (422). OA-S1..OA-S5, BR-O09..BR-O11.
 */
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPort orderEventPort;

    @Transactional
    public Order execute(OrderCommand.UpdateStatus cmd) {
        Order order = orderRepository.findById(cmd.orderId())
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + cmd.orderId()));

        OrderStatus target = parseStatus(cmd.targetStatus());
        switch (target) {
            case CONFIRMED -> {                                        // OA-S1
                // BR-O09/UC-07: đơn VNPay chỉ CONFIRMED qua payment callback thành công;
                // không cho staff xác nhận thủ công khi khách chưa thanh toán.
                if (order.getPaymentMethod() == PaymentMethod.VNPAY
                        && order.getPaymentStatus() != PaymentStatus.PAID) {
                    throw new BusinessException(
                            "Đơn VNPay chưa thanh toán — đơn sẽ tự xác nhận khi thanh toán thành công");
                }
                order.confirm(cmd.changedBy());
            }
            case SHIPPING  -> order.ship(cmd.changedBy());             // OA-S2 (BR-O10)
            case DELIVERED -> order.deliver(cmd.changedBy());          // OA-S3 (BR-O10), COD → PAID
            case CANCELLED -> order.cancel(cmd.reason(), cmd.changedBy()); // OA-S4/OA-07
            case PENDING   -> throw new BusinessException("Cannot transition back to PENDING (OA-S5)");
        }
        Order saved = orderRepository.save(order);

        switch (target) {
            case CONFIRMED -> orderEventPort.orderConfirmed(saved);
            case SHIPPING  -> orderEventPort.orderShipped(saved);
            case DELIVERED -> orderEventPort.orderDelivered(saved);
            case CANCELLED -> orderEventPort.orderCancelled(saved, cmd.reason());
            case PENDING   -> { /* unreachable: rejected above */ }
        }
        return saved;
    }

    private OrderStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank())
            throw new BusinessException("targetStatus is required");
        try {
            return OrderStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid targetStatus: " + raw
                    + " — expected CONFIRMED|SHIPPING|DELIVERED|CANCELLED");
        }
    }
}
