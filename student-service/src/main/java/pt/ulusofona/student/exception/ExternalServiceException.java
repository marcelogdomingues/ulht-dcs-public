package pt.ulusofona.student.exception;

/**
 * Exception thrown when external service (Lusofona/Fulfilment) fails
 */
public class ExternalServiceException extends StudentServiceException {
    
    public ExternalServiceException(String message) {
        super(ErrorCodes.FULFILMENT_SERVICE_ERROR, message);
    }
    
    public ExternalServiceException(String message, Throwable cause) {
        super(ErrorCodes.FULFILMENT_SERVICE_ERROR, message, cause);
    }
    
    public ExternalServiceException(ErrorCodes errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}

