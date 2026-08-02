package com.example.dcs.credential.dto.waltid.verifier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolderInitiatedStartResponse {
    private String walletUrl;
    private String state;
    private String presentationId;
}
