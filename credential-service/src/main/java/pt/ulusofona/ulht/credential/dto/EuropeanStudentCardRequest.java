package pt.ulusofona.ulht.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Request for European Student Card credential issuance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "European Student Card credential request")
public class EuropeanStudentCardRequest {
    
    // Student information
    @Schema(description = "Given name", required = true, example = "Luísa")
    private String givenName;
    
    @Schema(description = "Family name", required = true, example = "Costa")
    private String familyName;
    
    @Schema(description = "Email address", required = true, 
            example = "luisa.costa@aluno.ulusofona.pt")
    private String email;
    
    // ESC specific fields
    @Schema(description = "European Student Identifier", 
            required = true, example = "ESI-3618254416")
    private String esi;
    
    @Schema(description = "European Student Card Number", 
            required = true, example = "511114706")
    private String escn;
    
    @Schema(description = "Institution PIC", required = true, example = "997605425")
    private String institutionPIC;
    
    @Schema(description = "Institution name", example = "ULHT - Universidade Lusófona")
    private String institutionName;
    
    @Schema(description = "Academic level", 
            required = true,
            example = "bachelor",
            allowableValues = {"bachelor", "master", "doctorate", "short-cycle", "other"})
    private String academicLevel;
    
    @Schema(description = "Card type", 
            example = "Digital",
            allowableValues = {"Digital", "Physical", "Hybrid"})
    private String cardType;
    
    @Schema(description = "Valid from date (ISO 8601)", example = "2024-09-01")
    private String validFrom;
    
    @Schema(description = "Valid until date (ISO 8601)", example = "2025-08-31")
    private String validUntil;
    
    // Credential issuance fields
    @Schema(description = "Subject DID (student's DID)", 
            required = true, 
            example = "did:jwk:student123")
    private String subjectDid;
    
    @Schema(description = "Issuer key for signing", required = true)
    private Object issuerKey;
    
    @Schema(description = "Issuer DID", required = true, 
            example = "did:jwk:issuer456")
    private String issuerDid;
}
