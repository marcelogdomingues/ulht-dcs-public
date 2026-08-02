package com.example.dcs.sis.exception.sis;

/**
 * Sis API Error Codes
 * 
 * This enum maps the error codes returned by the Sis API to meaningful names and descriptions.
 * The Sis API returns these error codes in the response body's errorCode field.
 * 
 * Error Codes:
 * 0 - OK (No error)
 * 1 - Generic error
 * 2 - Authentication failure
 * 3 - IMEI already registered (legacy, kept for backward compatibility)
 * 4 - Application not registered (for the user)
 * 5 - Operation not available for the user profile
 * 6 - Request attribute with incorrect format
 * 7 - Data not found
 * 8 - Invalid installation key
 * 9 - Invalid request
 * 10 - Business rule validation error (with message)
 */
public enum SisErrorCode {
    
    OK("0", "Success", "Operation completed successfully"),
    GENERIC_ERROR("1", "Generic Error", "A generic error occurred in the Sis system"),
    AUTHENTICATION_FAILURE("2", "Authentication Failure", "Authentication failed - invalid credentials or session expired"),
    IMEI_ALREADY_REGISTERED("3", "IMEI Already Registered", "Device IMEI already registered (legacy error code)"),
    APPLICATION_NOT_REGISTERED("4", "Application Not Registered", "Application is not registered for this user"),
    OPERATION_NOT_AVAILABLE("5", "Operation Not Available", "This operation is not available for the user's profile or role"),
    INCORRECT_FORMAT("6", "Incorrect Format", "Request attribute has incorrect format or invalid data type"),
    DATA_NOT_FOUND("7", "Data Not Found", "The requested data was not found in the Sis system"),
    INVALID_INSTALLATION_KEY("8", "Invalid Installation Key", "The installation key provided is invalid or expired"),
    INVALID_REQUEST("9", "Invalid Request", "The request is invalid or malformed"),
    BUSINESS_RULE_VALIDATION("10", "Business Rule Validation Error", "Business rule validation failed");
    
    private final String code;
    private final String name;
    private final String description;
    
    SisErrorCode(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this error code represents a successful operation
     */
    public boolean isSuccess() {
        return this == OK;
    }
    
    /**
     * Check if this error code represents an authentication error
     */
    public boolean isAuthenticationError() {
        return this == AUTHENTICATION_FAILURE || this == INVALID_INSTALLATION_KEY;
    }
    
    /**
     * Check if this error code represents a validation error
     */
    public boolean isValidationError() {
        return this == INCORRECT_FORMAT || this == INVALID_REQUEST || this == BUSINESS_RULE_VALIDATION;
    }
    
    /**
     * Check if this error code represents a not found error
     */
    public boolean isNotFoundError() {
        return this == DATA_NOT_FOUND;
    }
    
    /**
     * Check if this error code represents a forbidden/permission error
     */
    public boolean isForbiddenError() {
        return this == OPERATION_NOT_AVAILABLE || this == APPLICATION_NOT_REGISTERED;
    }
    
    /**
     * Get SisErrorCode from string code
     * 
     * @param code the error code string (e.g., "0", "1", "2")
     * @return the SisErrorCode enum value
     * @throws IllegalArgumentException if the code is not recognized
     */
    public static SisErrorCode fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return OK; // Treat null/empty as success
        }
        
        for (SisErrorCode errorCode : values()) {
            if (errorCode.code.equals(code.trim())) {
                return errorCode;
            }
        }
        
        // If we don't recognize the code, treat it as a generic error
        return GENERIC_ERROR;
    }
    
    /**
     * Check if a code string represents an error
     * 
     * @param code the error code string
     * @return true if the code represents an error (not OK)
     */
    public static boolean isError(String code) {
        return code != null && !code.trim().isEmpty() && !"0".equals(code.trim());
    }
}

