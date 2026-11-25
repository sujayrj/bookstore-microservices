package dev.jeppu.domain.exception;

public class ProductAlreadyExistsException extends RuntimeException {
    public static ProductAlreadyExistsException forCode(String code) {
        return new ProductAlreadyExistsException(
                "Product with code: " + code + " already exists. Cannot create a new Product with same code");
    }

    public ProductAlreadyExistsException(String message) {
        super(message);
    }
}
