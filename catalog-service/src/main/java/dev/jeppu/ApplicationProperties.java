package dev.jeppu;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "catalog")
public record ApplicationProperties(@DefaultValue(value = "10") @Min(value = 1) int pageSize) {}
