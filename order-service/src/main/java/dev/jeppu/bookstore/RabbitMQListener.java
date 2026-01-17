package dev.jeppu.bookstore;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class RabbitMQListener {
    private final ApplicationProperties properties;

    @RabbitListener(queues = "${orders.new-orders-queue}")
    public void consumeMessageFromNewOrdersQueue(String message) {
        log.info("Consumed message : {} from {}", message, properties.newOrdersQueue());
    }

    @RabbitListener(queues = "${orders.delivered-orders-queue}")
    public void consumeMessageFromDeliveredOrdersQueue(String message) {
        log.info("Consumed message : {} from {}", message, properties.deliveredOrdersQueue());
    }
}
