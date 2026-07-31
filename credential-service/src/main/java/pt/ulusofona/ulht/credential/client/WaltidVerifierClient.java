package pt.ulusofona.ulht.credential.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.ulht.credential.config.WaltidClientConfiguration;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.PresentedCredentialsResponse;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.VerificationRequest;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.VerificationStatusResponse;

import java.util.Map;

/**
 * WaltID Verifier Client for W3C Verifiable Credential verification.
 * Follows walt.id verifier API specification for OID4VP protocol.
 * 
 * API Reference: https://verifier.demo.walt.id/api-docs
 */
@FeignClient(
    name = "waltidVerifierClient",
    url = "${waltid.verifier.url}",
    configuration = WaltidClientConfiguration.class,
    primary = false
)
public interface WaltidVerifierClient {

    /**
     * Initiate W3C Verifiable Credential verification via OID4VP.
     * 
     * Endpoint: POST /openid4vc/verify
     * 
     * @param authorizeBaseUrl Base URL for OID4VP authorization (default: "openid4vp://authorize")
     * @param responseMode Response mode (must be "direct_post")
     * @param successRedirectUri Optional redirect URL on success (can use $id placeholder)
     * @param errorRedirectUri Optional redirect URL on error (can use $id placeholder)
     * @param statusCallbackUri Optional URL for status callbacks
     * @param statusCallbackApiKey Optional API key for callback authentication
     * @param stateId Optional custom state ID
     * @param openId4VPProfile Optional OID4VP profile (DEFAULT, EBSIV3, ISO_18013_7_MDOC)
     * @param request Verification request body
     * @return Response with verification URL
     */
    @PostMapping(
        value = "/openid4vc/verify",
        consumes = "application/json",
        produces = "*/*"
    )
    ResponseEntity<Map<String, Object>> initiateVerification(
        @RequestHeader(value = "authorizeBaseUrl", required = false, defaultValue = "openid4vp://authorize") 
        String authorizeBaseUrl,
        
        @RequestHeader(value = "responseMode", required = false, defaultValue = "direct_post") 
        String responseMode,
        
        @RequestHeader(value = "successRedirectUri", required = false) 
        String successRedirectUri,
        
        @RequestHeader(value = "errorRedirectUri", required = false) 
        String errorRedirectUri,
        
        @RequestHeader(value = "statusCallbackUri", required = false) 
        String statusCallbackUri,
        
        @RequestHeader(value = "statusCallbackApiKey", required = false) 
        String statusCallbackApiKey,
        
        @RequestHeader(value = "stateId", required = false) 
        String stateId,
        
        @RequestHeader(value = "openId4VPProfile", required = false) 
        String openId4VPProfile,
        
        @RequestBody VerificationRequest request
    );

    /**
     * Retrieve verification status and policy results.
     * 
     * Endpoint: GET /openid4vc/session/{state}
     * 
     * @param state State value from verification initiation
     * @return Verification status and policy results
     */
    @GetMapping(
        value = "/openid4vc/session/{state}",
        produces = "application/json"
    )
    ResponseEntity<VerificationStatusResponse> getVerificationStatus(
        @PathVariable("state") String state
    );

    /**
     * Inspect presented credentials in decoded format.
     * 
     * Endpoint: GET /openid4vc/session/{id}/presented-credentials
     * 
     * @param id Session identifier
     * @param viewMode View mode: "simple" or "verbose" (default: "simple")
     * @return Decoded presented credentials
     */
    @GetMapping(
        value = "/openid4vc/session/{id}/presented-credentials",
        produces = "application/json"
    )
    ResponseEntity<PresentedCredentialsResponse> getPresentedCredentials(
        @PathVariable("id") String id,
        @RequestParam(value = "viewMode", required = false, defaultValue = "simple") String viewMode
    );
}


