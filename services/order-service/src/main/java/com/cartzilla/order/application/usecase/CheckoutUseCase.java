package com.cartzilla.order.application.usecase;

import com.cartzilla.order.application.command.OrderCommand;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.OrderItem;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.infrastructure.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CheckoutUseCase {

    private final OrderRepository orderRepository;
    private final OrderSagaOrchestrator sagaOrchestrator;

    @Transactional
    public UUID execute(OrderCommand.Checkout cmd) {
        List<OrderItem> items = cmd.lines().stream()
                .map(l -> OrderItem.create(
                        UUID.fromString(l.productId()), l.sku(), l.name(), l.image(),
                        l.size(), l.color(), l.unitPrice(), l.quantity()))
                .toList();

        // TODO: gọi user-service validate voucher (Resilience4j) → discount
        BigDecimal discount = BigDecimal.ZERO;

        Order order = Order.create(cmd.userId(), PaymentMethod.valueOf(cmd.paymentMethod()),
                cmd.shippingAddress(), items, discount, cmd.voucherCode(), null);

        Order saved = orderRepository.save(order);
        sagaOrchestrator.start(saved);   // async qua RabbitMQ
        return saved.getId();
    }
}
