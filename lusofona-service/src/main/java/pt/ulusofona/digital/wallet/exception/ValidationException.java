package pt.ulusofona.digital.wallet.exception;

/**
 * Exception thrown when data validation fails
 * Maps to error code TE-015: "Spoiled Milk"
 */
public class ValidationException extends Exception {

    public ValidationException() {
        super("Data validation error – this milk has gone bad");
    }

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(Throwable cause) {
        super(cause);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "ValidationException: " + getMessage();
    }
} 