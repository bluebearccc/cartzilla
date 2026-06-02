package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.OrderItem;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.infrastructure.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutUseCase {

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Transactional
    public UUID execute(OrderCommand.Checkout cmd) {
        BigDecimal subtotal = cmd.lines().stream()
                .map(l -> l.unitPrice().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // TODO: gọi user-service validate voucher (Resilience4j) → discount
        BigDecimal discount = BigDecimal.ZERO;

        Order order = Order.create(cmd.userId(), cmd.paymentMethod(),
                cmd.shippingAddress(), subtotal, discount, cmd.voucherCode());
        cmd.lines().forEach(l -> order.addItem(OrderItem.create(
                l.productId(), l.sku(), l.name(), l.image(),
                l.size(), l.color(), l.unitPrice(), l.quantity())));

        Order saved = orderRepository.save(order);
        sagaOrchestrator.start(saved);   // async qua RabbitMQ
        return saved.getId();
    }
}
