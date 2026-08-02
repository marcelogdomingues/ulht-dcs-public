package com.example.dcs.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Results of verification policies applied to credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyResults {
    
    /**
     * List of policy results per credential
     */
    private List<CredentialPolicyResult> results;
    
    /**
     * Time taken to execute policies
     */
    private String time;
    
    /**
     * Number of policies executed
     */
    private Integer policiesRun;
}


