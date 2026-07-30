package pt.ulusofona.digital.wallet.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for error handling operations
 * 
 * This class provides convenient methods for creating error responses,
 * logging errors, and handling common error scenarios.
 */
public class ErrorUtils {

    private static final Logger logger = LoggerFactory.getLogger(ErrorUtils.class);

    /**
     * Create an error response with the specified error code and message
     * 
     * @param errorCode the error code enum
     * @param message the error message
     * @return ErrorResponse object
     */
    public static ErrorResponse createErrorResponse(ErrorCodes errorCode, String message) {
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        logger.error("Error Response: {}", errorResponse);
        return errorResponse;
    }

    /**
     * Create an error response with trace information
     * 
     * @param errorCode the error code enum
     * @param message the error message
     * @param trace the stack trace or trace information
     * @return ErrorResponse object
     */
    public static ErrorResponse createErrorResponse(ErrorCodes errorCode, String message, String trace) {
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, trace);
        logger.error("Error Response: {}", errorResponse);
        return errorResponse;
    }

    /**
     * Create an error response from an exception
     * 
     * @param errorCode the error code enum
     * @param exception the exception that occurred
     * @return ErrorResponse object
     */
    public static ErrorResponse createErrorResponse(ErrorCodes errorCode, Exception exception) {
        String message = exception.getMessage() != null ? exception.getMessage() : "An error occurred";
        String trace = getStackTrace(exception);
        
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message, trace);
        logger.error("Error Response: {}", errorResponse);
        
        return errorResponse;
    }

    /**
     * Log an error with the error code information
     * 
     * @param errorCode the error code enum
     * @param message the error message
     */
    public static void logError(ErrorCodes errorCode, String message) {
        logger.error("Error: {} - {} - {}", errorCode.getCode(), errorCode.getFunName(), message);
    }

    /**
     * Log an error with exception details (without stack trace)
     * 
     * @param errorCode the error code enum
     * @param message the error message
     * @param exception the exception that occurred
     */
    public static void logError(ErrorCodes errorCode, String message, Exception exception) {
        logger.error("Error: {} - {} - {} - Exception: {}", 
            errorCode.getCode(), errorCode.getFunName(), message, exception.getClass().getSimpleName());
    }

    /**
     * Log a warning with the error code information
     * 
     * @param errorCode the error code enum
     * @param message the warning message
     */
    public static void logWarning(ErrorCodes errorCode, String message) {
        logger.warn("Warning: {} - {} - {}", errorCode.getCode(), errorCode.getFunName(), message);
    }

    /**
     * Get a formatted stack trace string (for internal use only)
     * 
     * @param exception the exception
     * @return formatted stack trace string
     */
    private static String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getSimpleName()).append(": ").append(exception.getMessage());
        
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace.length > 0) {
            sb.append("\n\tat ").append(stackTrace[0]);
            if (stackTrace.length > 1) {
                sb.append("\n\tat ").append(stackTrace[1]);
            }
        }
        
        return sb.toString();
    }

    /**
     * Check if an error code represents a client error (4xx)
     * 
     * @param errorCode the error code enum
     * @return true if it's a client error
     */
    public static boolean isClientError(ErrorCodes errorCode) {
        return switch (errorCode) {
            case NOT_FOUND, BAD_REQUEST, UNAUTHORIZED, FORBIDDEN, 
                 CONFLICT, UNPROCESSABLE_ENTITY, TOO_MANY_REQUESTS,
                 VALIDATION_ERROR -> true;
            default -> false;
        };
    }

    /**
     * Check if an error code represents a server error (5xx)
     * 
     * @param errorCode the error code enum
     * @return true if it's a server error
     */
    public static boolean isServerError(ErrorCodes errorCode) {
        return switch (errorCode) {
            case INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NETWORK_ERROR,
                 TIMEOUT, DATABASE_ERROR, EXTERNAL_SERVICE_ERROR,
                 CONFIGURATION_ERROR, FILE_ERROR, SECURITY_ERROR,
                 BUSINESS_LOGIC_ERROR, RESOURCE_EXHAUSTED, MAINTENANCE_ERROR -> true;
            default -> false;
        };
    }

    /**
     * Get a user-friendly error message
     * 
     * @param errorCode the error code enum
     * @return user-friendly message
     */
    public static String getUserFriendlyMessage(ErrorCodes errorCode) {
        return switch (errorCode) {
            case NOT_FOUND -> "The requested resource was not found";
            case BAD_REQUEST -> "The request was invalid or malformed";
            case UNAUTHORIZED -> "Authentication is required to access this resource";
            case FORBIDDEN -> "You don't have permission to access this resource";
            case CONFLICT -> "There was a conflict with the current state of the resource";
            case UNPROCESSABLE_ENTITY -> "The request was well-formed but cannot be processed";
            case TOO_MANY_REQUESTS -> "Too many requests, please try again later";
            case INTERNAL_SERVER_ERROR -> "An internal server error occurred";
            case SERVICE_UNAVAILABLE -> "The service is temporarily unavailable";
            case NETWORK_ERROR -> "A network error occurred";
            case TIMEOUT -> "The request timed out";
            case DATABASE_ERROR -> "A database error occurred";
            case EXTERNAL_SERVICE_ERROR -> "An external service error occurred";
            case CONFIGURATION_ERROR -> "A configuration error occurred";
            case VALIDATION_ERROR -> "Data validation failed";
            case FILE_ERROR -> "A file operation error occurred";
            case SECURITY_ERROR -> "A security error occurred";
            case BUSINESS_LOGIC_ERROR -> "A business logic error occurred";
            case RESOURCE_EXHAUSTED -> "System resources are exhausted";
            case MAINTENANCE_ERROR -> "The system is under maintenance";
            // Lusofona API Error Codes
            case LUSOFONA_OK -> "Operation completed successfully";
            case LUSOFONA_GENERIC_ERROR -> "A generic error occurred in the Lusofona system";
            case LUSOFONA_AUTH_FAILURE -> "Authentication failed - invalid credentials or expired session";
            case LUSOFONA_IMEI_REGISTERED -> "Device already registered in the Lusofona system";
            case LUSOFONA_APP_NOT_REGISTERED -> "Application is not registered for this user";
            case LUSOFONA_OPERATION_UNAVAILABLE -> "This operation is not available for your user profile";
            case LUSOFONA_INCORRECT_FORMAT -> "Request has incorrect format or invalid data";
            case LUSOFONA_DATA_NOT_FOUND -> "The requested data was not found in the Lusofona system";
            case LUSOFONA_INVALID_KEY -> "The installation key is invalid or expired";
            case LUSOFONA_INVALID_REQUEST -> "The request is invalid or malformed";
            case LUSOFONA_BUSINESS_RULE -> "Business rule validation failed";
        };
    }
} 