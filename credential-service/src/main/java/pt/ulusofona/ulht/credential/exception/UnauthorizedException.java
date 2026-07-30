package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when access is unauthorized.
 * Independent exception for the credential service.
 */
public class UnauthorizedException extends Exception {
    
    public UnauthorizedException() {
        super("Unauthorized access");
    }
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
