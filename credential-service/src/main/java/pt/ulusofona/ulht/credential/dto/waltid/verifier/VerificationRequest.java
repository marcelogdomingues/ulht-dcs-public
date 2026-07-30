package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request body for initiating W3C Verifiable Credential verification via OID4VP.
 * Supports custom policies and presentation definitions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationRequest {
    
    /**
     * Policies applied to the Verifiable Presentation
     * Can be strings (e.g., "signature") or objects with policy and args
     */
    @JsonProperty("vp_policies")
    private List<Object> vpPolicies;
    
    /**
     * Policies applied to all requested Verifiable Credentials
     * Can be strings (e.g., "signature") or objects with policy and args
     */
    @JsonProperty("vc_policies")
    private List<Object> vcPolicies;
    
    /**
     * Array of credentials to request from the user
     */
    @JsonProperty("request_credentials")
    private List<RequestedCredential> requestCredentials;
}


