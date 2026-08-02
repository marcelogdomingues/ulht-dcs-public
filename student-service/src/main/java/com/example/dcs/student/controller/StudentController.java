package com.example.dcs.student.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import com.example.dcs.student.client.FulfilmentClient;
import com.example.dcs.student.generated.api.StudentApi;
import com.example.dcs.student.generated.model.CredentialsResult;
import com.example.dcs.student.generated.model.LoginRequest;
import com.example.dcs.student.generated.model.LoginResponse;
import com.example.dcs.student.generated.model.VerificationRequest;
import com.example.dcs.student.generated.model.VerificationWorkflowResponse;
import com.example.dcs.student.generated.model.WorkflowStatus;
import com.example.dcs.student.kafka.StudentLoginProducer;
import com.example.dcs.student.kafka.VerificationRequestProducer;

import java.util.UUID;

/**
 * Student Controller - THE entry point for all student operations
 *
 * Implements the generated StudentApi interface from OpenAPI specification.
 *
 * Super simple:
 * 1. Receive request
 * 2. Publish to Kafka
 * 3. Return correlationId
 *
 * Everything else happens automatically via Kafka!
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StudentController implements StudentApi {

    private final StudentLoginProducer loginProducer;
    private final VerificationRequestProducer verificationProducer;
    private final FulfilmentClient fulfilmentClient;

    /**
     * POST /student/issue
     *
     * THE starting point for credential issuance!
     *
     * Flow:
     * 1. Student Service publishes to Kafka: student.login.requested
     * 2. Sis Service gets student data from DCS API
     * 3. Credential Service issues W3C credentials (Educational ID, Identity Credential, etc.)
     * 4. Fulfilment Service tracks and notifies
     *
     * Implementation of StudentApi.issueCredentials() from generated OpenAPI interface.
     */

    //TODO: Tratar dos Disclose Atributes
    @Override
    public ResponseEntity<LoginResponse> issueCredentials(LoginRequest loginRequest) {
        log.info("🚀 Credential issuance request received: userName={}", loginRequest.getUserName());

        // Generate correlation ID for tracking
        UUID correlationId = UUID.randomUUID();
        String correlationIdStr = correlationId.toString();

        // Publish to Kafka (this triggers the entire workflow!)
        loginProducer.publishStudentLogin(correlationIdStr, loginRequest);

        // Build response using generated model
        LoginResponse response = LoginResponse.builder()
            .correlationId(correlationId)
            .status(LoginResponse.StatusEnum.PROCESSING)
            .message("Credential issuance initiated, processing...")
            .monitorAt("/student/status/" + correlationIdStr)
            .credentialsAt("/student/credentials/" + correlationIdStr)
            .build();

        log.info("✅ Credential issuance request published, correlationId: {}", correlationIdStr);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /student/verify
     *
     * THE starting point for credential verification!
     *
     * Flow:
     * 1. Student Service publishes to Kafka: verification.requested
     * 2. Credential Service picks up event and initiates verification with walt.id
     * 3. Returns verification URL (for QR code or direct link)
     * 4. User presents credentials from wallet
     * 5. Service validates credentials and stores results
     * 6. Fulfilment Service tracks verification progress
     *
     * Implementation of StudentApi.verifyCredentials() from generated OpenAPI interface.
     */
    @Override
    public ResponseEntity<VerificationWorkflowResponse> verifyCredentials(VerificationRequest verificationRequest) {
        log.info("🔍 Credential verification request received: type={}, userId={}",
                verificationRequest.getCredentialType(), verificationRequest.getUserId());

        // Generate correlation ID for tracking
        UUID correlationId = UUID.randomUUID();
        String correlationIdStr = correlationId.toString();

        // CRITICAL: Initialize workflow in Fulfilment Service immediately
        // This ensures the workflow exists before Kafka events are consumed
        try {
            fulfilmentClient.initializeWorkflow(correlationIdStr);
            log.debug("✅ Workflow initialized in Fulfilment Service for correlationId: {}", correlationIdStr);
        } catch (Exception e) {
            log.warn("⚠️ Failed to initialize workflow in Fulfilment Service (will be created by Kafka events): {}", e.getMessage());
            // Continue anyway - Kafka events will create the workflow
        }

        // Publish to Kafka (this triggers the verification workflow!)
        verificationProducer.publishVerificationRequest(
            correlationIdStr,
            verificationRequest.getCredentialType(),
            verificationRequest.getFormat() != null ? verificationRequest.getFormat().getValue() : null,
            verificationRequest.getUserId(),
            verificationRequest.getVpPolicies(),
            verificationRequest.getVcPolicies(),
            verificationRequest.getSuccessRedirectUri(),
            verificationRequest.getErrorRedirectUri(),
            verificationRequest.getStatusCallbackUri(),
            verificationRequest.getStatusCallbackApiKey(),
            verificationRequest.getCustomStateId()
        );

        // Build response using generated model
        VerificationWorkflowResponse response = VerificationWorkflowResponse.builder()
            .correlationId(correlationId)
            .status(VerificationWorkflowResponse.StatusEnum.PROCESSING)
            .message("Verification request initiated, processing...")
            .monitorAt("/student/status/" + correlationIdStr)
            .verificationAt("/student/verification/" + correlationIdStr)
            .build();

        log.info("✅ Verification request published, correlationId: {}", correlationIdStr);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /student/status/{correlationId}
     *
     * Check workflow progress (proxies to Fulfilment Service)
     *
     * Implementation of StudentApi.getWorkflowStatus() from generated OpenAPI interface.
     */
    @Override
    public ResponseEntity<WorkflowStatus> getWorkflowStatus(String correlationId) {
        log.debug("📊 Checking status for correlationId: {}", correlationId);

        try {
            // Proxy to Fulfilment Service - returns WorkflowStatus JSON
            // Fulfilment Service now always returns 200 with a status (even if workflow not tracked yet)
            ResponseEntity<java.util.Map<String, Object>> fulfilmentResponse = fulfilmentClient.getStatus(correlationId);

            java.util.Map<String, Object> body = fulfilmentResponse.getBody();
            if (body == null) {
                // This shouldn't happen anymore, but handle it gracefully
                log.warn("Fulfilment service returned null body for correlationId: {}", correlationId);
                WorkflowStatus defaultStatus = WorkflowStatus.builder()
                    .correlationId(UUID.fromString(correlationId))
                    .status(WorkflowStatus.StatusEnum.PROCESSING)
                    .progress(0)
                    .message("Workflow is being initialized, please wait...")
                    .build();
                return ResponseEntity.ok(defaultStatus);
            }

            // Parse fulfilment response to WorkflowStatus
            
            WorkflowStatus status = WorkflowStatus.builder()
                .correlationId(UUID.fromString(correlationId))
                .status(mapStatus((String) body.get("status")))
                .progress((Integer) body.get("progress"))
                .message((String) body.get("message"))
                .startTime(body.get("timestamp") != null ? 
                    java.time.OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli((Long) body.get("timestamp")),
                        java.time.ZoneId.systemDefault()) : null)
                .completionTime(body.get("lastUpdated") != null ?
                    java.time.OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli((Long) body.get("lastUpdated")),
                        java.time.ZoneId.systemDefault()) : null)
                .result(body.get("result"))
                .error((String) body.get("errorMessage"))
                .build();

            return ResponseEntity.ok(status);
        } catch (feign.FeignException.NotFound e) {
            // Fulfilment service returned 404 (shouldn't happen with new implementation, but handle gracefully)
            log.debug("Fulfilment service returned 404 for correlationId: {} - returning default PROCESSING status", correlationId);
            WorkflowStatus defaultStatus = WorkflowStatus.builder()
                .correlationId(UUID.fromString(correlationId))
                .status(WorkflowStatus.StatusEnum.PROCESSING)
                .progress(0)
                .message("Workflow is being initialized, please wait...")
                .build();
            return ResponseEntity.ok(defaultStatus);
        } catch (Exception e) {
            log.error("Error getting workflow status for correlationId: {}", correlationId, e);
            throw e;
        }
    }
    
    /**
     * Maps Fulfilment Service status strings to WorkflowStatus.StatusEnum
     */
    private WorkflowStatus.StatusEnum mapStatus(String status) {
        if (status == null) return WorkflowStatus.StatusEnum.PROCESSING;
        
        return switch (status.toUpperCase()) {
            case "INITIATED" -> WorkflowStatus.StatusEnum.PROCESSING;
            case "PROCESSING", "VALIDATING", "PREPARING_ISSUER", "ISSUING_CREDENTIALS" -> 
                WorkflowStatus.StatusEnum.PROCESSING;
            case "COMPLETED" -> WorkflowStatus.StatusEnum.COMPLETED;
            case "FAILED" -> WorkflowStatus.StatusEnum.FAILED;
            case "CANCELLED" -> WorkflowStatus.StatusEnum.CANCELLED;
            default -> WorkflowStatus.StatusEnum.PROCESSING;
        };
    }

    /**
     * GET /student/credentials/{correlationId}
     *
     * Get final credential URLs (proxies to Fulfilment Service)
     *
     * Implementation of StudentApi.getCredentials() from generated OpenAPI interface.
     */
    @Override
    public ResponseEntity<CredentialsResult> getCredentials(String correlationId) {
        log.debug("🎓 Getting credentials for correlationId: {}", correlationId);

        try {
            // First check workflow status to see if it exists and is completed
            ResponseEntity<java.util.Map<String, Object>> statusResponse = fulfilmentClient.getStatus(correlationId);
            
            if (statusResponse.getBody() == null) {
                log.debug("Workflow not found for correlationId: {}", correlationId);
                return ResponseEntity.status(202).build();  // Return 202 - workflow may not be tracked yet
            }
            
            java.util.Map<String, Object> statusBody = statusResponse.getBody();
            if (statusBody == null) {
                log.debug("Empty status body for correlationId: {}", correlationId);
                return ResponseEntity.status(202).build();
            }
            
            String status = (String) statusBody.get("status");
            
            // If workflow is not completed, return 202 (still processing)
            if (!"COMPLETED".equals(status)) {
                log.debug("Workflow not completed yet: {} (status: {})", correlationId, status);
                return ResponseEntity.status(202).build();  // 202 Accepted - still processing
            }
            
            // Workflow is completed, try to get result
            ResponseEntity<java.util.Map<String, Object>> fulfilmentResponse = fulfilmentClient.getResult(correlationId);

            if (fulfilmentResponse.getBody() == null) {
                log.warn("Workflow completed but result not available for correlationId: {}", correlationId);
                return ResponseEntity.status(202).build();  // Still processing result
            }

            // Parse fulfilment response - credential data is in nested "result" field
            java.util.Map<String, Object> body = fulfilmentResponse.getBody();
            
            if (body == null) {
                log.warn("Empty response body for completed workflow: {}", correlationId);
                return ResponseEntity.status(202).build();
            }
            
            // Extract result object (contains the actual credential data)
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> resultData = (java.util.Map<String, Object>) body.get("result");
            
            if (resultData == null) {
                log.warn("No result data found for completed workflow: {}", correlationId);
                return ResponseEntity.status(202).build();  // Result not ready yet
            }

            // Build CredentialsResult from nested result data
            @SuppressWarnings("unchecked")
            CredentialsResult result = CredentialsResult.builder()
                .correlationId(UUID.fromString(correlationId))
                .userId((String) resultData.get("userId"))
                .studentId((String) resultData.get("studentId"))
                .status(CredentialsResult.StatusEnum.COMPLETED)
                .credentialOfferUrls((java.util.List<String>) resultData.get("credentialOfferUrls"))
                .credentialTypes((java.util.List<String>) resultData.get("credentialTypes"))
                .credentialsIssued((Integer) resultData.get("credentialsIssued"))
                .build();

            return ResponseEntity.ok(result);
            
        } catch (feign.FeignException.NotFound e) {
            // Handle 404 from fulfilment service - workflow may still be processing
            log.debug("Workflow not found or still processing (404): {}", correlationId);
            return ResponseEntity.status(202).build();  // Return 202 instead of 404
        } catch (feign.FeignException e) {
            log.error("Feign error getting credentials for correlationId: {}", correlationId, e);
            return ResponseEntity.status(500).build();
        } catch (Exception e) {
            log.error("Unexpected error getting credentials for correlationId: {}", correlationId, e);
            return ResponseEntity.status(500).build();
        }
    }
}

