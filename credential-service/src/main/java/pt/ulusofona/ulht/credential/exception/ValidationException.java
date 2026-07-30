package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when validation fails.
 * Independent exception for the credential service.
 */
public class ValidationException extends Exception {
    
    public ValidationException() {
        super("Validation failed");
    }
    
    public ValidationException(String message) {
        super(message);
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
