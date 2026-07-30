package pt.ulusofona.ulht.credential.dto.waltid;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Dynamic value mapping for credential fields
 * Allows runtime insertion of values using data functions
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Mapping for dynamic credential field values using data functions like <uuid>, <timestamp>, etc.")
public class CredentialMapping {
    
    @Schema(description = "Credential ID mapping (e.g., '<uuid>')")
    private String id;
    
    @Schema(description = "Issuer mapping with nested properties")
    private Map<String, Object> issuer;
    
    @Schema(description = "Credential subject mapping with nested properties")
    private Map<String, Object> credentialSubject;
    
    @Schema(description = "Issuance date mapping (e.g., '<timestamp>')")
    private String issuanceDate;
    
    @Schema(description = "Expiration date mapping (e.g., '<timestamp-in:365d>')")
    private String expirationDate;
}

