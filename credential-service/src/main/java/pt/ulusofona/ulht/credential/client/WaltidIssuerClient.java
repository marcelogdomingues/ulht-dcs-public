package pt.ulusofona.ulht.credential.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import pt.ulusofona.ulht.credential.config.WaltidClientConfiguration;
import pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerRequest;
import pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse;
import pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest;

import java.util.Map;

/**
 * WaltID Issuer Client for credential service.
 * Follows walt.id issuer API specification for W3C Verifiable Credentials.
 * Supports OID4VCI protocol for credential issuance.
 */
@FeignClient(
    name = "waltidIssuerClient",
    url = "${waltid.issuer.url}",
    configuration = WaltidClientConfiguration.class
)
public interface WaltidIssuerClient {

    /**
     * Onboard an issuer by creating a signing key and DID
     * Endpoint: POST /onboard/issuer
     */
    @PostMapping(
        value = "/onboard/issuer",
        consumes = "application/json",
        produces = "application/json"
    )
    ResponseEntity<OnboardIssuerResponse> onboardIssuer(@RequestBody OnboardIssuerRequest request);

    /**
     * Issue a JWT W3C Verifiable Credential
     * Endpoint: POST /openid4vc/jwt/issue
     * Returns an OID4VCI credential offer URL
     */
    @PostMapping(
        value = "/openid4vc/jwt/issue",
        consumes = "application/json",
        produces = "text/plain"
    )
    ResponseEntity<String> issueJwtCredential(
        @RequestBody IssueCredentialRequest request,
        @RequestHeader(value = "statusCallbackUri", required = false) String statusCallbackUri
    );

    /**
     * Issue an SD-JWT (Selective Disclosure JWT) W3C Verifiable Credential
     * Endpoint: POST /openid4vc/sdjwt/issue
     * Returns an OID4VCI credential offer URL
     */
    @PostMapping(
        value = "/openid4vc/sdjwt/issue",
        consumes = "application/json",
        produces = "text/plain"
    )
    ResponseEntity<String> issueSdJwtCredential(
        @RequestBody IssueCredentialRequest request,
        @RequestHeader(value = "statusCallbackUri", required = false) String statusCallbackUri
    );

    /**
     * Get issuer metadata (OpenID4VCI)
     * Endpoint: GET /.well-known/openid-credential-issuer
     */
    @GetMapping(value = "/.well-known/openid-credential-issuer")
    ResponseEntity<Map<String, Object>> getIssuerMetadata();

    /**
     * Legacy endpoint - kept for backward compatibility
     */
    @PostMapping(
        value = "/onboard",
        consumes = "application/json",
        produces = "application/json"
    )
    @Deprecated
    ResponseEntity<String> onboardUser(@RequestBody Map<String, Object> requestBody);

    /**
     * Legacy endpoint - kept for backward compatibility
     */
    @PostMapping(
        value = "/issue",
        consumes = "application/json",
        produces = "application/json"
    )
    @Deprecated
    ResponseEntity<String> issueCredential(@RequestBody Map<String, Object> requestBody);

    /**
     * Legacy endpoint - kept for backward compatibility
     */
    @GetMapping(value = "/status/{userId}")
    @Deprecated
    ResponseEntity<String> getIssuerStatus(@PathVariable("userId") String userId);
}
