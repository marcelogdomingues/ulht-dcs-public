package com.example.dcs.sis.exception.http;

/**
 * Exception thrown when access is forbidden
 * Maps to error code TE-004: "Forbidden Fruit"
 */
public class ForbiddenException extends Exception {

    public ForbiddenException() {
        super("Access forbidden – this fruit is not for you");
    }

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException(Throwable cause) {
        super(cause);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "ForbiddenException: " + getMessage();
    }
} 