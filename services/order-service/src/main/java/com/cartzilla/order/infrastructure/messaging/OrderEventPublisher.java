package com.cartzilla.order.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.order.OrderEvents;
import com.cartzilla.order.application.port.OrderEventPort;
import com.cartzilla.order.domain.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Adapter phát order lifecycle events; recipientUserId = order.userId (notification tra email). */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher implements OrderEventPort {

    private final RabbitTemplate rabbit;

    @Override
    public void orderConfirmed(Order order) {
        rabbit.convertAndSend(RabbitTopics.ORDER_EXCHANGE, RabbitTopics.RK_ORDER_CONFIRMED,
                new OrderEvents.OrderConfirmedEvent(order.getId(), order.getUserId()));
        log.info("Published order.confirmed order={}", order.getId());
    }

    @Override
    public void orderCancelled(Order order, String reason) {
        rabbit.convertAndSend(RabbitTopics.ORDER_EXCHANGE, RabbitTopics.RK_ORDER_CANCELLED,
                new OrderEvents.OrderCancelledEvent(order.getId(), order.getUserId(), reason));
        log.info("Published order.cancelled order={} reason={}", order.getId(), reason);
    }

    @Override
    public void orderShipped(Order order) {
        rabbit.convertAndSend(RabbitTopics.ORDER_EXCHANGE, RabbitTopics.RK_ORDER_SHIPPED,
                new OrderEvents.OrderShippedEvent(order.getId(), order.getUserId()));
        log.info("Published order.shipped order={}", order.getId());
    }

    @Override
    public void orderDelivered(Order order) {
        rabbit.convertAndSend(RabbitTopics.ORDER_EXCHANGE, RabbitTopics.RK_ORDER_DELIVERED,
                new OrderEvents.OrderDeliveredEvent(order.getId(), order.getUserId()));
        log.info("Published order.delivered order={}", order.getId());
    }
}
