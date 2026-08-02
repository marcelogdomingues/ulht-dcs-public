package com.example.dcs.credential.domain.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Wallet keys response for credential service.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Wallet keys response")
public class WalletKeysResponse {
    
    @Schema(description = "List of wallet keys")
    private List<WalletKey> keys;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Individual wallet key")
    public static class WalletKey {
        
        @Schema(description = "Key ID")
        private String keyId;
        
        @Schema(description = "Key type")
        private String keyType;
        
        @Schema(description = "Public key")
        private String publicKey;
        
        @Schema(description = "Key algorithm")
        private String algorithm;
        
        @Schema(description = "Key purpose")
        private String purpose;
        
        @Schema(description = "Creation timestamp")
        private String createdAt;
        
        @Schema(description = "Key status")
        private String status;
    }
}
