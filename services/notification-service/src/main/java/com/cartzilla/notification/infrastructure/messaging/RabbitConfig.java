package com.cartzilla.notification.infrastructure.messaging;

import com.cartzilla.events.RabbitTopics;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(RabbitTopics.ORDER_EXCHANGE).durable(true).build();
    }
    @Bean public Queue orderConfirmedQueue() { return QueueBuilder.durable(RabbitTopics.Q_ORDER_CONFIRMED).build(); }
    @Bean public Queue orderCancelledQueue() { return QueueBuilder.durable(RabbitTopics.Q_ORDER_CANCELLED).build(); }

    @Bean public Binding bindConfirmed() {
        return BindingBuilder.bind(orderConfirmedQueue()).to(orderExchange()).with(RabbitTopics.RK_ORDER_CONFIRMED);
    }
    @Bean public Binding bindCancelled() {
        return BindingBuilder.bind(orderCancelledQueue()).to(orderExchange()).with(RabbitTopics.RK_ORDER_CANCELLED);
    }

    @Bean public MessageConverter jsonConverter() { return new Jackson2JsonMessageConverter(); }
}
