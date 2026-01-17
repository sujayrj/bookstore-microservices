package dev.jeppu.bookstore.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String message) {
        super(message);
    }

    public static ProductNotFoundException forCode(String productCode) {
        String message = String.format("Product with code : %s not found", productCode);
        return new ProductNotFoundException(message);
    }
}
