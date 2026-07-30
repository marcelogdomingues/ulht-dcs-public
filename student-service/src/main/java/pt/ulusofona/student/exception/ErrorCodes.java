package pt.ulusofona.student.exception;

/**
 * Student Service Error Codes
 * 
 * This enum provides a centralized way to manage error codes for the Student Service.
 * Each error code has:
 * - A unique identifier (e.g., "STUD-001")
 * - A fun name for easy reference
 * - A descriptive message explaining what went wrong
 * 
 * Usage:
 * - Use these codes in exception handling
 * - Reference them in API responses
 * - Use them for logging and monitoring
 */
public enum ErrorCodes {

    // Request Processing Errors (STUD-001 series)
    INVALID_REQUEST("STUD-001", "Confused Student", "The request is invalid or malformed"),
    MISSING_REQUIRED_FIELD("STUD-002", "Incomplete Homework", "A required field is missing from the request"),
    INVALID_FIELD_FORMAT("STUD-003", "Messy Handwriting", "A field has an invalid format"),
    
    // Validation Errors (STUD-010 series)
    VALIDATION_ERROR("STUD-010", "Failed the Test", "Validation failed for the provided data"),
    INVALID_USERNAME("STUD-011", "Wrong Name Tag", "Username format is invalid"),
    INVALID_PASSWORD("STUD-012", "Forgot Locker Code", "Password format is invalid"),
    INVALID_STUDENT_CODE("STUD-013", "Invalid ID Card", "Student code format is invalid"),
    
    // Kafka Errors (STUD-020 series)
    KAFKA_PRODUCE_ERROR("STUD-020", "Lost in the Mail", "Failed to publish message to Kafka"),
    KAFKA_CONNECTION_ERROR("STUD-021", "Post Office Closed", "Failed to connect to Kafka broker"),
    
    // Correlation ID Errors (STUD-030 series)
    CORRELATION_ID_GENERATION_FAILED("STUD-030", "Lost Ticket Number", "Failed to generate correlation ID"),
    INVALID_CORRELATION_ID("STUD-031", "Unreadable Ticket", "Correlation ID format is invalid"),
    
    // External Service Errors (STUD-040 series)
    FULFILMENT_SERVICE_ERROR("STUD-040", "Graduation Delayed", "Fulfilment service is unavailable or returned an error"),
    FULFILMENT_NOT_FOUND("STUD-041", "Diploma Missing", "Workflow status not found in fulfilment service"),
    FULFILMENT_TIMEOUT("STUD-042", "Ceremony Running Late", "Request to fulfilment service timed out"),
    
    // Workflow Status Errors (STUD-050 series)
    WORKFLOW_NOT_FOUND("STUD-050", "Lost Enrollment Form", "No workflow found for the provided correlation ID"),
    WORKFLOW_STATUS_ERROR("STUD-051", "Grade Book Error", "Error retrieving workflow status"),
    
    // Credential Errors (STUD-060 series)
    CREDENTIALS_NOT_READY("STUD-060", "Diploma in Progress", "Credentials are not ready yet, still processing"),
    CREDENTIALS_NOT_FOUND("STUD-061", "Empty Certificate Holder", "No credentials found for the provided correlation ID"),
    CREDENTIALS_FETCH_ERROR("STUD-062", "Printer Jam", "Error retrieving credentials"),
    
    // Configuration Errors (STUD-070 series)
    CONFIGURATION_ERROR("STUD-070", "Wrong Classroom", "System configuration error"),
    MISSING_CONFIGURATION("STUD-071", "No Syllabus", "Required configuration is missing"),
    
    // Generic Errors (STUD-999 series)
    INTERNAL_SERVER_ERROR("STUD-999", "School System Crashed", "Internal server error - something unexpected happened"),
    UNKNOWN_ERROR("STUD-000", "Mystery Assignment", "An unknown error occurred");

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
     * @param code the error code string (e.g., "STUD-001")
     * @return the ErrorCodes enum value, or UNKNOWN_ERROR if not found
     */
    public static ErrorCodes fromCode(String code) {
        if (code == null) {
            return UNKNOWN_ERROR;
        }
        
        for (ErrorCodes errorCode : values()) {
            if (errorCode.code.equals(code)) {
                return errorCode;
            }
        }
        return UNKNOWN_ERROR;
    }

    /**
     * Get error code by its fun name
     * @param funName the fun name (e.g., "Bad Request")
     * @return the ErrorCodes enum value, or null if not found
     */
    public static ErrorCodes fromFunName(String funName) {
        if (funName == null) {
            return null;
        }
        
        for (ErrorCodes errorCode : values()) {
            if (errorCode.funName.equals(funName)) {
                return errorCode;
            }
        }
        return null;
    }
    
    /**
     * Check if error code represents a validation error
     */
    public boolean isValidationError() {
        return code.startsWith("STUD-01");
    }
    
    /**
     * Check if error code represents a Kafka error
     */
    public boolean isKafkaError() {
        return code.startsWith("STUD-02");
    }
    
    /**
     * Check if error code represents an external service error
     */
    public boolean isExternalServiceError() {
        return code.startsWith("STUD-04");
    }
}

