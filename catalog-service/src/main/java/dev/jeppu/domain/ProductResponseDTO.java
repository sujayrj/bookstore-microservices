package dev.jeppu.domain;

import java.math.BigDecimal;

public record ProductResponseDTO(String code, String name, String description, BigDecimal price, String imageUrl) {}
