package com.cartzilla.order.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.order.infrastructure.saga.OrderSagaOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SagaConsumer {

    private final OrderSagaOrchestrator orchestrator;

    @RabbitListener(queues = RabbitTopics.Q_STOCK_RESERVED)
    public void onStockReserved(StockEvents.StockReservedEvent event) {
        orchestrator.onStockReserved(event);
    }

    @RabbitListener(queues = RabbitTopics.Q_PAYMENT_RESULT)
    public void onPaymentResult(PaymentEvents.PaymentResultEvent event) {
        orchestrator.onPaymentResult(event);
    }
}
