package pt.ulusofona.digital.wallet.exception.lusofona;

import lombok.Getter;

/**
 * Exception thrown when the Lusofona API returns an error code in the response body.
 * 
 * Unlike HTTP errors which are handled by the CustomErrorDecoder, this exception
 * handles business logic errors that are returned with HTTP 200 OK status but
 * contain an error code in the response body.
 */
@Getter
public class LusofonaApiException extends RuntimeException {
    
    private final LusofonaErrorCode errorCode;
    private final String errorMessage;
    private final String operationContext;
    
    /**
     * Create a new LusofonaApiException
     * 
     * @param errorCode the Lusofona error code
     * @param errorMessage additional error message from the API
     * @param operationContext context about which operation failed
     */
    public LusofonaApiException(LusofonaErrorCode errorCode, String errorMessage, String operationContext) {
        super(String.format("Lusofona API Error [%s - %s]: %s (Operation: %s)", 
                errorCode.getCode(), 
                errorCode.getName(), 
                errorMessage != null ? errorMessage : errorCode.getDescription(),
                operationContext));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage != null ? errorMessage : errorCode.getDescription();
        this.operationContext = operationContext;
    }
    
    /**
     * Create a new LusofonaApiException with just the error code
     * 
     * @param errorCode the Lusofona error code
     * @param operationContext context about which operation failed
     */
    public LusofonaApiException(LusofonaErrorCode errorCode, String operationContext) {
        this(errorCode, null, operationContext);
    }
    
    /**
     * Create a LusofonaApiException from a string error code
     * 
     * @param errorCodeString the error code as a string
     * @param operationContext context about which operation failed
     * @return LusofonaApiException instance
     */
    public static LusofonaApiException fromErrorCode(String errorCodeString, String operationContext) {
        LusofonaErrorCode errorCode = LusofonaErrorCode.fromCode(errorCodeString);
        return new LusofonaApiException(errorCode, operationContext);
    }
    
    /**
     * Create a LusofonaApiException from a string error code with custom message
     * 
     * @param errorCodeString the error code as a string
     * @param errorMessage custom error message
     * @param operationContext context about which operation failed
     * @return LusofonaApiException instance
     */
    public static LusofonaApiException fromErrorCode(String errorCodeString, String errorMessage, String operationContext) {
        LusofonaErrorCode errorCode = LusofonaErrorCode.fromCode(errorCodeString);
        return new LusofonaApiException(errorCode, errorMessage, operationContext);
    }
}

