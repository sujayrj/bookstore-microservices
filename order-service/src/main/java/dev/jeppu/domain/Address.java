package dev.jeppu.domain;

import jakarta.persistence.Column;

public record Address(
        @Column(name = "delivery_address_line1", nullable = false)
        String addressLine1,

        @Column(name = "delivery_address_line2") String addressLine2,

        @Column(name = "delivery_address_city", nullable = false)
        String addressCity,

        @Column(name = "delivery_address_state", nullable = false)
        String addressState,

        @Column(name = "delivery_address_zip_code", nullable = false)
        String addressZipCode,

        @Column(name = "delivery_address_country", nullable = false)
        String addressCountry) {}
