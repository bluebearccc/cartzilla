package com.cartzilla.order.infrastructure.saga;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.order.OrderEvents;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.SagaState;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.SagaStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saga orchestrator: RESERVE_STOCK → PROCESS_PAYMENT → DONE (+ compensate khi fail).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final RabbitTemplate rabbit;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaRepository;

    /** Bước 1: khởi động saga sau khi tạo Order PENDING. */
    @Transactional
    public void start(Order order) {
        sagaRepository.save(SagaState.start(order.getId()));
        List<StockEvents.Item> items = order.getItems().stream()
                .map(i -> new StockEvents.Item(i.getSku(), i.getQuantity())).toList();
        rabbit.convertAndSend(RabbitTopics.STOCK_EXCHANGE, RabbitTopics.RK_STOCK_RESERVE,
                new StockEvents.StockReserveEvent(order.getId(), items));
        log.info("Saga started order={}", order.getId());
    }

    /** Bước 2: kết quả giữ kho. */
    @Transactional
    public void onStockReserved(StockEvents.StockReservedEvent event) {
        SagaState saga = sagaRepository.findByOrderId(event.orderId()).orElseThrow();
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        if (!event.success()) {
            fail(saga, order, "Hết hàng: " + event.failedSku());
            return;
        }
        saga.moveTo(SagaState.Step.PROCESS_PAYMENT);
        sagaRepository.save(saga);
        rabbit.convertAndSend(RabbitTopics.PAYMENT_EXCHANGE, RabbitTopics.RK_PAYMENT_PROCESS,
                new PaymentEvents.PaymentProcessEvent(order.getId(), order.getUserId(),
                        order.getTotalAmount(), order.getPaymentMethod()));
    }

    /** Bước 3: kết quả thanh toán. */
    @Transactional
    public void onPaymentResult(PaymentEvents.PaymentResultEvent event) {
        SagaState saga = sagaRepository.findByOrderId(event.orderId()).orElseThrow();
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        if (!event.success()) {
            // compensate: trả kho
            List<StockEvents.Item> items = order.getItems().stream()
                    .map(i -> new StockEvents.Item(i.getSku(), i.getQuantity())).toList();
            rabbit.convertAndSend(RabbitTopics.STOCK_EXCHANGE, RabbitTopics.RK_STOCK_RELEASE,
                    new StockEvents.StockReleaseEvent(order.getId(), items));
            fail(saga, order, "Thanh toán thất bại");
            return;
        }
        order.confirm();
        orderRepository.save(order);
        saga.complete();
        sagaRepository.save(saga);
        rabbit.convertAndSend(RabbitTopics.ORDER_EXCHANGE, RabbitTopics.RK_ORDER_CONFIRMED,
                new OrderEvents.OrderConfirmedEvent(order.getId(), null));
        log.info("Saga completed order={}", order.getId());
    }

    private void fail(SagaState saga, Order order, String reason) {
        saga.fail(reason);
        sagaRepository.save(saga);
        order.cancel(reason);
        orderRepository.save(order);
        rabbit.convertAndSend(RabbitTopics.ORDER_EXCHANGE, RabbitTopics.RK_ORDER_CANCELLED,
                new OrderEvents.OrderCancelledEvent(order.getId(), reason, null));
        log.warn("Saga failed order={} reason={}", order.getId(), reason);
    }
}
