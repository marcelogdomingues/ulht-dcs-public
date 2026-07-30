package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a requested credential in a verification request.
 * Can be a simple credential type or include custom input_descriptor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestedCredential {
    
    /**
     * Type of credential to request (e.g., "VerifiableDiploma", "OpenBadgeCredential")
     */
    private String type;
    
    /**
     * Format of the credential (e.g., "jwt_vc_json", "sd_jwt_vc", "mso_mdoc")
     */
    private String format;
    
    /**
     * Optional: Policies to apply to this specific credential
     * Can be a list of strings or objects with policy and args
     */
    private List<Object> policies;
    
    /**
     * Optional: Custom input descriptor for presentation definition
     * Used for advanced verification requirements
     */
    @JsonProperty("input_descriptor")
    private InputDescriptor inputDescriptor;
}

