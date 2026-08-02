package com.example.dcs.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Issuer key wrapper for credential signing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IssuerKey {
    /**
     * Type of key reference
     * "jwk" for local JWK, or KMS provider name
     */
    private String type;
    
    /**
     * The actual JWK (when type is "jwk")
     */
    private JwkKey jwk;
    
    /**
     * Reference to external KMS (when not using JWK)
     */
    private Object kmsRef;
}

