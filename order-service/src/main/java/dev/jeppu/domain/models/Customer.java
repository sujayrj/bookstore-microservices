package dev.jeppu.domain.models;

import jakarta.persistence.Embeddable;

@Embeddable
public record Customer(String name, String email, String phone) {}
