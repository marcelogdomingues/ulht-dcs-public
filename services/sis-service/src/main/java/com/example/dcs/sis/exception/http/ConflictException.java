package com.example.dcs.sis.exception.http;

/**
 * Exception thrown when there's a resource conflict
 * Maps to error code TE-005: "Duplicate Donut"
 */
public class ConflictException extends Exception {

    public ConflictException() {
        super("Resource conflict – someone already took this donut");
    }

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(Throwable cause) {
        super(cause);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "ConflictException: " + getMessage();
    }
} 