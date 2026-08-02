package com.example.dcs.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to onboard an issuer and create signing keys + DID
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to create a signing key and DID for credential issuance")
public class OnboardIssuerRequest {
    
    @Schema(description = "Key configuration for signing credentials", required = true)
    private KeyConfig key;
    
    @Schema(description = "DID configuration", required = true)
    private DidConfig did;
}

