package dev.jeppu.domain.models;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record CreateOrderRequest(
        @Valid Customer customer,
        @Valid Address deliveryAddress,
        @NotEmpty(message = "Items cannot be empty") Set<OrderItem> items) {}
