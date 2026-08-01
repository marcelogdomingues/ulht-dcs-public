package pt.ulusofona.ulht.credential.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pt.ulusofona.ulht.credential.client.WaltidVerifierClient;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.*;
import pt.ulusofona.ulht.credential.exception.ExternalServiceException;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Service for W3C Verifiable Credential verification using walt.id OID4VP protocol.
 * 
 * This service provides methods to:
 * - Initiate verification requests
 * - Check verification status
 * - Inspect presented credentials
 * 
 * Supports various verification scenarios:
 * - Basic verification with default signature policy
 * - Custom VP/VC policies
 * - Per-credential policies
 * - Custom presentation definitions with relational constraints
 */
@Slf4j
@Service
public class VerifierService {

    private final WaltidVerifierClient verifierClient;

    public VerifierService(WaltidVerifierClient verifierClient) {
        this.verifierClient = verifierClient;
    }

    /**
     * Executes an operation and handles errors appropriately.
     */
    private <T> T executeOperation(final Supplier<T> operation, final String operationName)
            throws ExternalServiceException {
        try {
            return operation.get();
        } catch (Exception e) {
            log.error("Error executing {} operation: {}", operationName, e.getMessage(), e);
            throw new ExternalServiceException(
                String.format("Error during %s: %s", operationName, e.getMessage()),
                "WaltID Verifier"
            );
        }
    }

    /**
     * Initiates a W3C Verifiable Credential verification request.
     * 
     * Example 1 - Basic verification:
     * <pre>
     * VerificationRequest request = VerificationRequest.builder()
     *     .requestCredentials(List.of(
     *         RequestedCredential.builder()
     *             .type("VerifiableDiploma")
     *             .format("jwt_vc_json")
     *             .build()
     *     ))
     *     .build();
     * 
     * VerificationHeaders headers = VerificationHeaders.builder()
     *     .successRedirectUri("https://example.com/success?id=$id")
     *     .errorRedirectUri("https://example.com/error?id=$id")
     *     .build();
     * 
     * VerificationResponse response = verifierService.initiateVerification(request, headers);
     * </pre>
     * 
     * Example 2 - With custom policies:
     * <pre>
     * VerificationRequest request = VerificationRequest.builder()
     *     .vpPolicies(List.of("signature", "expired", "not-before"))
     *     .vcPolicies(List.of("signature", "expired"))
     *     .requestCredentials(List.of(
     *         RequestedCredential.builder()
     *             .type("VerifiableDiploma")
     *             .format("jwt_vc_json")
     *             .build()
     *     ))
     *     .build();
     * </pre>
     * 
     * @param request Verification request with credentials to verify
     * @param headers Optional headers for configuration (redirects, callbacks, etc.)
     * @return Verification response with URL for wallet
     * @throws ExternalServiceException if verification initiation fails
     */
    @CircuitBreaker(name = "waltid", fallbackMethod = "initiateVerificationFallback")
    @Retry(name = "waltid")
    public VerificationResponse initiateVerification(
            VerificationRequest request,
            VerificationHeaders headers) throws ExternalServiceException {

        log.info("Initiating verification request for {} credentials",
                request.getRequestCredentials() != null ? request.getRequestCredentials().size() : 0);

        return executeOperation(() -> {
            // Set default headers if not provided
            final VerificationHeaders finalHeaders = headers != null ? headers : VerificationHeaders.builder().build();

            ResponseEntity<Map<String, Object>> response = verifierClient.initiateVerification(
                finalHeaders.getAuthorizeBaseUrl(),
                finalHeaders.getResponseMode(),
                finalHeaders.getSuccessRedirectUri(),
                finalHeaders.getErrorRedirectUri(),
                finalHeaders.getStatusCallbackUri(),
                finalHeaders.getStatusCallbackApiKey(),
                finalHeaders.getStateId(),
                finalHeaders.getOpenId4VPProfile(),
                request
            );

            Object responseBody = response.getBody();
            if (responseBody == null) {
                throw new RuntimeException("Empty response from verifier service");
            }

            log.info("Verification initiated successfully");
            
            // Handle both plain text URL response and JSON response
            String verificationUrl;
            String state = null;
            String presentationId = null;
            
            if (responseBody instanceof String) {
                // walt.id returns plain text URL
                verificationUrl = (String) responseBody;
                log.debug("Received plain text verification URL: {}", verificationUrl);
                
                // Extract state parameter from URL
                state = extractStateFromUrl(verificationUrl);
                log.debug("Extracted state from URL: {}", state);
            } else if (responseBody instanceof Map) {
                // JSON response
                @SuppressWarnings("unchecked")
                Map<String, Object> jsonBody = (Map<String, Object>) responseBody;
                verificationUrl = (String) jsonBody.get("url");
                presentationId = (String) jsonBody.get("presentationId");
                state = (String) jsonBody.get("state");
            } else {
                throw new RuntimeException("Unexpected response type: " + responseBody.getClass().getName());
            }
            
            return VerificationResponse.builder()
                .url(verificationUrl)
                .presentationId(presentationId)
                .state(state)
                .build();
                
        }, "verification initiation");
    }

    /**
     * Retrieves the status and results of a verification session.
     * 
     * Example:
     * <pre>
     * String state = "a07bdb17-7d87-4965-9296-1adefcaaddd9";
     * VerificationStatusResponse status = verifierService.getVerificationStatus(state);
     * 
     * if (status.getVerificationResult()) {
     *     // All policies passed
     *     log.info("Verification successful!");
     * } else {
     *     // Some policies failed
     *     log.warn("Verification failed");
     * }
     * </pre>
     * 
     * @param state State value from verification initiation
     * @return Verification status with policy results
     * @throws ExternalServiceException if status retrieval fails
     */
    @CircuitBreaker(name = "waltid", fallbackMethod = "getVerificationStatusFallback")
    @Retry(name = "waltid")
    public VerificationStatusResponse getVerificationStatus(String state) throws ExternalServiceException {
        log.info("Retrieving verification status for state: {}", state);

        return executeOperation(() -> {
            ResponseEntity<VerificationStatusResponse> response = 
                verifierClient.getVerificationStatus(state);

            VerificationStatusResponse status = response.getBody();
            if (status == null) {
                throw new RuntimeException("Empty response when retrieving verification status");
            }

            log.info("Verification status retrieved: {}", status.getVerificationResult());
            return status;
            
        }, "verification status retrieval");
    }

    /**
     * Inspects the credentials presented during a verification session.
     * 
     * Example - Simple view:
     * <pre>
     * String sessionId = "X31C7TERsFzR";
     * PresentedCredentialsResponse credentials = 
     *     verifierService.getPresentedCredentials(sessionId, "simple");
     * </pre>
     * 
     * Example - Verbose view:
     * <pre>
     * PresentedCredentialsResponse credentials = 
     *     verifierService.getPresentedCredentials(sessionId, "verbose");
     * // Includes raw JWTs, headers, payloads, and disclosures
     * </pre>
     * 
     * @param sessionId Session identifier
     * @param viewMode View mode: "simple" or "verbose" (default: "simple")
     * @return Decoded presented credentials
     * @throws ExternalServiceException if credential retrieval fails
     */
    @CircuitBreaker(name = "waltid", fallbackMethod = "getPresentedCredentialsFallback")
    @Retry(name = "waltid")
    public PresentedCredentialsResponse getPresentedCredentials(String sessionId, String viewMode) throws ExternalServiceException {
        log.info("Retrieving presented credentials for session: {} (viewMode: {})",
                sessionId, viewMode);

        return executeOperation(() -> {
            ResponseEntity<PresentedCredentialsResponse> response = 
                verifierClient.getPresentedCredentials(sessionId, viewMode);

            PresentedCredentialsResponse credentials = response.getBody();
            if (credentials == null) {
                throw new RuntimeException("Empty response when retrieving presented credentials");
            }

            log.info("Presented credentials retrieved successfully");
            return credentials;
            
        }, "presented credentials retrieval");
    }

    /**
     * Helper method to initiate basic verification with minimal configuration.
     * Uses default policies (signature only) and no redirects.
     * 
     * Example:
     * <pre>
     * VerificationResponse response = verifierService.initiateBasicVerification(
     *     "VerifiableDiploma", 
     *     "jwt_vc_json"
     * );
     * </pre>
     * 
     * @param credentialType Type of credential to verify
     * @param format Credential format (jwt_vc_json, sd_jwt_vc, mso_mdoc)
     * @return Verification response with URL
     * @throws ExternalServiceException if verification initiation fails
     */
    public VerificationResponse initiateBasicVerification(String credentialType, String format) throws ExternalServiceException {
        log.info("Initiating basic verification for credential type: {}, format: {}", 
                credentialType, format);

        RequestedCredential credential = RequestedCredential.builder()
            .type(credentialType)
            .format(format)
            .build();

        VerificationRequest request = VerificationRequest.builder()
            .requestCredentials(java.util.List.of(credential))
            .build();

        return initiateVerification(request, null);
    }

    /**
     * Helper method to check if a verification session has completed successfully.
     * 
     * @param state State value from verification initiation
     * @return true if verification passed all policies, false otherwise
     */
    public boolean isVerificationSuccessful(String state) {
        try {
            VerificationStatusResponse status = getVerificationStatus(state);
            return status.getVerificationResult() != null && status.getVerificationResult();
        } catch (Exception e) {
            log.error("Error checking verification status: {}", e.getMessage());
            return false;
        }
    }
    
    // -------------------------------------------------------------------------
    // Resilience4j fallbacks for walt.id verifier calls.
    //
    // Invoked when the circuit breaker is OPEN or retries are exhausted for a
    // walt.id outage. They translate the failure into the project's graceful
    // ExternalServiceException (preserving an existing ExternalServiceException
    // cause) rather than surfacing a raw exception. The DEMO profile mocks always
    // succeed, so fallbacks never trigger there and the happy path is unchanged.
    // -------------------------------------------------------------------------

    @SuppressWarnings("unused")
    private VerificationResponse initiateVerificationFallback(
            VerificationRequest request, VerificationHeaders headers, Throwable t)
            throws ExternalServiceException {
        throw toExternalServiceException("verification initiation", t);
    }

    @SuppressWarnings("unused")
    private VerificationStatusResponse getVerificationStatusFallback(String state, Throwable t)
            throws ExternalServiceException {
        throw toExternalServiceException("verification status retrieval", t);
    }

    @SuppressWarnings("unused")
    private PresentedCredentialsResponse getPresentedCredentialsFallback(
            String sessionId, String viewMode, Throwable t) throws ExternalServiceException {
        throw toExternalServiceException("presented credentials retrieval", t);
    }

    /**
     * Translates a resilience4j failure into the project's graceful
     * {@link ExternalServiceException}, preserving an existing cause when present.
     */
    private ExternalServiceException toExternalServiceException(String operationName, Throwable t) {
        log.error("walt.id verifier resilience fallback triggered for {}: {}", operationName,
                t != null ? t.getMessage() : "unknown", t);
        Throwable cause = t;
        while (cause != null) {
            if (cause instanceof ExternalServiceException) {
                return (ExternalServiceException) cause;
            }
            cause = cause.getCause();
        }
        String reason = (t != null && t.getMessage() != null) ? t.getMessage()
                : (t != null ? t.getClass().getSimpleName() : "walt.id unavailable");
        return new ExternalServiceException(
                String.format("walt.id verifier is currently unavailable during %s: %s",
                        operationName, reason),
                "WaltID Verifier");
    }

    /**
     * Extracts the state parameter from a verification URL.
     *
     * @param url Verification URL
     * @return State value, or null if not found
     */
    private String extractStateFromUrl(String url) {
        if (url == null || !url.contains("state=")) {
            return null;
        }
        
        try {
            // Extract state parameter from URL
            int stateStart = url.indexOf("state=") + 6;
            int stateEnd = url.indexOf("&", stateStart);
            
            if (stateEnd == -1) {
                // state is the last parameter
                return url.substring(stateStart);
            } else {
                return url.substring(stateStart, stateEnd);
            }
        } catch (Exception e) {
            log.error("Error extracting state from URL: {}", e.getMessage());
            return null;
        }
    }
}

