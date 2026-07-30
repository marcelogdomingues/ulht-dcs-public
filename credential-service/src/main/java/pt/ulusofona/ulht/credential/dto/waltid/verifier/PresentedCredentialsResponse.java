package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Response containing presented credentials in decoded format.
 * Supports simple and verbose view modes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresentedCredentialsResponse {
    
    /**
     * View mode: "simple" or "verbose"
     */
    private String viewMode;
    
    /**
     * Credentials organized by format
     * Key: format (jwt_vc_json, sd_jwt_vc, mso_mdoc)
     * Value: array of decoded credentials
     */
    private Map<String, Object> credentialsByFormat;
}


