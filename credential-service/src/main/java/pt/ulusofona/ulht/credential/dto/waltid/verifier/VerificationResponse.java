package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response from verification initiation.
 * Contains the URL for the wallet to fulfill the verification request.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VerificationResponse {
    
    /**
     * URL for wallet to fulfill verification request
     * Can be displayed as QR code or shared directly
     */
    private String url;
    
    /**
     * Presentation definition ID
     */
    private String presentationId;
    
    /**
     * State value for tracking the verification session
     */
    private String state;
}


