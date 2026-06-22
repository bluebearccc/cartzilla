package com.cartzilla.notification.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.order.OrderEvents;
import com.cartzilla.notification.application.usecase.SendOrderEmailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SendOrderEmailUseCase sendOrderEmailUseCase;

    @RabbitListener(queues = RabbitTopics.Q_ORDER_CONFIRMED)
    public void onConfirmed(OrderEvents.OrderConfirmedEvent event) {
        sendOrderEmailUseCase.sendConfirmed(event.orderId(), event.recipientUserId());
    }

    @RabbitListener(queues = RabbitTopics.Q_ORDER_CANCELLED)
    public void onCancelled(OrderEvents.OrderCancelledEvent event) {
        sendOrderEmailUseCase.sendCancelled(event.orderId(), event.recipientUserId(), event.reason());
    }

    @RabbitListener(queues = RabbitTopics.Q_ORDER_SHIPPED)
    public void onShipped(OrderEvents.OrderShippedEvent event) {
        sendOrderEmailUseCase.sendShipped(event.orderId(), event.recipientUserId());
    }
}
