package pt.ulusofona.ulht.credential.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.*;
import pt.ulusofona.ulht.credential.exception.ErrorResponse;
import pt.ulusofona.ulht.credential.service.VerifierService;

import jakarta.validation.Valid;

/**
 * REST Controller for W3C Verifiable Credential verification via OID4VP.
 * 
 * Provides endpoints to:
 * - Initiate verification requests
 * - Check verification status
 * - Inspect presented credentials
 * 
 * Based on walt.id verifier API specification.
 */
@Slf4j
@RestController
@RequestMapping("/verifier")
@Tag(
    name = "Verifier", 
    description = "W3C Verifiable Credential verification using OID4VP protocol"
)
public class VerifierController {

    private final VerifierService verifierService;

    public VerifierController(VerifierService verifierService) {
        this.verifierService = verifierService;
    }

    @Operation(
        summary = "Initiate credential verification",
        description = """
            Initiates a W3C Verifiable Credential verification request via OID4VP protocol.
            
            Returns a URL that can be:
            - Displayed as a QR code for cross-device flow
            - Shared directly with a wallet for same-device flow
            
            Supports:
            - Basic verification with default signature policy
            - Custom VP/VC policies
            - Per-credential policies
            - Custom presentation definitions with relational constraints
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Verification initiated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VerificationResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "url": "openid4vp://authorize?response_type=vp_token&...",
                          "presentationId": "HbIdo1BMxEwf",
                          "state": "X31C7TERsFzR"
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid verification request",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> initiateVerification(
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Verification request with credentials to verify and optional policies",
            required = true,
            content = @Content(
                examples = {
                    @ExampleObject(
                        name = "Basic Verification",
                        value = """
                            {
                              "request_credentials": [
                                {
                                  "type": "VerifiableDiploma",
                                  "format": "jwt_vc_json"
                                }
                              ]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "With Policies",
                        value = """
                            {
                              "vp_policies": ["signature", "expired", "not-before"],
                              "vc_policies": ["signature", "expired"],
                              "request_credentials": [
                                {
                                  "type": "VerifiableDiploma",
                                  "format": "jwt_vc_json"
                                }
                              ]
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "With Custom Policies per Credential",
                        value = """
                            {
                              "vp_policies": ["signature"],
                              "vc_policies": ["signature"],
                              "request_credentials": [
                                {
                                  "type": "OpenBadgeCredential",
                                  "format": "jwt_vc_json",
                                  "policies": [
                                    "signature",
                                    {
                                      "policy": "webhook",
                                      "args": "https://example.org/verify"
                                    }
                                  ]
                                }
                              ]
                            }
                            """
                    )
                }
            )
        )
        @Valid @RequestBody VerificationRequest request,
        
        @Parameter(description = "Base URL for OID4VP authorization (default: openid4vp://authorize)")
        @RequestHeader(value = "authorizeBaseUrl", required = false) String authorizeBaseUrl,
        
        @Parameter(description = "Response mode (must be direct_post)")
        @RequestHeader(value = "responseMode", required = false) String responseMode,
        
        @Parameter(description = "Success redirect URL (can use $id placeholder)")
        @RequestHeader(value = "successRedirectUri", required = false) String successRedirectUri,
        
        @Parameter(description = "Error redirect URL (can use $id placeholder)")
        @RequestHeader(value = "errorRedirectUri", required = false) String errorRedirectUri,
        
        @Parameter(description = "Status callback URL")
        @RequestHeader(value = "statusCallbackUri", required = false) String statusCallbackUri,
        
        @Parameter(description = "API key for status callback authentication")
        @RequestHeader(value = "statusCallbackApiKey", required = false) String statusCallbackApiKey,
        
        @Parameter(description = "Custom state ID")
        @RequestHeader(value = "stateId", required = false) String stateId,
        
        @Parameter(description = "OID4VP profile (DEFAULT, EBSIV3, ISO_18013_7_MDOC)")
        @RequestHeader(value = "openId4VPProfile", required = false) String openId4VPProfile
    ) throws pt.ulusofona.ulht.credential.exception.ExternalServiceException {
        log.info("Received verification request for {} credentials", 
                request.getRequestCredentials() != null ? request.getRequestCredentials().size() : 0);

        VerificationHeaders headers = VerificationHeaders.builder()
            .authorizeBaseUrl(authorizeBaseUrl)
            .responseMode(responseMode)
            .successRedirectUri(successRedirectUri)
            .errorRedirectUri(errorRedirectUri)
            .statusCallbackUri(statusCallbackUri)
            .statusCallbackApiKey(statusCallbackApiKey)
            .stateId(stateId)
            .openId4VPProfile(openId4VPProfile)
            .build();

        VerificationResponse response = verifierService.initiateVerification(request, headers);
        
        log.info("Verification initiated with state: {}", response.getState());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Initiate basic verification",
        description = """
            Simplified endpoint for basic credential verification.
            Uses default signature policy with no custom redirects or callbacks.
            
            Useful for quick testing and simple verification scenarios.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Verification initiated successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VerificationResponse.class)
            )
        )
    })
    @PostMapping("/verify/basic")
    public ResponseEntity<VerificationResponse> initiateBasicVerification(
        @Parameter(description = "Credential type to verify", example = "VerifiableDiploma")
        @RequestParam String credentialType,
        
        @Parameter(description = "Credential format", example = "jwt_vc_json")
        @RequestParam(defaultValue = "jwt_vc_json") String format
    ) throws pt.ulusofona.ulht.credential.exception.ExternalServiceException {
        log.info("Received basic verification request for type: {}, format: {}", 
                credentialType, format);

        VerificationResponse response = verifierService.initiateBasicVerification(
            credentialType, 
            format
        );
        
        log.info("Basic verification initiated with state: {}", response.getState());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get verification status",
        description = """
            Retrieves the status and policy results of a verification session.
            
            Returns:
            - Overall verification result (true/false)
            - Detailed policy results for each credential
            - Presentation definition used
            - Token response from wallet
            
            The state value is obtained from the verification initiation response.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Verification status retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = VerificationStatusResponse.class),
                examples = @ExampleObject(
                    value = """
                        {
                          "id": "X31C7TERsFzR",
                          "verificationResult": true,
                          "policyResults": {
                            "results": [
                              {
                                "credential": "VerifiablePresentation",
                                "policyResults": [
                                  {
                                    "policy": "signature",
                                    "description": "Checks JWT signature",
                                    "is_success": true
                                  }
                                ]
                              }
                            ],
                            "policiesRun": 2
                          }
                        }
                        """
                )
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Verification session not found",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/session/{state}")
    public ResponseEntity<VerificationStatusResponse> getVerificationStatus(
        @Parameter(description = "State value from verification initiation", required = true)
        @PathVariable String state
    ) throws pt.ulusofona.ulht.credential.exception.ExternalServiceException {
        log.info("Retrieving verification status for state: {}", state);
        
        VerificationStatusResponse response = verifierService.getVerificationStatus(state);
        
        log.info("Verification status: {}", response.getVerificationResult());
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Get presented credentials",
        description = """
            Inspects credentials presented during a verification session.
            
            View modes:
            - simple: Shows holder and decoded credential payload (default)
            - verbose: Includes raw JWTs, headers, full/undisclosed payloads, and disclosures
            
            Only available after successful credential presentation.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Presented credentials retrieved successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PresentedCredentialsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Session not found or credentials not yet presented",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    @GetMapping("/session/{sessionId}/credentials")
    public ResponseEntity<PresentedCredentialsResponse> getPresentedCredentials(
        @Parameter(description = "Session identifier", required = true)
        @PathVariable String sessionId,
        
        @Parameter(description = "View mode: simple or verbose", example = "simple")
        @RequestParam(defaultValue = "simple") String viewMode
    ) throws pt.ulusofona.ulht.credential.exception.ExternalServiceException {
        log.info("Retrieving presented credentials for session: {} (viewMode: {})", 
                sessionId, viewMode);
        
        PresentedCredentialsResponse response = verifierService.getPresentedCredentials(
            sessionId, 
            viewMode
        );
        
        log.info("Presented credentials retrieved successfully");
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Check verification success",
        description = """
            Simple endpoint to check if a verification session completed successfully.
            Returns true if all policies passed, false otherwise.
            
            Useful for quick status checks without parsing full verification results.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Verification status checked",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{ \"success\": true }")
            )
        )
    })
    @GetMapping("/session/{state}/success")
    public ResponseEntity<java.util.Map<String, Boolean>> checkVerificationSuccess(
        @Parameter(description = "State value from verification initiation", required = true)
        @PathVariable String state
    ) {
        log.info("Checking verification success for state: {}", state);
        
        boolean success = verifierService.isVerificationSuccessful(state);
        
        log.info("Verification success: {}", success);
        return ResponseEntity.ok(java.util.Map.of("success", success));
    }
}

