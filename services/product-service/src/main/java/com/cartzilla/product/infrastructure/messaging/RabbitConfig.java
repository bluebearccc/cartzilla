package com.cartzilla.product.infrastructure.messaging;

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

    @Bean public TopicExchange stockExchange() {
        return ExchangeBuilder.topicExchange(RabbitTopics.STOCK_EXCHANGE).durable(true).build();
    }
    @Bean public Queue stockReserveQueue() { return QueueBuilder.durable(RabbitTopics.Q_STOCK_RESERVE).build(); }
    @Bean public Queue stockReleaseQueue() { return QueueBuilder.durable(RabbitTopics.Q_STOCK_RELEASE).build(); }

    @Bean public Binding bindReserve() {
        return BindingBuilder.bind(stockReserveQueue()).to(stockExchange()).with(RabbitTopics.RK_STOCK_RESERVE);
    }
    @Bean public Binding bindRelease() {
        return BindingBuilder.bind(stockReleaseQueue()).to(stockExchange()).with(RabbitTopics.RK_STOCK_RELEASE);
    }

    @Bean public MessageConverter jsonConverter() { return new Jackson2JsonMessageConverter(); }
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(jsonConverter());
        return t;
    }
}
