package dev.jeppu.web.controller;

import dev.jeppu.config.ApplicationPropertiesConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
@Slf4j
public class RabbitMqDemoController {
    private final ApplicationPropertiesConfig propertiesConfig;
    private final RabbitTemplate rabbitTemplate;

    @PostMapping("/send")
    public void publishMessage(@RequestBody MyPayload payload) {
        rabbitTemplate.convertAndSend(propertiesConfig.orderEventsExchange(), payload.routingKey, payload);
        log.info("Message : {} published to Queue : {} successfully", payload, payload.routingKey);
    }

    record MyPayload(String routingKey, String message) {}
}
