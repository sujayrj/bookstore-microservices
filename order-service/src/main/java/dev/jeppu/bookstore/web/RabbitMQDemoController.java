package dev.jeppu.bookstore.web;

import dev.jeppu.bookstore.ApplicationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class RabbitMQDemoController {
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationProperties properties;

    @PostMapping("/send")
    public void publishMessage(@RequestBody MyMessage message) {
        rabbitTemplate.convertAndSend(
                properties.orderEventsExchange(),
                message.routingKey(),
                message.payload().content()
        );
    }
}

record MyMessage(String routingKey, MyPayload payload) {
}

record MyPayload(String content) {
}