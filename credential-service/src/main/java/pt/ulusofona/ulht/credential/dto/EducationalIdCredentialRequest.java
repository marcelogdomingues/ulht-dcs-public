package pt.ulusofona.ulht.credential.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Request for Educational ID credential issuance
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Educational ID credential request based on SCHAC schema")
public class EducationalIdCredentialRequest {
    
    // Identity fields
    @Schema(description = "Student identifier", required = true, example = "511114706")
    private String identifier;
    
    @Schema(description = "Family name", required = true, example = "Costa")
    private String familyName;
    
    @Schema(description = "First name", required = true, example = "Luísa")
    private String firstName;
    
    @Schema(description = "Email address", required = true, 
            example = "luisa.costa@alunos.ulusofona.pt")
    private String mail;
    
    @Schema(description = "Date of birth (ISO 8601)", example = "1970-01-01")
    private String dateOfBirth;
    
    // Educational fields
    @Schema(description = "Home organization domain", 
            required = true, example = "ulusofona.pt")
    private String schacHomeOrganization;
    
    @Schema(description = "Primary affiliation", 
            required = true, 
            example = "student",
            allowableValues = {"student", "faculty", "staff", "employee", "member"})
    private String eduPersonPrimaryAffiliation;
    
    @Schema(description = "List of affiliations", example = "[\"student\"]")
    private List<String> eduPersonAffiliation;
    
    @Schema(description = "Assurance levels", 
            example = "[\"https://refeds.org/assurance/IAP/low\"]")
    private List<String> eduPersonAssurance;
    
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
