package dev.jeppu.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RabbitMqDemoListener {
    @RabbitListener(queues = "${orders.new-orders-queue}")
    public void handleNewOrder(RabbitMqDemoController.MyPayload payload) {
        log.info("Message read from Queue : new-orders = msg : {}", payload.message());
    }

    @RabbitListener(queues = "${orders.delivered-orders-queue}")
    public void handleDeliveredOrder(RabbitMqDemoController.MyPayload payload) {
        log.info("Message read from Queue : delivered-orders = msg : {}", payload.message());
    }
}
