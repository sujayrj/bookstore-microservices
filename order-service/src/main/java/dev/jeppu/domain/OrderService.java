package dev.jeppu.domain;

import dev.jeppu.domain.models.CreateOrderRequest;
import dev.jeppu.domain.models.CreateOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final SecurityService securityService;
    private final OrderValidator orderValidator;

    public CreateOrderResponse createOrder(CreateOrderRequest orderRequest) {
        orderValidator.validateOrder(orderRequest);
        OrderEntity orderEntity = OrderMapper.toOrderEntity(orderRequest);

        orderEntity.setUsername(securityService.getLoginUserName());
        OrderEntity savedOrder = orderRepository.save(orderEntity);
        log.info(
                "Order Id : {} and orderNumber : {} saved successfully",
                savedOrder.getId(),
                savedOrder.getOrderNumber());
        return new CreateOrderResponse(savedOrder.getOrderNumber());
    }
}
