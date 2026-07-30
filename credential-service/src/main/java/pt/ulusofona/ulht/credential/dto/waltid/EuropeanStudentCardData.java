package pt.ulusofona.ulht.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * European Student Card (ESC) credential data
 * Follows European Student Card Initiative standards
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "European Student Card credential data")
public class EuropeanStudentCardData {
    
    @Schema(description = "Unique card identifier", example = "168446251")
    private String id;
    
    @Schema(description = "Student's given name", example = "Luísa")
    private String givenName;
    
    @Schema(description = "Student's family name", example = "Costa")
    private String familyName;
    
    @Schema(description = "Student's email address", 
            example = "luisa.costa@aluno.ulusofona.pt")
    private String email;
    
    @Schema(description = "European Student Identifier", example = "ESI-3618254416")
    private String esi;
    
    @Schema(description = "European Student Card Number", example = "511114706")
    private String escn;
    
    @Schema(description = "Institution PIC (Participant Identification Code)", 
            example = "997605425")
    private String institutionPIC;
    
    @Schema(description = "Academic level", 
            example = "bachelor",
            allowableValues = {"bachelor", "master", "doctorate", "short-cycle", "other"})
    private String academicLevel;
    
    @Schema(description = "Card type", 
            example = "Digital",
            allowableValues = {"Digital", "Physical", "Hybrid"})
    private String cardType;
    
    @Schema(description = "Institution name", example = "ULHT - Universidade Lusófona")
    private String institutionName;
    
    @Schema(description = "Valid from date (ISO 8601)", example = "2024-09-01")
    private String validFrom;
    
    @Schema(description = "Valid until date (ISO 8601)", example = "2025-08-31")
    private String validUntil;
}

