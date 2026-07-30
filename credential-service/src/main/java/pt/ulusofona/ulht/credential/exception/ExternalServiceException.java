package pt.ulusofona.ulht.credential.exception;

/**
 * Exception thrown when external service calls fail.
 * Independent exception for the credential service.
 */
public class ExternalServiceException extends Exception {

    private final String errorCode;
    private final String serviceName;

    public ExternalServiceException() {
        super(ErrorCodes.EXTERNAL_SERVICE_ERROR.getDescription());
        this.errorCode = ErrorCodes.EXTERNAL_SERVICE_ERROR.getCode();
        this.serviceName = "Unknown";
    }

    public ExternalServiceException(String message) {
        super(message);
        this.errorCode = ErrorCodes.EXTERNAL_SERVICE_ERROR.getCode();
        this.serviceName = "Unknown";
    }

    public ExternalServiceException(String message, String serviceName) {
        super(message);
        this.errorCode = ErrorCodes.EXTERNAL_SERVICE_ERROR.getCode();
        this.serviceName = serviceName;
    }

    public ExternalServiceException(String message, String serviceName, String errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.serviceName = serviceName;
    }

    public ExternalServiceException(Throwable cause) {
        super("External service error", cause);
        this.errorCode = ErrorCodes.EXTERNAL_SERVICE_ERROR.getCode();
        this.serviceName = "Unknown";
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCodes.EXTERNAL_SERVICE_ERROR.getCode();
        this.serviceName = "Unknown";
    }

    public ExternalServiceException(String message, String serviceName, Throwable cause) {
        super(message, cause);
        this.errorCode = ErrorCodes.EXTERNAL_SERVICE_ERROR.getCode();
        this.serviceName = serviceName;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getServiceName() {
        return serviceName;
    }

    @Override
    public String toString() {
        return String.format("ExternalServiceException [%s]: %s (Service: %s)", 
                errorCode, getMessage(), serviceName);
    }
}
