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
 * F09 — Customer hủy đơn của chính mình khi còn PENDING (SRS §2.3).
 * OA-07: cancelledReason bắt buộc (default nếu khách không nhập).
 */
@Service
@RequiredArgsConstructor
public class CancelOrderUseCase {

    private final OrderRepository orderRepository;

    @Transactional
    public Order execute(OrderCommand.CancelByCustomer cmd) {
        Order order = orderRepository.findById(cmd.orderId())
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + cmd.orderId()));

        // Ownership: không tiết lộ sự tồn tại của đơn người khác
        if (!order.getUserId().equals(cmd.userId()))
            throw new NoSuchElementException("Order not found: " + cmd.orderId());

        if (order.getStatus() != OrderStatus.PENDING)
            throw new BusinessException("Chỉ được hủy đơn ở trạng thái PENDING (hiện tại: "
                    + order.getStatus() + ")");

        String reason = (cmd.reason() == null || cmd.reason().isBlank())
                ? "Khách hàng hủy đơn" : cmd.reason().trim();
        order.cancel(reason, cmd.userId());
        return orderRepository.save(order);
    }
}
