package pt.ulusofona.ulht.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Request to issue a W3C Verifiable Credential (JWT or SD-JWT)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Request to issue a W3C Verifiable Credential using OID4VCI protocol")
public class IssueCredentialRequest {
    
    @Schema(description = "The signing key (JWK or KMS reference)", required = true)
    private IssuerKey issuerKey;
    
    @Schema(description = "The DID of the issuer", required = true, example = "did:jwk:eyJrdHkiOiJPS1AiLCJjcnYiOiJFZDI1NTE5In0...")
    private String issuerDid;
    
    @Schema(description = "Credential configuration ID (e.g., 'UniversityDegree_jwt_vc_json' or 'UniversityDegree_vc+sd-jwt')", 
            required = true)
    private String credentialConfigurationId;
    
    @Schema(description = "The W3C credential data to sign", required = true)
    private Map<String, Object> credentialData;
    
    @Schema(description = "Optional mapping for dynamic value insertion using data functions")
    private CredentialMapping mapping;
    
    @Schema(description = "Authentication method for credential exchange", 
            example = "PRE_AUTHORIZED",
            allowableValues = {"PRE_AUTHORIZED", "ID_TOKEN", "VP_TOKEN", "PWD"})
    @Builder.Default
    private String authenticationMethod = "PRE_AUTHORIZED";
    
    @Schema(description = "OID4VCI standard version", 
            example = "DRAFT13",
            allowableValues = {"DRAFT13", "DRAFT11"})
    @Builder.Default
    private String standardVersion = "DRAFT13";
    
    @Schema(description = "Selective disclosure settings (for SD-JWT credentials)")
    private Map<String, Object> selectiveDisclosure;
}

