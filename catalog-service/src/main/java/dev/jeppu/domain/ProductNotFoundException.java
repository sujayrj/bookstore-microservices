package dev.jeppu.domain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }

    public static ProductNotFoundException forCode(String productCode) {
        String message = String.format("Product with code : %s not found", productCode);
        log.warn(message);
        return new ProductNotFoundException(message);
    }
}
