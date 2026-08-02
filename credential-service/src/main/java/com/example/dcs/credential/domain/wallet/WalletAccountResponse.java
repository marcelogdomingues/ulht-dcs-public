package com.example.dcs.credential.domain.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * Wallet account response for credential service.
 * Independent model for the credential service.
 * 
 * Matches the actual WaltID Wallet API response structure:
 * {
 *   "account": "account-id",
 *   "wallets": [
 *     {
 *       "id": "wallet-id",
 *       "name": "Wallet name",
 *       "createdOn": "2025-11-04T23:40:49.773203Z",
 *       "addedOn": "2025-11-04T23:40:49.775467Z",
 *       "permission": "ADMINISTRATE"
 *     }
 *   ]
 * }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Wallet account response")
public class WalletAccountResponse {
    
    @JsonProperty("account")
    @Schema(description = "Account ID")
    private String account;
    
    @JsonProperty("wallets")
    @Schema(description = "List of wallets associated with the account")
    private List<WalletAccount> wallets;
    
    /**
     * Convenience method to get accounts (for backward compatibility)
     * Maps wallets to accounts
     */
    public List<WalletAccount> getAccounts() {
        return wallets;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Individual wallet account")
    public static class WalletAccount {
        
        @JsonProperty("id")
        @Schema(description = "Wallet ID")
        private String walletId;  // Mapped from "id" in JSON
        
        @JsonProperty("name")
        @Schema(description = "Wallet name")
        private String name;
        
        @JsonProperty("createdOn")
        @Schema(description = "Wallet creation timestamp")
        private String createdOn;
        
        @JsonProperty("addedOn")
        @Schema(description = "When wallet was added to account")
        private String addedOn;
        
        @JsonProperty("permission")
        @Schema(description = "Permission level (e.g., ADMINISTRATE)")
        private String permission;
        
        // Legacy fields for backward compatibility
        @Schema(description = "Wallet description (not provided by API)")
        private String description;
        
        @Schema(description = "Wallet type (not provided by API)")
        private String type;
        
        /**
         * Convenience method to get createdAt (maps from createdOn)
         */
        public String getCreatedAt() {
            return createdOn;
        }
        
        /**
         * Convenience method to get updatedAt (maps from addedOn)
         */
        public String getUpdatedAt() {
            return addedOn;
        }
    }
}
