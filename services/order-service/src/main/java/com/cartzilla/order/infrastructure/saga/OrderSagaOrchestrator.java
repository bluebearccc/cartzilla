package com.cartzilla.order.infrastructure.saga;

import com.cartzilla.events.RabbitTopics;
import com.cartzilla.events.payment.PaymentEvents;
import com.cartzilla.events.stock.StockEvents;
import com.cartzilla.order.application.port.OrderEventPort;
import com.cartzilla.order.domain.entity.Order;
import com.cartzilla.order.domain.entity.SagaState;
import com.cartzilla.order.domain.repository.OrderRepository;
import com.cartzilla.order.domain.repository.SagaStateRepository;
import com.cartzilla.order.domain.vo.OrderStatus;
import com.cartzilla.order.domain.vo.PaymentMethod;
import com.cartzilla.order.domain.vo.SagaStatus;
import com.cartzilla.order.domain.vo.SagaStep;
import com.cartzilla.order.infrastructure.feign.UserFeignClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Saga orchestrator: RESERVE_STOCK → PROCESS_PAYMENT → DONE (+ compensate khi fail).
 * COD: auto-process payment qua MQ. VNPAY: chờ payment.result từ VNPay callback (UC-07).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaOrchestrator {

    private final RabbitTemplate rabbit;
    private final OrderRepository orderRepository;
    private final SagaStateRepository sagaRepository;
    private final UserFeignClient userFeignClient;
    private final OrderEventPort orderEventPort;

    /** Bước 1: khởi động saga sau khi tạo Order PENDING. */
    @Transactional
    public void start(Order order) {
        sagaRepository.save(SagaState.start(order.getId()));
        publishStockReserve(order);
        log.info("Saga started order={}", order.getId());
    }

    /** Bước 2: kết quả giữ kho. */
    @Transactional
    public void onStockReserved(StockEvents.StockReservedEvent event) {
        SagaState saga = sagaRepository.findByOrderId(event.orderId()).orElseThrow();
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        // Idempotency: duplicate event hoặc saga đã đóng → bỏ qua, không xử lý lại.
        if (saga.getStatus() != SagaStatus.IN_PROGRESS) {
            log.warn("Ignore stock.reserved for closed saga order={}", event.orderId());
            return;
        }
        // Đơn đã bị hủy trong lúc chờ reserve (customer cancel) → trả kho ngay nếu đã trừ.
        if (order.getStatus() != OrderStatus.PENDING) {
            if (event.success()) publishStockRelease(order);
            saga.fail("Order no longer PENDING when stock reserved: " + order.getStatus());
            sagaRepository.save(saga);
            log.warn("Saga order={} closed — order already {} at stock.reserved",
                    order.getId(), order.getStatus());
            return;
        }
        if (!event.success()) {
            fail(saga, order, "Hết hàng: " + event.failedSku());
            return;
        }
        saga.advanceStep();
        sagaRepository.save(saga);

        // VNPAY: dừng tại PROCESS_PAYMENT, chờ payment.result do VNPay callback publish (UC-07).
        if (order.getPaymentMethod() == PaymentMethod.VNPAY) {
            log.info("Saga order={} awaiting VNPay callback (PROCESS_PAYMENT)", order.getId());
            return;
        }
        // COD: xử lý thanh toán đồng bộ qua MQ.
        rabbit.convertAndSend(RabbitTopics.PAYMENT_EXCHANGE, RabbitTopics.RK_PAYMENT_PROCESS,
                new PaymentEvents.PaymentProcessEvent(order.getId(), order.getUserId(),
                        order.getTotalAmount(), order.getPaymentMethod().name()));
    }

    /** Bước 3: kết quả thanh toán. */
    @Transactional
    public void onPaymentResult(PaymentEvents.PaymentResultEvent event) {
        SagaState saga = sagaRepository.findByOrderId(event.orderId()).orElseThrow();
        Order order = orderRepository.findById(event.orderId()).orElseThrow();
        // Idempotency: duplicate payment.result hoặc saga đã đóng → bỏ qua.
        if (saga.getStatus() != SagaStatus.IN_PROGRESS) {
            log.warn("Ignore payment.result for closed saga order={}", event.orderId());
            return;
        }
        // Đơn đã bị hủy trước khi có kết quả thanh toán → trả kho, không confirm.
        if (order.getStatus() != OrderStatus.PENDING) {
            publishStockRelease(order);
            saga.fail("Order no longer PENDING when payment settled: " + order.getStatus());
            sagaRepository.save(saga);
            log.warn("Saga order={} closed — order already {} at payment.result",
                    order.getId(), order.getStatus());
            return;
        }
        if (!event.success()) {
            // compensate: trả kho
            publishStockRelease(order);
            fail(saga, order, "Thanh toán thất bại");
            return;
        }
        if (!redeemVoucherIfNeeded(order)) {
            publishStockRelease(order);
            fail(saga, order, "Cannot redeem voucher");
            return;
        }
        order.confirm(null); // changedBy null = system/saga (OSL-04)
        orderRepository.save(order);
        saga.complete();
        sagaRepository.save(saga);
        orderEventPort.orderConfirmed(order);
        log.info("Saga completed order={}", order.getId());
    }

    /**
     * Compensation khi customer hủy đơn PENDING (F09/SRS §2.3): nếu saga đã vượt qua
     * bước RESERVE_STOCK thì kho đã bị trừ → publish stock.release, rồi đóng saga.
     * Trường hợp saga còn ở RESERVE_STOCK, onStockReserved sẽ tự trả kho khi thấy đơn đã hủy.
     */
    @Transactional
    public void compensateCustomerCancel(Order order, String reason) {
        SagaState saga = sagaRepository.findByOrderId(order.getId()).orElse(null);
        if (saga == null || saga.getStatus() != SagaStatus.IN_PROGRESS) return;
        if (saga.getCurrentStep() != SagaStep.RESERVE_STOCK) {
            publishStockRelease(order);
        }
        saga.fail("Customer cancelled: " + reason);
        sagaRepository.save(saga);
        log.info("Saga order={} closed by customer cancel, stock compensated", order.getId());
    }

    private void fail(SagaState saga, Order order, String reason) {
        saga.fail(reason);
        sagaRepository.save(saga);
        // Đơn có thể đã CANCELLED bởi luồng khác — cancel() sẽ throw ở terminal state.
        if (order.getStatus() == OrderStatus.PENDING || order.getStatus() == OrderStatus.CONFIRMED) {
            order.cancel(reason, null); // changedBy null = system/saga (OSL-04)
            orderRepository.save(order);
            orderEventPort.orderCancelled(order, reason);
        }
        log.warn("Saga failed order={} reason={}", order.getId(), reason);
    }

    private void publishStockReserve(Order order) {
        rabbit.convertAndSend(RabbitTopics.STOCK_EXCHANGE, RabbitTopics.RK_STOCK_RESERVE,
                new StockEvents.StockReserveEvent(order.getId(), toItems(order)));
    }

    private void publishStockRelease(Order order) {
        rabbit.convertAndSend(RabbitTopics.STOCK_EXCHANGE, RabbitTopics.RK_STOCK_RELEASE,
                new StockEvents.StockReleaseEvent(order.getId(), toItems(order)));
    }

    private static List<StockEvents.Item> toItems(Order order) {
        return order.getItems().stream()
                .map(i -> new StockEvents.Item(i.getSku(), i.getQuantity())).toList();
    }

    private boolean redeemVoucherIfNeeded(Order order) {
        if (order.getVoucherCode() == null || order.getVoucherCode().isBlank()) {
            return true;
        }
        try {
            var response = userFeignClient.redeemVoucher(new UserFeignClient.VoucherRedeemRequest(
                    order.getVoucherCode(), order.getUserId(), order.getId(), order.getSubtotal()));
            return response != null && response.success() && response.data() != null;
        } catch (Exception ex) {
            log.warn("Voucher redeem failed order={} code={}", order.getId(), order.getVoucherCode(), ex);
            return false;
        }
    }
}
