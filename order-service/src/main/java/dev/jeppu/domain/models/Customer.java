package dev.jeppu.domain.models;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public record Customer(
        @NotBlank(message = "Customer name is required") String name,
        @NotBlank(message = "Customer email is required") String email,
        @NotBlank(message = "Customer phone is required") String phone) {}
