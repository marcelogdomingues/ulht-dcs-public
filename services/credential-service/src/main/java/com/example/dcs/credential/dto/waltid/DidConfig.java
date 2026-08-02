package com.example.dcs.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Configuration for DID creation in walt.id
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DidConfig {
    /**
     * DID method to use
     * Options: "key", "jwk", "web", "cheqd"
     */
    private String method;
}

