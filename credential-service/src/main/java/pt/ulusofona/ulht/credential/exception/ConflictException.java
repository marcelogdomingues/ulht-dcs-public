package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when a conflict occurs.
 * Independent exception for the credential service.
 */
public class ConflictException extends Exception {
    
    public ConflictException() {
        super("Conflict occurred");
    }
    
    public ConflictException(String message) {
        super(message);
    }
    
    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
