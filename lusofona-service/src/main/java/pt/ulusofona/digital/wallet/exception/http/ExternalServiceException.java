package pt.ulusofona.digital.wallet.exception.http;

/**
 * Exception thrown when external service calls fail
 * Maps to error code TE-013: "Grumpy API"
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException() {
        super("External service error – the API is in a bad mood");
    }

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(Throwable cause) {
        super(cause);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "ExternalServiceException: " + getMessage();
    }
} 