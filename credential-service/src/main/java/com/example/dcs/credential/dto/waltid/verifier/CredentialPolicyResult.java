package com.example.dcs.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Policy results for a specific credential.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialPolicyResult {
    
    /**
     * Type/name of the credential
     */
    private String credential;
    
    /**
     * Results of each policy applied to this credential
     */
    private List<PolicyResult> policyResults;
}


