package com.example.dcs.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Result of a single policy verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PolicyResult {
    
    /**
     * Policy name (e.g., "signature", "expired", "webhook")
     */
    private String policy;
    
    /**
     * Policy description
     */
    private String description;
    
    /**
     * Whether the policy check succeeded
     */
    @JsonProperty("is_success")
    private Boolean isSuccess;
    
    /**
     * Detailed result data from the policy check
     */
    private Map<String, Object> result;
}


