package dev.jeppu.domain.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record Product(
        @NotEmpty(message = "Product Code cannot be blank") String code,
        @NotEmpty(message = "Product name cannot be blank") String name,
        String description,
        String imageUrl,

        @NotNull(message = "Price cannot be null") @DecimalMin(value = "0.1", message = "Minimum Price must be greater than or equal to 0.1") BigDecimal price) {}
