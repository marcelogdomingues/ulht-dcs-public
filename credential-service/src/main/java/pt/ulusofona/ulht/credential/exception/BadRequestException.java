package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when a bad request is made.
 * Independent exception for the credential service.
 */
public class BadRequestException extends Exception {
    
    public BadRequestException() {
        super("Bad request");
    }
    
    public BadRequestException(String message) {
        super(message);
    }
    
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
