package com.example.dcs.sis.exception.http;

/**
 * Exception thrown when authentication fails
 * Maps to error code TE-003: "Locked Cookie Jar"
 */
public class UnauthorizedException extends Exception {

    public UnauthorizedException() {
        super("Unauthorized access – the cookie jar is locked tight");
    }

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(Throwable cause) {
        super(cause);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "UnauthorizedException: " + getMessage();
    }
} 