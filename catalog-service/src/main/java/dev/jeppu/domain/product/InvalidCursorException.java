package dev.jeppu.domain.product;

/** Thrown when a client supplies a malformed or unsupported pagination cursor. */
public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException(String message) {
        super(message);
    }
}
