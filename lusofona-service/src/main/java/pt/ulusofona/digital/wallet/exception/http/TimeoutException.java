package pt.ulusofona.digital.wallet.exception.http;

/**
 * Exception thrown when operations timeout
 * Maps to error code TE-011: "Slow Snail"
 */
public class TimeoutException extends Exception {

    public TimeoutException() {
        super("Request timeout – this snail is taking forever");
    }

    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String toString() {
        return "TimeoutException: " + getMessage();
    }
} 