package com.example.dcs.credential.dto.waltid.verifier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolderInitiatedStartRequest {
    @NotBlank
    private String credentialType;
    private String format;
    private List<Object> vpPolicies;
    private List<Object> vcPolicies;
}
