package com.cartzilla.payment.infrastructure.messaging;

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

    @Bean public TopicExchange paymentExchange() {
        return ExchangeBuilder.topicExchange(RabbitTopics.PAYMENT_EXCHANGE).durable(true).build();
    }
    @Bean public Queue paymentProcessQueue() { return QueueBuilder.durable(RabbitTopics.Q_PAYMENT_PROCESS).build(); }
    @Bean public Binding bindProcess() {
        return BindingBuilder.bind(paymentProcessQueue()).to(paymentExchange()).with(RabbitTopics.RK_PAYMENT_PROCESS);
    }

    @Bean public MessageConverter jsonConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonConverter());
        return t;
    }
}
