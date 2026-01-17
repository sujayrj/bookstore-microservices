package dev.jeppu.bookstore.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jeppu.bookstore.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class RabbitMQConfig {
    private final ApplicationProperties properties;

    @Bean
    DirectExchange ordersExchange() {
        return new DirectExchange(properties.orderEventsExchange());
    }

    @Bean
    Queue newOrdersQueue() {
        return QueueBuilder.durable(properties.newOrdersQueue()).build();
    }

    @Bean
    Queue deliveredOrdersQueue() {
        return QueueBuilder.durable(properties.deliveredOrdersQueue()).build();
    }

    @Bean
    Queue errorOrdersQueue() {
        return QueueBuilder.durable(properties.errorOrdersQueue()).build();
    }

    @Bean
    Queue cancelledOrdersQueue() {
        return QueueBuilder.durable(properties.cancelledOrdersQueue()).build();
    }

    @Bean
    Binding newOrdersQueueToExchangeBinding() {
        return BindingBuilder.bind(newOrdersQueue()).to(ordersExchange()).with(properties.newOrdersQueue());
    }

    @Bean
    Binding deliveredOrdersToExchangeBinding() {
        return BindingBuilder.bind(deliveredOrdersQueue()).to(ordersExchange()).with(properties.deliveredOrdersQueue());
    }

    @Bean
    Binding errorOrdersToExchangeBinding() {
        return BindingBuilder.bind(errorOrdersQueue()).to(ordersExchange()).with(properties.errorOrdersQueue());
    }

    @Bean
    Binding cancelledOrdersToExchangeBinding() {
        return BindingBuilder.bind(cancelledOrdersQueue()).to(ordersExchange()).with(properties.cancelledOrdersQueue());
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jackson2JsonMessageConverter(objectMapper));
        return rabbitTemplate;
    }
}
