package dev.jeppu.domain;

import dev.jeppu.domain.models.CreateOrderRequest;
import dev.jeppu.domain.models.OrderItem;
import dev.jeppu.domain.models.OrderStatus;
import java.util.UUID;

public class OrderMapper {
    public static OrderEntity toOrderEntity(CreateOrderRequest orderRequest) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderNumber(UUID.randomUUID().toString());
        orderEntity.setStatus(OrderStatus.NEW);
        orderEntity.setCustomer(orderRequest.customer());
        orderEntity.setDeliveryAddress(orderRequest.deliveryAddress());

        orderRequest.items().stream().map(OrderMapper::toOrderItemEntity).forEach(orderEntity::addItem);

        return orderEntity;
    }

    public static OrderItemEntity toOrderItemEntity(OrderItem item) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setName(item.name());
        itemEntity.setCode(item.code());
        itemEntity.setQuantity(item.quantity());
        itemEntity.setPrice(item.price());
        return itemEntity;
    }
}
