package pt.ulusofona.ulht.credential.dto.waltid.verifier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Header parameters for verification requests.
 * Encapsulates all optional configuration headers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationHeaders {
    
    /**
     * Base URL for OID4VP authorization
     * Default: "openid4vp://authorize"
     * For same-device flow: wallet-specific URL
     */
    @Builder.Default
    private String authorizeBaseUrl = "openid4vp://authorize";
    
    /**
     * Response mode for the verification
     * Currently only "direct_post" is supported
     */
    @Builder.Default
    private String responseMode = "direct_post";
    
    /**
     * URL to redirect on successful verification
     * Can use $id placeholder (e.g., "https://example.com/success?id=$id")
     */
    private String successRedirectUri;
    
    /**
     * URL to redirect on failed verification
     * Can use $id placeholder (e.g., "https://example.com/error?id=$id")
     */
    private String errorRedirectUri;
    
    /**
     * URL to receive status callbacks when presentation is fulfilled
     * Receives POST with full presentation result
     */
    private String statusCallbackUri;
    
    /**
     * API key for status callback endpoint authentication
     * Appended as Authorization header
     */
    private String statusCallbackApiKey;
    
    /**
     * Custom state ID to override auto-generated state value
     */
    private String stateId;
    
    /**
     * OpenID4VP profile to use
     * Options: DEFAULT, EBSIV3, ISO_18013_7_MDOC
     */
    private String openId4VPProfile;
}


