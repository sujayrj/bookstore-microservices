package dev.jeppu.domain.models;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record OrderItem(
        @NotBlank(message = "Product code cannot be blank") String code,
        @NotBlank(message = "Product name cannot be blank") String name,

        @DecimalMin("0.1") @NotNull(message = "Price is required") BigDecimal price,

        @Min(1) @NotNull(message = "Quantity is required") Integer quantity) {}
