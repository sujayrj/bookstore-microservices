package dev.jeppu.client;

import java.math.BigDecimal;

public record Product(String code, String name, BigDecimal price, Integer quantity, String imageUrl) {}
