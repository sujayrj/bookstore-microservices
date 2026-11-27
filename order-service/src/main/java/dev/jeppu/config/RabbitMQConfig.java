package dev.jeppu.config;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {
    private final ApplicationPropertiesConfig applicationPropertiesConfig;

    @Bean
    DirectExchange ordersExchange() {
        return new DirectExchange(applicationPropertiesConfig.orderEventsExchange());
    }

    @Bean
    Queue newOrdersQueue() {
        return QueueBuilder.durable(applicationPropertiesConfig.newOrdersQueue())
                .build();
    }

    @Bean
    Queue deliveredOrdersQueue() {
        return QueueBuilder.durable(applicationPropertiesConfig.deliveredOrdersQueue())
                .build();
    }

    @Bean
    Queue cancelledOrdersQueue() {
        return QueueBuilder.durable(applicationPropertiesConfig.cancelledOrdersQueue())
                .build();
    }

    @Bean
    Queue errorOrdersQueue() {
        return QueueBuilder.durable(applicationPropertiesConfig.errorOrdersQueue())
                .build();
    }

    @Bean
    Binding newOrdersBinding() {
        return BindingBuilder.bind(newOrdersQueue())
                .to(ordersExchange())
                .with(applicationPropertiesConfig.newOrdersQueue());
    }

    @Bean
    Binding deliveredOrdersBinding() {
        return BindingBuilder.bind(deliveredOrdersQueue())
                .to(ordersExchange())
                .with(applicationPropertiesConfig.deliveredOrdersQueue());
    }

    @Bean
    Binding cancelledOrdersBinding() {
        return BindingBuilder.bind(cancelledOrdersQueue())
                .to(ordersExchange())
                .with(applicationPropertiesConfig.cancelledOrdersQueue());
    }

    @Bean
    Binding errorOrdersBinding() {
        return BindingBuilder.bind(errorOrdersQueue())
                .to(ordersExchange())
                .with(applicationPropertiesConfig.errorOrdersQueue());
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, JacksonJsonMessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    JacksonJsonMessageConverter jacksonJsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(jsonMapper);
    }
}
