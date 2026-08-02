package com.example.dcs.sis.exception;

/**
 * Custom Error Codes with fun names and descriptions
 * 
 * This enum provides a centralized way to manage error codes across the DCS system.
 * Each error code has:
 * - A unique identifier (e.g., "TE-001")
 * - A fun name (e.g., "Wired Nutella")
 * - A descriptive message explaining what went wrong
 * 
 * Usage:
 * - Use these codes in exception handling
 * - Reference them in API responses
 * - Use them for logging and monitoring
 */
public enum ErrorCodes {

    // Resource not found errors
    NOT_FOUND("TE-001", "Wired Nutella", "The requested resource (like a student record, course, or file) could not be found in our system. This usually means the ID or reference you provided doesn't exist or has been removed."),

    // Bad request errors
    BAD_REQUEST("TE-002", "Broken Biscuit", "The request you sent is not properly formatted or is missing required information. Please check that all required fields are filled out correctly and try again."),

    // Authentication errors
    UNAUTHORIZED("TE-003", "Locked Cookie Jar", "You need to log in or provide valid authentication credentials to access this resource. Please sign in with your username and password."),

    // Authorization errors
    FORBIDDEN("TE-004", "Forbidden Fruit", "You don't have permission to access this resource or perform this action. This might be because your account doesn't have the required role or access level."),

    // Conflict errors
    CONFLICT("TE-005", "Duplicate Donut", "The resource you're trying to create already exists, or there's a conflict with existing data. For example, trying to register a student with an email that's already in use."),

    // Validation errors
    UNPROCESSABLE_ENTITY("TE-006", "Messy Milkshake", "The request contains valid information, but it doesn't make sense for the operation you're trying to perform. For example, trying to enroll a student in a course that doesn't exist."),

    // Rate limiting errors
    TOO_MANY_REQUESTS("TE-007", "Hungry Hippo", "You're making too many requests too quickly. Please wait a moment before trying again. This helps us maintain system performance for all users."),

    // Internal server errors
    INTERNAL_SERVER_ERROR("TE-008", "Kitchen Chaos", "Something unexpected went wrong on our servers. This is not your fault - it's an internal system issue that our team will investigate and fix."),

    // Service unavailable errors
    SERVICE_UNAVAILABLE("TE-009", "Sleeping Chef", "The service you're trying to use is temporarily unavailable, usually due to maintenance or high system load. Please try again in a few minutes."),

    // Network/Connection errors
    NETWORK_ERROR("TE-010", "Disconnected Coffee", "We couldn't connect to one of our external services or databases. This might be due to network issues or the external service being down."),

    // Timeout errors
    TIMEOUT("TE-011", "Slow Snail", "The request took too long to process and timed out. This can happen when the system is busy or when processing complex operations. Please try again."),

    // Database errors
    DATABASE_ERROR("TE-012", "Confused Database", "There was a problem accessing or updating our database. This could be due to database maintenance, connection issues, or data corruption."),

    // External service errors
    EXTERNAL_SERVICE_ERROR("TE-013", "Grumpy API", "One of the external services we depend on (like the student information system or credential service) is not responding properly or returned an error."),

    // Configuration errors
    CONFIGURATION_ERROR("TE-014", "Missing Recipe", "There's a problem with our system configuration. This is an internal issue that prevents the system from working properly and needs to be fixed by our team."),

    // Data validation errors
    VALIDATION_ERROR("TE-015", "Spoiled Milk", "The data you provided doesn't meet our validation requirements. For example, an email address that's not properly formatted, or a date that's in the wrong format."),

    // File operation errors
    FILE_ERROR("TE-016", "Lost Recipe Book", "There was a problem reading, writing, or processing a file. This could be due to file permissions, disk space issues, or the file being corrupted."),

    // Security errors
    SECURITY_ERROR("TE-017", "Broken Lock", "A security-related operation failed, such as encryption, decryption, or token validation. This might indicate a security configuration issue or an attempt to access protected resources."),

    // Business logic errors
    BUSINESS_LOGIC_ERROR("TE-018", "Wrong Recipe", "The operation you're trying to perform doesn't make sense according to our business rules. For example, trying to enroll a student in a course that's already full or has prerequisites they haven't met."),

    // Resource exhausted errors
    RESOURCE_EXHAUSTED("TE-019", "Empty Pantry", "The system has run out of available resources (like memory, storage, or processing capacity) to handle your request. This is usually temporary and will resolve when system load decreases."),

    // Maintenance errors
    MAINTENANCE_ERROR("TE-020", "Kitchen Renovation", "The system is currently undergoing scheduled maintenance or updates. During this time, some features may be temporarily unavailable. Please check back later."),
    
    // Sis API Error Codes (mapped from external API)
    SIS_OK("SIS-000", "All Good", "Operation completed successfully in Sis system"),
    SIS_GENERIC_ERROR("SIS-001", "Mystery Box", "A generic error occurred in the Sis system"),
    SIS_AUTH_FAILURE("SIS-002", "Wrong Password", "Authentication failed - invalid credentials or session expired in Sis system"),
    SIS_IMEI_REGISTERED("SIS-003", "Déjà Vu Device", "Device IMEI already registered (legacy error code for backward compatibility)"),
    SIS_APP_NOT_REGISTERED("SIS-004", "Missing Membership", "Application is not registered for this user in Sis system"),
    SIS_OPERATION_UNAVAILABLE("SIS-005", "Access Denied", "This operation is not available for the user's profile or role in Sis system"),
    SIS_INCORRECT_FORMAT("SIS-006", "Garbled Message", "Request attribute has incorrect format or invalid data type for Sis API"),
    SIS_DATA_NOT_FOUND("SIS-007", "Empty Locker", "The requested data was not found in the Sis system"),
    SIS_INVALID_KEY("SIS-008", "Expired Ticket", "The installation key provided is invalid or expired for Sis system"),
    SIS_INVALID_REQUEST("SIS-009", "Broken Package", "The request is invalid or malformed for Sis API"),
    SIS_BUSINESS_RULE("SIS-010", "Rule Breaker", "Business rule validation failed in Sis system");

    private final String code;
    private final String funName;
    private final String description;

    ErrorCodes(String code, String funName, String description) {
        this.code = code;
        this.funName = funName;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getFunName() {
        return funName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Get error code by its string identifier
     * @param code the error code string (e.g., "TE-001")
     * @return the ErrorCodes enum value, or null if not found
     */
    public static ErrorCodes fromCode(String code) {
        for (ErrorCodes errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return null;
    }

    /**
     * Get error code by its fun name
     * @param funName the fun name (e.g., "Wired Nutella")
     * @return the ErrorCodes enum value, or null if not found
     */
    public static ErrorCodes fromFunName(String funName) {
        for (ErrorCodes errorCode : values()) {
            if (errorCode.funName.equals(funName)) {
                return errorCode;
            }
        }
        return null;
    }
}