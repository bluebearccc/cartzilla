package com.cartzilla.order.application.port;

import com.cartzilla.order.domain.entity.Order;

/**
 * Outbound port — phát event vòng đời đơn hàng (order.confirmed/cancelled/shipped/delivered).
 * Adapter ở infrastructure publish qua RabbitMQ. Giữ application không phụ thuộc framework MQ.
 */
public interface OrderEventPort {

    void orderConfirmed(Order order);

    void orderCancelled(Order order, String reason);

    void orderShipped(Order order);

    void orderDelivered(Order order);
}
