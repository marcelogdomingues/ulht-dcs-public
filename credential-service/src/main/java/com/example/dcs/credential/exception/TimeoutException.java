package com.example.dcs.credential.exception;

/**
 * Exception thrown when a service call times out.
 * Independent exception for the credential service.
 */
public class TimeoutException extends Exception {
    
    public TimeoutException() {
        super("Request timeout");
    }
    
    public TimeoutException(String message) {
        super(message);
    }
    
    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
