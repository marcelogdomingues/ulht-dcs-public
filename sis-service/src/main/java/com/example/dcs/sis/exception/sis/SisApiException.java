package com.example.dcs.sis.exception.sis;

import lombok.Getter;

/**
 * Exception thrown when the Sis API returns an error code in the response body.
 * 
 * Unlike HTTP errors which are handled by the CustomErrorDecoder, this exception
 * handles business logic errors that are returned with HTTP 200 OK status but
 * contain an error code in the response body.
 */
@Getter
public class SisApiException extends RuntimeException {
    
    private final SisErrorCode errorCode;
    private final String errorMessage;
    private final String operationContext;
    
    /**
     * Create a new SisApiException
     * 
     * @param errorCode the Sis error code
     * @param errorMessage additional error message from the API
     * @param operationContext context about which operation failed
     */
    public SisApiException(SisErrorCode errorCode, String errorMessage, String operationContext) {
        super(String.format("Sis API Error [%s - %s]: %s (Operation: %s)", 
                errorCode.getCode(), 
                errorCode.getName(), 
                errorMessage != null ? errorMessage : errorCode.getDescription(),
                operationContext));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage != null ? errorMessage : errorCode.getDescription();
        this.operationContext = operationContext;
    }
    
    /**
     * Create a new SisApiException with just the error code
     * 
     * @param errorCode the Sis error code
     * @param operationContext context about which operation failed
     */
    public SisApiException(SisErrorCode errorCode, String operationContext) {
        this(errorCode, null, operationContext);
    }
    
    /**
     * Create a SisApiException from a string error code
     * 
     * @param errorCodeString the error code as a string
     * @param operationContext context about which operation failed
     * @return SisApiException instance
     */
    public static SisApiException fromErrorCode(String errorCodeString, String operationContext) {
        SisErrorCode errorCode = SisErrorCode.fromCode(errorCodeString);
        return new SisApiException(errorCode, operationContext);
    }
    
    /**
     * Create a SisApiException from a string error code with custom message
     * 
     * @param errorCodeString the error code as a string
     * @param errorMessage custom error message
     * @param operationContext context about which operation failed
     * @return SisApiException instance
     */
    public static SisApiException fromErrorCode(String errorCodeString, String errorMessage, String operationContext) {
        SisErrorCode errorCode = SisErrorCode.fromCode(errorCodeString);
        return new SisApiException(errorCode, errorMessage, operationContext);
    }
}

