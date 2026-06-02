package com.cartzilla.payment.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.payment.application.usecase.ProcessPaymentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final ProcessPaymentUseCase processPaymentUseCase;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitTopics.Q_PAYMENT_PROCESS)
    public void onPaymentProcess(PaymentEvents.PaymentProcessEvent event) {
        PaymentEvents.PaymentResultEvent result = processPaymentUseCase.execute(event);
        rabbitTemplate.convertAndSend(RabbitTopics.PAYMENT_EXCHANGE, RabbitTopics.RK_PAYMENT_RESULT, result);
    }
}
