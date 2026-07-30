package pt.ulusofona.digital.wallet.exception.lusofona;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulusofona.digital.wallet.domain.student.*;
import pt.ulusofona.digital.wallet.exception.ValidationException;
import pt.ulusofona.digital.wallet.exception.http.*;

/**
 * Utility class for validating Lusofona API responses and handling error codes.
 * 
 * The Lusofona API returns error codes in the response body even when HTTP status is 200 OK.
 * This validator checks for these error codes and throws appropriate exceptions.
 */
public class LusofonaResponseValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(LusofonaResponseValidator.class);
    
    /**
     * Validate a response object that has an errorCode field.
     * If the errorCode indicates an error, throws an appropriate exception.
     * 
     * @param response the response object (can be null)
     * @param errorCode the error code from the response
     * @param operationContext context describing the operation (for error messages)
     * @throws LusofonaApiException if the error code indicates an error
     * @throws BadRequestException if validation or format errors occur
     * @throws UnauthorizedException if authentication fails
     * @throws ForbiddenException if operation is not allowed
     * @throws NotFoundException if data is not found
     */
    public static void validateResponse(Object response, String errorCode, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        
        // If no error code, assume success
        if (!LusofonaErrorCode.isError(errorCode)) {
            logger.debug("Response validation successful for operation: {}", operationContext);
            return;
        }
        
        LusofonaErrorCode lusofonaError = LusofonaErrorCode.fromCode(errorCode);
        logger.warn("Lusofona API returned error code {} ({}) for operation: {}", 
                errorCode, lusofonaError.getName(), operationContext);
        
        // Map Lusofona error codes to our exception hierarchy
        switch (lusofonaError) {
            case OK:
                // This shouldn't happen as we check isError above, but handle it anyway
                return; // No exception
                
            case AUTHENTICATION_FAILURE, INVALID_INSTALLATION_KEY:
                throw new UnauthorizedException(String.format("%s: %s (Operation: %s)", 
                        lusofonaError.getName(), lusofonaError.getDescription(), operationContext));
                
            case OPERATION_NOT_AVAILABLE, APPLICATION_NOT_REGISTERED:
                throw new ForbiddenException(String.format("%s: %s (Operation: %s)", 
                        lusofonaError.getName(), lusofonaError.getDescription(), operationContext));
                
            case DATA_NOT_FOUND:
                throw new NotFoundException(String.format("%s: %s (Operation: %s)", 
                        lusofonaError.getName(), lusofonaError.getDescription(), operationContext));
                
            case INCORRECT_FORMAT, INVALID_REQUEST:
                throw new BadRequestException(String.format("%s: %s (Operation: %s)", 
                        lusofonaError.getName(), lusofonaError.getDescription(), operationContext));
                
            case BUSINESS_RULE_VALIDATION:
                throw new ValidationException(String.format("%s: %s (Operation: %s)", 
                        lusofonaError.getName(), lusofonaError.getDescription(), operationContext));
                
            case IMEI_ALREADY_REGISTERED, GENERIC_ERROR:
                throw new LusofonaApiException(lusofonaError, operationContext);
                
            default:
                throw new LusofonaApiException(lusofonaError, operationContext);
        }
    }
    
    /**
     * Validate StudentEnrolments response
     */
    public static void validateStudentEnrolments(StudentEnrolments response, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        if (response != null) {
            validateResponse(response, response.errorCode(), operationContext);
        }
    }
    
    /**
     * Validate StudentGrades response
     */
    public static void validateStudentGrades(StudentGrades response, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        if (response != null) {
            validateResponse(response, response.errorCode(), operationContext);
        }
    }
    
    /**
     * Validate StudentEvals response
     */
    public static void validateStudentEvals(StudentEvals response, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        if (response != null) {
            validateResponse(response, response.errorCode(), operationContext);
        }
    }
    
    /**
     * Validate StudentCourseCredits response
     */
    public static void validateStudentCourseCredits(StudentCourseCredits response, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        if (response != null) {
            validateResponse(response, response.errorCode(), operationContext);
        }
    }
    
    /**
     * Validate StudentSchedule response
     */
    public static void validateStudentSchedule(StudentSchedule response, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        if (response != null) {
            validateResponse(response, response.errorCode(), operationContext);
        }
    }
    
    /**
     * Validate generic Object response (for login/registration).
     * Attempts to extract errorCode field if present.
     */
    public static void validateGenericResponse(Object response, String operationContext) 
            throws LusofonaApiException, BadRequestException, UnauthorizedException, ForbiddenException, NotFoundException, ValidationException {
        if (response == null) {
            return;
        }
        
        String errorCode = null;
        
        // Try to extract errorCode using reflection or type checking
        if (response instanceof StudentLogin studentLogin) {
            errorCode = studentLogin.getErrorCode();
        } else if (response instanceof StudentRegistration studentRegistration) {
            errorCode = studentRegistration.getErrorCode();
        } else if (response instanceof java.util.Map) {
            // Handle Map responses
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> responseMap = (java.util.Map<String, Object>) response;
            Object errorCodeObj = responseMap.get("errorCode");
            if (errorCodeObj != null) {
                errorCode = errorCodeObj.toString();
            }
        } else {
            // Try reflection as last resort
            try {
                java.lang.reflect.Method getErrorCode = response.getClass().getMethod("getErrorCode");
                Object errorCodeObj = getErrorCode.invoke(response);
                if (errorCodeObj != null) {
                    errorCode = errorCodeObj.toString();
                }
            } catch (Exception e) {
                // No errorCode field or method - assume success
                logger.debug("Could not extract errorCode from response, assuming success for operation: {}", operationContext);
                return;
            }
        }
        
        validateResponse(response, errorCode, operationContext);
    }
}

