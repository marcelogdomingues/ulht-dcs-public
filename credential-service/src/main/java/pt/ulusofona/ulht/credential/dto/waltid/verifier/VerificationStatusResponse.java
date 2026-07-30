package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response containing the verification status and policy results.
 * Retrieved after the user presents credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationStatusResponse {
    
    /**
     * Unique identifier for this verification session
     */
    private String id;
    
    /**
     * Overall verification result
     * true if all policies passed, false otherwise
     */
    private Boolean verificationResult;
    
    /**
     * Presentation definition used for verification
     */
    private Map<String, Object> presentationDefinition;
    
    /**
     * Token response from the wallet
     */
    private Map<String, Object> tokenResponse;
    
    /**
     * Results of verification policies applied
     */
    private PolicyResults policyResults;
    
    /**
     * Custom parameters
     */
    private Map<String, Object> customParameters;
}


