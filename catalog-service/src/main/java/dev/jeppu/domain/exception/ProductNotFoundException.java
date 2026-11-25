package dev.jeppu.domain.exception;

public class ProductNotFoundException extends RuntimeException {
    public static ProductNotFoundException forCode(String code) {
        String message = String.format("Product with code : %s not found", code);
        return new ProductNotFoundException(message);
    }

    ProductNotFoundException(String message) {
        super(message);
    }
}
