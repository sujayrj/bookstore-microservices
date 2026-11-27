package dev.jeppu.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record Customer(String name, String email, String phone) {}
