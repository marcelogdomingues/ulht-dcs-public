package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when access is forbidden.
 * Independent exception for the credential service.
 */
public class ForbiddenException extends Exception {
    
    public ForbiddenException() {
        super("Access forbidden");
    }
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
