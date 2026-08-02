package com.example.dcs.credential.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.example.dcs.credential.dto.waltid.verifier.*;
import com.example.dcs.credential.service.VerifierService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka consumer for verification workflow requests from Student Service.
 * Automatically initiates W3C Verifiable Credential verification via walt.id OID4VP.
 * 
 * NEW: Request-Reply Pattern
 * - Returns confirmation that verification was initiated
 * - Student Service waits for this reply
 * - Enables guaranteed end-to-end confirmation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationWorkflowConsumer {
    
    private final VerifierService verifierService;
    private final KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate;
    private final com.example.dcs.credential.service.ProcessedEventService processedEventService;
    
    /**
     * Consumes verification workflow requests from Student Service
     * Returns reply message confirming verification initiated
     */
    @KafkaListener(
        topics = "verification.requested",
        groupId = "credential-service-verification-group",
        containerFactory = "workflowKafkaListenerContainerFactory"
    )
    @org.springframework.messaging.handler.annotation.SendTo
    public Map<String, Object> handleVerificationWorkflowRequest(@Payload Map<String, Object> request,
                                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                              @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("📨 Received verification workflow request from topic: {} (partition: {}, offset: {})",
                topic, partition, offset);

        // Idempotency guard: skip records that have already been processed (at-least-once safety).
        if (processedEventService.isDuplicate(topic, partition, offset, "verification-workflow")) {
            String dupCorrelationId = (String) request.get("correlationId");
            Map<String, Object> dupReply = new HashMap<>();
            dupReply.put("status", "ALREADY_PROCESSED");
            dupReply.put("correlationId", dupCorrelationId);
            dupReply.put("message", "Verification request already processed (duplicate delivery ignored)");
            dupReply.put("timestamp", Instant.now().toString());
            return dupReply;
        }

        String correlationId = (String) request.get("correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            String credentialType = (String) request.get("credentialType");
            String format = (String) request.get("format");
            String userId = (String) request.get("userId");

            // userId is optional for verification - use default if not provided
            if (userId == null || userId.isEmpty()) {
                userId = "verifier";
                log.debug("No userId provided, using default: {}", userId);
            }

            if (correlationId == null || credentialType == null) {
                log.error("❌ Invalid verification request - missing required fields: {}", request);
                publishVerificationError(correlationId, "INVALID_REQUEST", "Missing required fields: correlationId or credentialType");
                return buildErrorReply(correlationId, userId, "INVALID_REQUEST", "Missing required fields: correlationId or credentialType");
            }

            log.info("🔍 Processing verification workflow: correlationId={}, type={}, userId={}",
                    correlationId, credentialType, userId);

            try {
                return processVerificationWorkflow(correlationId, credentialType, format, userId, request);
            } catch (Exception e) {
                log.error("❌ Error processing verification workflow for correlationId: {}", correlationId, e);
                publishVerificationError(correlationId, "WORKFLOW_ERROR", e.getMessage());
                return buildErrorReply(correlationId, userId, "WORKFLOW_ERROR", e.getMessage());
            }
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Processes the complete verification workflow
     * Initiates W3C Verifiable Credential verification via walt.id
     * Returns reply message with verification URL
     */
    private Map<String, Object> processVerificationWorkflow(
            String correlationId,
            String credentialType,
            String format,
            String userId,
            Map<String, Object> request) throws Exception {
        
        log.info("🚀 Starting verification workflow: correlationId={}", correlationId);
        
        // Build verification request
        // Ensure format is set (default to jwt_vc_json if not provided)
        String effectiveFormat = format != null && !format.isEmpty() ? format : "jwt_vc_json";
        
        log.debug("Building verification request: type={}, format={}", credentialType, effectiveFormat);
        
        RequestedCredential credential = RequestedCredential.builder()
            .type(credentialType)
            .format(effectiveFormat)
            .build();
        
        log.debug("Created RequestedCredential: type={}, format={}", credential.getType(), credential.getFormat());
        
        List<Object> vpPolicies = new ArrayList<>();
        List<Object> vcPolicies = new ArrayList<>();
        
        // Add policies from request
        if (request.containsKey("vpPolicies")) {
            @SuppressWarnings("unchecked")
            List<Object> vp = (List<Object>) request.get("vpPolicies");
            vpPolicies = vp;
        }
        if (request.containsKey("vcPolicies")) {
            @SuppressWarnings("unchecked")
            List<Object> vc = (List<Object>) request.get("vcPolicies");
            vcPolicies = vc;
        }
        
        VerificationRequest verificationRequest = VerificationRequest.builder()
            .requestCredentials(List.of(credential))
            .vpPolicies(vpPolicies)
            .vcPolicies(vcPolicies)
            .build();
        
        // Build verification headers
        VerificationHeaders headers = VerificationHeaders.builder()
            .successRedirectUri((String) request.get("successRedirectUri"))
            .errorRedirectUri((String) request.get("errorRedirectUri"))
            .statusCallbackUri((String) request.get("statusCallbackUri"))
            .statusCallbackApiKey((String) request.get("statusCallbackApiKey"))
            .stateId((String) request.get("customStateId"))
            .build();
        
        // Progress update 0: Initialize workflow (CRITICAL - ensures workflow is tracked)
        // This must be published FIRST to ensure the workflow exists in fulfilment service
        log.info("📤 Publishing INITIATED event for correlationId: {}", correlationId);
        publishVerificationProgress(correlationId, userId, "INITIATED", 0, 
            "Verification workflow initiated, preparing verification request...");
        log.info("✅ Published INITIATED event for correlationId: {}", correlationId);
        
        // Progress update 1: Initiating verification
        publishVerificationProgress(correlationId, userId, "PROCESSING", 30, 
            "Initiating verification with walt.id...");
        
        // Initiate verification with walt.id
        log.info("📤 Calling walt.id verifier API for correlationId: {}", correlationId);
        VerificationResponse response = verifierService.initiateVerification(verificationRequest, headers);
        
        log.info("✅ Verification initiated successfully: url={}, state={}", 
                response.getUrl(), response.getState());
        
        // Progress update 2: Verification URL ready
        publishVerificationProgress(correlationId, userId, "PROCESSING", 60, 
            "Verification URL ready, waiting for credential presentation...");
        
        // Build result map
        Map<String, Object> verificationResult = new HashMap<>();
        verificationResult.put("verificationUrl", response.getUrl());
        verificationResult.put("presentationId", response.getPresentationId());
        verificationResult.put("state", response.getState());
        verificationResult.put("credentialType", credentialType);
        verificationResult.put("format", format);
        
        // Publish completed event
        publishVerificationCompleted(correlationId, userId, verificationResult);
        
        log.info("✅ Verification workflow completed for correlationId: {}", correlationId);
        
        // SUCCESS: Return reply confirming verification initiated
        Map<String, Object> successReply = new HashMap<>();
        successReply.put("status", "VERIFICATION_STARTED");
        successReply.put("correlationId", correlationId);
        successReply.put("verificationUrl", response.getUrl());
        successReply.put("state", response.getState());
        successReply.put("presentationId", response.getPresentationId());
        successReply.put("credentialType", credentialType);
        successReply.put("format", format);
        successReply.put("userId", userId);
        successReply.put("message", "Verification initiated successfully with walt.id");
        successReply.put("timestamp", Instant.now().toString());
        
        log.info("📤 Sending SUCCESS reply for verification correlationId: {}", correlationId);
        return successReply;
    }
    
    /**
     * Publishes verification progress to Kafka
     */
    private void publishVerificationProgress(String correlationId, String userId, 
                                            String status, int progress, String message) {
        Map<String, Object> progressPayload = new HashMap<>();
        progressPayload.put("correlationId", correlationId);
        progressPayload.put("userId", userId);
        progressPayload.put("status", status);
        progressPayload.put("progress", progress);
        progressPayload.put("message", message);
        progressPayload.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send("verification.progress", correlationId, progressPayload);
            log.info("📊 Published verification progress to Kafka: correlationId={}, status={}, progress={}%", 
                    correlationId, status, progress);
        } catch (Exception e) {
            log.error("❌ Failed to publish verification progress: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Publishes verification completion to Kafka
     */
    private void publishVerificationCompleted(String correlationId, String userId, 
                                             Map<String, Object> result) {
        Map<String, Object> completedPayload = new HashMap<>();
        completedPayload.put("correlationId", correlationId);
        completedPayload.put("userId", userId);
        completedPayload.put("status", "COMPLETED");
        completedPayload.put("result", result);
        completedPayload.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send("verification.completed", correlationId, completedPayload);
            log.info("✅ Published verification completed event");
        } catch (Exception e) {
            log.error("❌ Failed to publish verification completed: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Publishes verification error to Kafka
     */
    private void publishVerificationError(String correlationId, String errorCode, String errorMessage) {
        Map<String, Object> errorPayload = new HashMap<>();
        errorPayload.put("correlationId", correlationId);
        errorPayload.put("status", "FAILED");
        errorPayload.put("errorCode", errorCode);
        errorPayload.put("errorMessage", errorMessage);
        errorPayload.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send("verification.error", correlationId, errorPayload);
            log.error("❌ Published verification error: {}", errorMessage);
        } catch (Exception e) {
            log.error("❌ Failed to publish verification error: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Builds error reply message for request-reply pattern
     */
    private Map<String, Object> buildErrorReply(String correlationId, String userId, 
                                                String errorCode, String errorMessage) {
        Map<String, Object> errorReply = new HashMap<>();
        errorReply.put("status", "VERIFICATION_FAILED");
        errorReply.put("correlationId", correlationId);
        errorReply.put("userId", userId);
        errorReply.put("errorCode", errorCode);
        errorReply.put("errorMessage", "Failed to initiate verification: " + errorMessage);
        errorReply.put("timestamp", Instant.now().toString());
        
        log.info("📤 Sending ERROR reply for verification correlationId: {} - {}", correlationId, errorCode);
        return errorReply;
    }
}


