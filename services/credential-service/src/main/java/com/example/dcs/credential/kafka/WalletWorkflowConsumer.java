package com.example.dcs.credential.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import com.example.dcs.credential.service.StudentWalletService;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer for wallet workflow requests.
 * Processes wallet operations (create, login, get DID) asynchronously via Kafka.
 * 
 * Topics:
 * - Input: wallet.requests (consumes wallet operation requests)
 * - Output: wallet.progress (publishes progress updates)
 * - Output: wallet.completed (publishes completion events)
 * - Output: wallet.error (publishes error events)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletWorkflowConsumer {

    private final StudentWalletService studentWalletService;
    private final KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate;
    private final ObjectMapper objectMapper;
    private final com.example.dcs.credential.service.ProcessedEventService processedEventService;

    /**
     * Consumes wallet workflow requests from Credential Service or other services
     */
    @KafkaListener(
        topics = "wallet.requests",
        groupId = "credential-service-wallet-group",
        containerFactory = "workflowKafkaListenerContainerFactory"
    )
    public void handleWalletWorkflowRequest(@Payload Map<String, Object> request,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                           @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("📨 Received wallet workflow request from topic: {} (partition: {}, offset: {})",
                topic, partition, offset);

        // Idempotency guard: skip records that have already been processed (at-least-once safety).
        if (processedEventService.isDuplicate(topic, partition, offset, "wallet-workflow")) {
            return;
        }

        String correlationId = (String) request.get("correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            String operationType = (String) request.get("operationType");
            String userId = (String) request.get("userId");
            Object studentData = request.get("studentData");

            if (correlationId == null || operationType == null || studentData == null) {
                log.error("❌ Invalid wallet workflow request - missing required fields: {}", request);
                publishWalletError(correlationId, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            try {
                processWalletWorkflow(correlationId, operationType, userId, studentData);
            } catch (Exception e) {
                log.error("❌ Error processing wallet workflow for correlationId: {}", correlationId, e);
                publishWalletError(correlationId, "WORKFLOW_ERROR", e.getMessage());
            }
        } finally {
            MDC.remove("correlationId");
        }
    }

    /**
     * Processes the wallet workflow
     */
    private void processWalletWorkflow(String correlationId, String operationType, 
                                       String userId, Object studentData) throws Exception {
        log.info("🚀 Starting wallet workflow: correlationId={}, operationType={}, userId={}",
                correlationId, operationType, userId);

        try {
            // Step 1: Validate student data (20%)
            publishWalletProgress(correlationId, userId, "VALIDATING", 20, 
                    "Validating student data for wallet operations");

            // Convert student data to Map if needed
            @SuppressWarnings("unchecked")
            Map<String, Object> dataMap = (studentData instanceof Map) ?
                    (Map<String, Object>) studentData :
                    objectMapper.convertValue(studentData, Map.class);

            log.debug("Converted student data to Map for correlationId: {}", correlationId);

            // Step 2: Ensure wallet exists (50%)
            publishWalletProgress(correlationId, userId, "ENSURE_WALLET", 50,
                    "Ensuring wallet exists for student");

            log.info("📱 Calling studentWalletService.ensureWalletExists for correlationId: {}", correlationId);
            StudentWalletService.StudentWalletInfo walletInfo = studentWalletService.ensureWalletExists(dataMap);
            log.info("✅ Wallet service returned wallet info for correlationId: {} (email: {}, walletId: {}, subjectDid: {})",
                    correlationId, walletInfo.getEmail(), walletInfo.getWalletId(), walletInfo.getSubjectDid());

            // Step 3: Complete workflow (100%)
            publishWalletProgress(correlationId, userId, "COMPLETED", 100,
                    "Wallet operations completed successfully");

            // Build result (include sessionCookie for credential acceptance)
            Map<String, Object> result = new HashMap<>();
            result.put("correlationId", correlationId);
            result.put("userId", userId);
            result.put("operationType", operationType);
            result.put("email", walletInfo.getEmail());
            result.put("walletId", walletInfo.getWalletId());
            result.put("subjectDid", walletInfo.getSubjectDid());
            result.put("sessionCookie", walletInfo.getSessionCookie());  // Include session cookie for credential acceptance
            result.put("status", "SUCCESS");
            result.put("timestamp", Instant.now().toString());

            log.info("📤 Publishing wallet completion event for correlationId: {}", correlationId);
            publishWalletCompleted(correlationId, userId, result);
            log.info("✅ Published wallet completion event for correlationId: {}", correlationId);

            log.info("✅ Wallet workflow completed successfully for correlationId: {}", correlationId);

        } catch (Exception e) {
            log.error("❌ Wallet workflow failed for correlationId: {}: {}",
                    correlationId, e.getMessage(), e);
            log.info("📤 Publishing wallet error event for correlationId: {}", correlationId);
            publishWalletError(correlationId, "WORKFLOW_FAILED", e.getMessage() != null ? e.getMessage() : "Unknown error");
            log.info("✅ Published wallet error event for correlationId: {}", correlationId);
            throw e;
        }
    }

    /**
     * Publishes wallet progress events
     */
    private void publishWalletProgress(String correlationId, String userId, String status,
                                      int progress, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("userId", userId);
        event.put("status", status);
        event.put("progress", progress);
        event.put("message", message);
        event.put("timestamp", Instant.now().toString());

        try {
            workflowKafkaTemplate.send("wallet.progress", correlationId, event);
            log.debug("Published wallet progress to wallet.progress topic (correlationId: {})", correlationId);
        } catch (Exception e) {
            log.error("Failed to publish wallet progress for correlationId: {}", correlationId, e);
        }
    }

    /**
     * Publishes wallet completion events
     */
    private void publishWalletCompleted(String correlationId, String userId, Map<String, Object> result) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("userId", userId);
        event.put("status", "COMPLETED");
        event.put("progress", 100);
        event.put("message", "Wallet operations completed successfully");
        event.put("result", result);
        event.put("timestamp", Instant.now().toString());

        try {
            workflowKafkaTemplate.send("wallet.completed", correlationId, event);
            log.info("Published wallet completion to wallet.completed topic (correlationId: {})", correlationId);
        } catch (Exception e) {
            log.error("Failed to publish wallet completion for correlationId: {}", correlationId, e);
        }
    }

    /**
     * Publishes wallet error events
     */
    private void publishWalletError(String correlationId, String errorCode, String errorMessage) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("status", "FAILED");
        event.put("errorCode", errorCode);
        event.put("errorMessage", errorMessage);
        event.put("timestamp", Instant.now().toString());

        try {
            workflowKafkaTemplate.send("wallet.error", correlationId, event);
            log.error("Published wallet error to wallet.error topic (correlationId: {})", correlationId);
        } catch (Exception e) {
            log.error("Failed to publish wallet error for correlationId: {}", correlationId, e);
        }
    }
}
