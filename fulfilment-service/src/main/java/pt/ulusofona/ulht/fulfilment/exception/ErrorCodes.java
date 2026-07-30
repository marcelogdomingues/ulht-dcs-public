package pt.ulusofona.ulht.fulfilment.exception;

/**
 * Fulfilment Service Error Codes
 * 
 * This enum provides a centralized way to manage error codes for the Fulfilment Service.
 * Each error code has:
 * - A unique identifier (e.g., "FULF-001")
 * - A fun name for easy reference
 * - A descriptive message explaining what went wrong
 * 
 * Usage:
 * - Use these codes in exception handling
 * - Reference them in API responses
 * - Use them for logging and monitoring
 */
public enum ErrorCodes {

    // Workflow Errors (FULF-001 series)
    WORKFLOW_NOT_FOUND("FULF-001", "Lost Package", "The requested workflow could not be found"),
    WORKFLOW_CREATION_FAILED("FULF-002", "Shipping Label Failed", "Failed to create or start the workflow"),
    WORKFLOW_ALREADY_EXISTS("FULF-003", "Double Booking", "A workflow with this correlation ID already exists"),
    WORKFLOW_TIMEOUT("FULF-004", "Delivery Delayed", "The workflow exceeded the maximum execution time"),
    WORKFLOW_FAILED("FULF-005", "Package Damaged", "The workflow failed during execution"),
    
    // SSE Connection Errors (FULF-010 series)
    SSE_CONNECTION_FAILED("FULF-010", "Broken Phone Line", "Failed to establish Server-Sent Events connection"),
    SSE_SEND_FAILED("FULF-011", "Missed Call", "Failed to send event through SSE connection"),
    SSE_TIMEOUT("FULF-012", "Call Dropped", "SSE connection timed out"),
    
    // Kafka Event Errors (FULF-020 series)
    KAFKA_CONSUME_ERROR("FULF-020", "Undelivered Mail", "Error consuming Kafka event"),
    KAFKA_PRODUCE_ERROR("FULF-021", "Postage Denied", "Error producing Kafka event"),
    INVALID_EVENT_FORMAT("FULF-022", "Illegible Address", "Kafka event has invalid format or missing required fields"),
    
    // Data/Status Errors (FULF-030 series)
    INVALID_STATUS("FULF-030", "Wrong Tracking Code", "Invalid workflow status"),
    STATUS_UPDATE_FAILED("FULF-031", "Scanner Malfunction", "Failed to update workflow status"),
    DATA_NOT_FOUND("FULF-032", "Empty Box", "Required data not found for workflow"),
    
    // Validation Errors (FULF-040 series)
    VALIDATION_ERROR("FULF-040", "Incorrect Weight", "Validation failed for the provided data"),
    MISSING_CORRELATION_ID("FULF-041", "No Tracking Number", "Correlation ID is required but was not provided"),
    INVALID_CORRELATION_ID("FULF-042", "Invalid Barcode", "Correlation ID format is invalid"),
    
    // Configuration Errors (FULF-050 series)
    CONFIGURATION_ERROR("FULF-050", "Wrong Warehouse", "System configuration error"),
    MISSING_CONFIGURATION("FULF-051", "Missing Route Map", "Required configuration is missing"),
    
    // Cleanup Errors (FULF-060 series)
    CLEANUP_FAILED("FULF-060", "Trash Bin Full", "Failed to clean up expired workflow data"),
    
    // Generic Errors (FULF-999 series)
    INTERNAL_SERVER_ERROR("FULF-999", "Warehouse Collapsed", "Internal server error - something unexpected happened"),
    UNKNOWN_ERROR("FULF-000", "Mystery Parcel", "An unknown error occurred");

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
     * @param code the error code string (e.g., "FULF-001")
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
     * @param funName the fun name (e.g., "Lost Workflow")
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
     * Check if error code represents a workflow error
     */
    public boolean isWorkflowError() {
        return code.startsWith("FULF-00");
    }
    
    /**
     * Check if error code represents an SSE connection error
     */
    public boolean isSSEError() {
        return code.startsWith("FULF-01");
    }
    
    /**
     * Check if error code represents a Kafka error
     */
    public boolean isKafkaError() {
        return code.startsWith("FULF-02");
    }
}

