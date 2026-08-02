package com.example.dcs.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from onboarding an issuer
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Response containing generated issuer key and DID")
public class OnboardIssuerResponse {
    
    @Schema(description = "The generated issuer key for signing credentials")
    private IssuerKey issuerKey;
    
    @Schema(description = "The DID associated with the issuer key")
    private String issuerDid;
}

