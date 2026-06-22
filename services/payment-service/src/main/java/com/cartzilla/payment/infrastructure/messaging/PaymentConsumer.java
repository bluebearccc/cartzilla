package com.cartzilla.payment.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.order.OrderEvents;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.payment.application.usecase.MarkCodPaidUseCase;
import com.cartzilla.payment.application.usecase.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final MarkCodPaidUseCase markCodPaidUseCase;
    private final RabbitTemplate rabbitTemplate;

    /** COD qua Saga: tạo payment PENDING, báo success cho order. */
    @RabbitListener(queues = RabbitTopics.Q_PAYMENT_PROCESS)
    public void onPaymentProcess(PaymentEvents.PaymentProcessEvent event) {
        PaymentEvents.PaymentResultEvent result = processPaymentUseCase.execute(event);
        rabbitTemplate.convertAndSend(RabbitTopics.PAYMENT_EXCHANGE, RabbitTopics.RK_PAYMENT_RESULT, result);
    }

    /** order DELIVERED → COD payment chuyển PAID (BR-PY07). */
    @RabbitListener(queues = RabbitTopics.Q_ORDER_DELIVERED_PAYMENT)
    public void onOrderDelivered(OrderEvents.OrderDeliveredEvent event) {
        markCodPaidUseCase.execute(event.orderId());
    }
}
