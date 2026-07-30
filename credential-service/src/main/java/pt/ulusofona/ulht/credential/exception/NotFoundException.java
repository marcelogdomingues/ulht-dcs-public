package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when a resource is not found.
 * Independent exception for the credential service.
 */
public class NotFoundException extends Exception {
    
    public NotFoundException() {
        super("Resource not found");
    }
    
    public NotFoundException(String message) {
        super(message);
    }
    
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
