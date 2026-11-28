package dev.jeppu.web.controller;

import dev.jeppu.domain.OrderService;
import dev.jeppu.domain.SecurityService;
import dev.jeppu.domain.models.CreateOrderRequest;
import dev.jeppu.domain.models.CreateOrderResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {
    private final OrderService orderService;
    private final SecurityService securityService;

    @PostMapping
    CreateOrderResponse createOrder(@Valid @RequestBody CreateOrderRequest orderRequest) {
        String userName = securityService.getLoginUserName();
        log.info("Creating order for user : {}", userName);
        return orderService.createOrder(orderRequest);
    }
}
