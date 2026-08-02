package com.example.dcs.credential.config;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Configuration template for a W3C Verifiable Credential type
 * Defines how to build and issue a specific credential type from student data
 */
@Data
public class CredentialTemplate {
    
    /**
     * Unique identifier for this credential template
     * Example: "educational-id", "european-student-card"
     */
    private String id;
    
    /**
     * The W3C credential type
     * Example: "EducationalID", "EuropeanStudentCard", "UniversityDegree"
     */
    private String type;
    
    /**
     * Human-readable display name
     * Example: "Educational ID (SCHAC)", "European Student Card"
     */
    private String displayName;
    
    /**
     * Whether this credential should be issued automatically
     */
    private boolean enabled = true;
    
    /**
     * WaltID credential configuration ID to use for issuance
     * Example: "UniversityDegree_jwt_vc_json", "BoardingPass_vc+sd-jwt"
     */
    private String waltidConfigId;
    
    /**
     * Credential format: "jwt_vc_json" or "vc+sd-jwt"
     */
    private String format = "jwt_vc_json";
    
    /**
     * Field mappings: Maps credential fields to student data fields
     * The value is a list of possible field names (tried in order)
     * 
     * Example:
     * {
     *   "studentId": ["studentId", "studentCode", "id"],
     *   "givenName": ["firstName", "givenName"],
     *   "email": ["email", "studentEmail"]
     * }
     */
    private Map<String, List<String>> fieldMappings;
    
    /**
     * Additional static fields to include in the credential
     * These are constant values that don't come from student data
     * 
     * Example:
     * {
     *   "institutionName": "DCS - Example University",
     *   "institutionPIC": "997605425"
     * }
     */
    private Map<String, Object> staticFields;
    
    /**
     * Conditional issuance rule (optional)
     * SpEL expression that evaluates to boolean
     * If present, credential is only issued if condition is true
     * 
     * Example: "#studentData['graduationDate'] != null"
     */
    private String condition;
    
    /**
     * Priority order for issuance (lower = issued first)
     * Default: 100
     */
    private int priority = 100;
    
    /**
     * JSON-LD context URLs for the credential
     * Example: ["https://www.w3.org/2018/credentials/v1"]
     */
    private List<String> contexts;
    
    /**
     * Additional credential types to include
     * Example: ["VerifiableCredential", "EducationalID"]
     */
    private List<String> additionalTypes;
    
    /**
     * Whether to fail the entire workflow if this credential fails
     * Default: false (continue with other credentials)
     */
    private boolean required = false;
}

