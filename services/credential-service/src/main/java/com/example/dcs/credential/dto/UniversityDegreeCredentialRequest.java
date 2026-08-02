package com.example.dcs.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request for university degree credential issuance.
 * Simplified interface for issuing university degree credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "University degree credential request")
public class UniversityDegreeCredentialRequest {
    
    @Schema(description = "Student ID (used as subject identifier if subjectDid is not provided)")
    private String studentId;
    
    @Schema(description = "Degree name (e.g., 'Bachelor of Science and Arts')", 
            example = "Bachelor of Science and Arts")
    private String degreeName;
    
    @Schema(description = "Degree type (e.g., 'BachelorDegree', 'MasterDegree', 'DoctoralDegree')",
            example = "BachelorDegree")
    private String degreeType;
    
    @Schema(description = "Field of study (e.g., 'Computer Science')",
            example = "Computer Science")
    private String field;
    
    @Schema(description = "University/Institution name",
            example = "DCS - Example University")
    private String universityName;
    
    @Schema(description = "Graduation/Award date (ISO 8601 format)",
            example = "2024-06-15")
    private String graduationDate;
    
    @Schema(description = "Final grade or GPA",
            example = "18.5")
    private String grade;
    
    @Schema(description = "Subject DID (the student's DID who will receive the credential)",
            required = true,
            example = "did:jwk:eyJrdHkiOiJPS1AiLCJjcnYiOiJFZDI1NTE5In0...")
    private String subjectDid;
    
    @Schema(description = "Issuer key for signing the credential (JWK or KMS reference)",
            required = true,
            example = "{\"type\": \"jwk\", \"jwk\": {\"kty\": \"OKP\", \"crv\": \"Ed25519\", ...}}")
    private Object issuerKey;
    
    @Schema(description = "Issuer DID (must match the issuerKey)",
            required = true,
            example = "did:jwk:eyJrdHkiOiJPS1AiLCJjcnYiOiJFZDI1NTE5In0...")
    private String issuerDid;
    
    @Schema(description = "Legacy degree information field (deprecated, use degreeType and degreeName instead)")
    @Deprecated
    private Object degree;
}
