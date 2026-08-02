package com.example.dcs.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Educational ID credential data based on SCHAC (Schema for Academia)
 * Follows European educational identity standards
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Educational ID based on SCHAC schema")
public class EducationalIdData {
    
    @Schema(description = "Unique identifier", example = "511114706")
    private String id;
    
    @Schema(description = "Primary identifier", example = "511114706")
    private String identifier;
    
    @Schema(description = "SCHAC Personal Unique Codes", 
            example = "[\"urn:schac:personalUniqueCode:int:esi:urn:esid:usis.pt:511114706\"]")
    private List<String> schacPersonalUniqueCode;
    
    @Schema(description = "SCHAC Personal Unique ID", 
            example = "urn:schac:personalUniqueID:PT:usis.pt:511114706")
    private String schacPersonalUniqueID;
    
    @Schema(description = "SCHAC Home Organization", example = "usis.pt")
    private String schacHomeOrganization;
    
    @Schema(description = "Family name", example = "Costa")
    private String familyName;
    
    @Schema(description = "First name", example = "Luísa")
    private String firstName;
    
    @Schema(description = "Display name", example = "Luísa Costa")
    private String displayName;
    
    @Schema(description = "Date of birth (ISO 8601)", example = "1970-01-01")
    private String dateOfBirth;
    
    @Schema(description = "Common name", example = "Luísa Costa")
    private String commonName;
    
    @Schema(description = "Email address", example = "luisa.costa@alunos.usis.pt")
    private String mail;
    
    @Schema(description = "eduPerson Principal Name", 
            example = "luisa.costa@alunos.usis.pt")
    private String eduPersonPrincipalName;
    
    @Schema(description = "eduPerson Primary Affiliation", example = "student")
    private String eduPersonPrimaryAffiliation;
    
    @Schema(description = "eduPerson Affiliations", example = "[\"student\"]")
    private List<String> eduPersonAffiliation;
    
    @Schema(description = "eduPerson Scoped Affiliations", 
            example = "[\"student@usis.pt\"]")
    private List<String> eduPersonScopedAffiliation;
    
    @Schema(description = "eduPerson Assurance levels", 
            example = "[\"https://refeds.org/assurance/IAP/low\"]")
    private List<String> eduPersonAssurance;
}
