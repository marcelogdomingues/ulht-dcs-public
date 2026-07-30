package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Custom input descriptor for presentation definition.
 * Allows specifying custom constraints and fields for credential verification.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InputDescriptor {
    
    /**
     * Unique identifier for this input descriptor
     */
    private String id;
    
    /**
     * Format constraints (e.g., {"jwt_vc_json": {"alg": ["EdDSA"]}})
     */
    private Map<String, Object> format;
    
    /**
     * Constraints for the credential
     */
    private Constraints constraints;
}


