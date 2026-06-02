package com.cartzilla.product.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.product.application.usecase.ReserveStockUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockConsumer {

    private final ReserveStockUseCase reserveStockUseCase;
    private final RabbitTemplate rabbitTemplate;

    @RabbitListener(queues = RabbitTopics.Q_STOCK_RESERVE)
    public void onReserve(StockEvents.StockReserveEvent event) {
        String failedSku = reserveStockUseCase.reserve(event);
        boolean success = failedSku == null;
        log.info("Stock reserve order={} success={}", event.orderId(), success);
        rabbitTemplate.convertAndSend(RabbitTopics.STOCK_EXCHANGE, RabbitTopics.RK_STOCK_RESERVED,
                new StockEvents.StockReservedEvent(event.orderId(), success, failedSku));
    }

    @RabbitListener(queues = RabbitTopics.Q_STOCK_RELEASE)
    public void onRelease(StockEvents.StockReleaseEvent event) {
        reserveStockUseCase.release(event);
        log.info("Stock released order={}", event.orderId());
    }
}
