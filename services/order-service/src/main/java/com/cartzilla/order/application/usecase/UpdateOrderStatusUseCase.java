package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.web.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

/**
 * F10 — UC-04: staff/admin chuyển trạng thái đơn theo state machine.
 * Mọi chuyển trạng thái hợp lệ ghi OrderStatusLog (BR-O08) — enforce trong domain.
 * Transition không hợp lệ → BusinessException (422). OA-S1..OA-S5, BR-O09..BR-O11.
 */
@Service
@RequiredArgsConstructor
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;

    @Transactional
    public Order execute(OrderCommand.UpdateStatus cmd) {
        Order order = orderRepository.findById(cmd.orderId())
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + cmd.orderId()));

        OrderStatus target = parseStatus(cmd.targetStatus());
        switch (target) {
            case CONFIRMED -> order.confirm(cmd.changedBy());          // OA-S1
            case SHIPPING  -> order.ship(cmd.changedBy());             // OA-S2 (BR-O10)
            case DELIVERED -> order.deliver(cmd.changedBy());          // OA-S3 (BR-O10), COD → PAID
            case CANCELLED -> order.cancel(cmd.reason(), cmd.changedBy()); // OA-S4/OA-07
            case PENDING   -> throw new BusinessException("Cannot transition back to PENDING (OA-S5)");
        }
        return orderRepository.save(order);
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
