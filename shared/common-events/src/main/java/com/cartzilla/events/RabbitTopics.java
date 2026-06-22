package com.cartzilla.events;

/** Hằng số exchange / routing key / queue dùng chung cho toàn hệ thống. */
public final class RabbitTopics {
    private RabbitTopics() {}

    // Exchanges
    public static final String STOCK_EXCHANGE   = "stock.exchange";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String ORDER_EXCHANGE   = "order.exchange";

    // Routing keys
    public static final String RK_STOCK_RESERVE   = "stock.reserve";
    public static final String RK_STOCK_RESERVED  = "stock.reserved";
    public static final String RK_STOCK_RELEASE   = "stock.release";
    public static final String RK_PAYMENT_PROCESS = "payment.process";
    public static final String RK_PAYMENT_RESULT  = "payment.result";
    public static final String RK_ORDER_CONFIRMED = "order.confirmed";
    public static final String RK_ORDER_CANCELLED = "order.cancelled";
    public static final String RK_ORDER_SHIPPED   = "order.shipped";
    public static final String RK_ORDER_DELIVERED = "order.delivered";

    // Queues
    public static final String Q_STOCK_RESERVE   = "stock.reserve.queue";
    public static final String Q_STOCK_RESERVED  = "stock.reserved.queue";
    public static final String Q_STOCK_RELEASE   = "stock.release.queue";
    public static final String Q_PAYMENT_PROCESS = "payment.process.queue";
    public static final String Q_PAYMENT_RESULT  = "payment.result.queue";
    public static final String Q_ORDER_CONFIRMED = "order.confirmed.queue";
    public static final String Q_ORDER_CANCELLED = "order.cancelled.queue";
    public static final String Q_ORDER_SHIPPED   = "order.shipped.queue";
    // order.delivered fan-out: notification + payment có queue riêng cùng bind RK_ORDER_DELIVERED
    public static final String Q_ORDER_DELIVERED_NOTIFICATION = "order.delivered.notification.queue";
    public static final String Q_ORDER_DELIVERED_PAYMENT      = "order.delivered.payment.queue";
}
