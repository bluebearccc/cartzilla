package com.cartzilla.order.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean public TopicExchange stockExchange()   { return ExchangeBuilder.topicExchange(RabbitTopics.STOCK_EXCHANGE).durable(true).build(); }
    @Bean public TopicExchange paymentExchange() { return ExchangeBuilder.topicExchange(RabbitTopics.PAYMENT_EXCHANGE).durable(true).build(); }
    @Bean public TopicExchange orderExchange()   { return ExchangeBuilder.topicExchange(RabbitTopics.ORDER_EXCHANGE).durable(true).build(); }

    @Bean public Queue stockReservedQueue() { return QueueBuilder.durable(RabbitTopics.Q_STOCK_RESERVED).build(); }
    @Bean public Queue paymentResultQueue() { return QueueBuilder.durable(RabbitTopics.Q_PAYMENT_RESULT).build(); }

    @Bean public Binding bindStockReserved() {
        return BindingBuilder.bind(stockReservedQueue()).to(stockExchange()).with(RabbitTopics.RK_STOCK_RESERVED);
    }
    @Bean public Binding bindPaymentResult() {
        return BindingBuilder.bind(paymentResultQueue()).to(paymentExchange()).with(RabbitTopics.RK_PAYMENT_RESULT);
    }

    @Bean public MessageConverter jsonConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonConverter());
        return t;
    }
}
