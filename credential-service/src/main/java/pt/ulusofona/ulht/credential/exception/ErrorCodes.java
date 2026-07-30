package pt.ulusofona.ulht.credential.exception;

/**
 * Credential Service Error Codes
 * 
 * This enum provides a centralized way to manage error codes for the Credential Service.
 * Each error code has:
 * - A unique identifier (e.g., "CRED-001")
 * - A fun name for easy reference
 * - A descriptive message explaining what went wrong
 * 
 * Usage:
 * - Use these codes in exception handling
 * - Reference them in API responses
 * - Use them for logging and monitoring
 */
public enum ErrorCodes {

    // External Service Errors (CRED-001 series)
    EXTERNAL_SERVICE_ERROR("CRED-001", "Grumpy External API", "An external service (like WaltID) is not responding properly or returned an error"),
    WALTID_BAD_REQUEST("CRED-WALTID-400", "WaltID Says No", "Bad request to WaltID service - check request format and parameters"),
    WALTID_UNAUTHORIZED("CRED-WALTID-401", "WaltID Auth Failed", "Unauthorized access to WaltID service - check API credentials"),
    WALTID_FORBIDDEN("CRED-WALTID-403", "WaltID Permission Denied", "Forbidden access to WaltID service - insufficient permissions"),
    WALTID_NOT_FOUND("CRED-WALTID-404", "WaltID Lost It", "WaltID service resource not found"),
    WALTID_SERVER_ERROR("CRED-WALTID-500", "WaltID Having Issues", "WaltID service internal error"),
    WALTID_UNAVAILABLE("CRED-WALTID-503", "WaltID Taking a Break", "WaltID service temporarily unavailable"),
    
    // Validation Errors (CRED-002 series)
    VALIDATION_ERROR("CRED-002", "Invalid Data", "Validation failed - the data provided doesn't meet our requirements"),
    
    // Bad Request Errors (CRED-003 series)
    BAD_REQUEST("CRED-003", "Broken Request", "The request is invalid, malformed, or missing required information"),
    ILLEGAL_ARGUMENT("CRED-003-ARG", "Wrong Argument", "Illegal argument provided to the operation"),
    
    // Credential Processing Errors (CRED-004 series)
    CREDENTIAL_CREATION_FAILED("CRED-004", "Credential Factory Jam", "Failed to create the credential"),
    CREDENTIAL_SIGNING_FAILED("CRED-005", "Signature Failed", "Failed to sign the credential"),
    CREDENTIAL_ISSUANCE_FAILED("CRED-006", "Issuance Blocked", "Failed to issue the credential"),
    CREDENTIAL_NOT_FOUND("CRED-007", "Missing Credential", "The requested credential was not found"),
    
    // Schema/Template Errors (CRED-010 series)
    SCHEMA_LOAD_ERROR("CRED-010", "Schema Missing", "Failed to load credential schema"),
    TEMPLATE_ERROR("CRED-011", "Template Broken", "Error processing credential template"),
    
    // DID/Key Management Errors (CRED-020 series)
    DID_RESOLUTION_FAILED("CRED-020", "DID Mystery", "Failed to resolve DID (Decentralized Identifier)"),
    KEY_GENERATION_FAILED("CRED-021", "Key Factory Broken", "Failed to generate cryptographic keys"),
    KEY_NOT_FOUND("CRED-022", "Lost Keys", "Cryptographic key not found"),
    
    // Data Mapping Errors (CRED-030 series)
    MAPPING_ERROR("CRED-030", "Data Confusion", "Error mapping data to credential format"),
    INVALID_CREDENTIAL_TYPE("CRED-031", "Unknown Type", "Invalid or unsupported credential type"),
    
    // Configuration Errors (CRED-040 series)
    CONFIGURATION_ERROR("CRED-040", "Misconfigured", "System configuration error"),
    MISSING_CONFIGURATION("CRED-041", "Config Missing", "Required configuration is missing"),
    
    // Generic/Unknown Errors (CRED-999 series)
    INTERNAL_SERVER_ERROR("CRED-999", "Something Exploded", "Internal server error - something unexpected happened"),
    UNKNOWN_ERROR("CRED-000", "Mystery Error", "An unknown error occurred");

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
     * @param code the error code string (e.g., "CRED-001")
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
     * @param funName the fun name (e.g., "Grumpy External API")
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
     * Check if error code represents a WaltID service error
     */
    public boolean isWaltIDError() {
        return code.startsWith("CRED-WALTID-");
    }
    
    /**
     * Check if error code represents a credential processing error
     */
    public boolean isCredentialError() {
        return code.startsWith("CRED-004") || code.startsWith("CRED-005") || 
               code.startsWith("CRED-006") || code.startsWith("CRED-007");
    }
}

