package pt.ulusofona.digital.wallet.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pt.ulusofona.digital.wallet.service.LusofonaService;
import pt.ulusofona.digital.wallet.exception.ErrorCodes;
import pt.ulusofona.digital.wallet.exception.http.*;
import pt.ulusofona.digital.wallet.exception.ValidationException;
import pt.ulusofona.digital.wallet.exception.lusofona.LusofonaApiException;

import java.util.HashMap;
import java.util.Map;

/**
 * Consumes student login requests from Student Service
 * Gets student data from ULHT API and publishes to credential service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentLoginConsumer {

    private final LusofonaService lusofonaService;
    private final CredentialWorkflowProducer credentialWorkflowProducer;
    private final KafkaTemplate<String, Map<String, Object>> kafkaTemplate;

    /**
     * Listens to student.login.requested topic from Student Service
     * 
     * Flow:
     * 1. Receives minimal student data (username + installKey)
     * 2. Calls ULHT API to get FULL student data
     * 3. Publishes to student.data.retrieved for Credential Service
     * 4. Sends REPLY message to confirm workflow started
     * 
     * NEW: Request-Reply Pattern
     * - Returns confirmation message that workflow started
     * - Student Service waits for this reply
     * - Enables reliable end-to-end confirmation
     */
    @KafkaListener(
        topics = "${kafka.topics.student-login-requested:student.login.requested}",
        groupId = "${spring.kafka.consumer.group-id:lusofona-service-group}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    @org.springframework.messaging.handler.annotation.SendTo
    public Map<String, Object> handleStudentLogin(Map<String, Object> payload) {
        String correlationId = (String) payload.get("correlationId");
        String userName = (String) payload.get("userName");
        String installKey = (String) payload.get("installKey");
        
        log.info("📨 Received student login from Kafka: correlationId={}, userName={}", 
                 correlationId, userName);

        try {
            // Build request map for ULHT API
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("userName", userName);
            requestMap.put("installKey", installKey);
            requestMap.put("language", payload.getOrDefault("language", "PT"));
            requestMap.put("platform", payload.getOrDefault("platform", "ios"));
            requestMap.put("application", payload.getOrDefault("application", "org.cofac.mobile.ulht"));
            requestMap.put("versionCode", payload.getOrDefault("versionCode", "1601206"));

            // 1. Get FULL student data from ULHT API (NO FALLBACK TO MOCK DATA!)
            log.info("🔍 Calling ULHT API for student data: {}", userName);
            Object studentData = lusofonaService.login(requestMap);
            log.info("✅ Got real student data from ULHT API for user: {}", userName);

            // 2. Publish to Kafka for Credential Service
            // IMPORTANT: Pass the SAME correlationId from Student Service (not a new one!)
            credentialWorkflowProducer.startCredentialWorkflow(
                correlationId,  // ← Use original correlationId from Student Service!
                userName,
                studentData
            );
            
            log.info("✅ Published student data to credential.requests: correlationId={}", 
                     correlationId);
            
            // SUCCESS: Return reply confirming workflow started
            Map<String, Object> successReply = new HashMap<>();
            successReply.put("status", "WORKFLOW_STARTED");
            successReply.put("correlationId", correlationId);
            successReply.put("userName", userName);
            successReply.put("message", "Student data retrieved successfully, credential workflow initiated");
            successReply.put("timestamp", System.currentTimeMillis());
            
            log.info("📤 Sending SUCCESS reply for correlationId: {}", correlationId);
            return successReply;
            
        } catch (UnauthorizedException e) {
            log.error("❌ Authentication failed for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.UNAUTHORIZED);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.UNAUTHORIZED);
            
        } catch (ForbiddenException e) {
            log.error("❌ Access forbidden for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.FORBIDDEN);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.FORBIDDEN);
            
        } catch (NotFoundException e) {
            log.error("❌ Data not found for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.NOT_FOUND);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.NOT_FOUND);
            
        } catch (BadRequestException e) {
            log.error("❌ Bad request for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.BAD_REQUEST);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.BAD_REQUEST);
            
        } catch (ValidationException e) {
            log.error("❌ Validation error for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.VALIDATION_ERROR);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.VALIDATION_ERROR);
            
        } catch (LusofonaApiException e) {
            log.error("❌ Lusofona API error for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            // Map Lusofona API error codes to our ErrorCodes
            ErrorCodes errorCode = mapLusofonaErrorCode(e.getErrorCode());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                errorCode);
            return buildErrorReply(correlationId, userName, e.getMessage(), errorCode);
            
        } catch (TimeoutException e) {
            log.error("❌ Timeout calling ULHT API for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.TIMEOUT);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.TIMEOUT);
            
        } catch (ExternalServiceException e) {
            log.error("❌ External service error for user {}: {} - Workflow will FAIL", 
                     userName, e.getMessage());
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.EXTERNAL_SERVICE_ERROR);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.EXTERNAL_SERVICE_ERROR);
            
        } catch (Exception e) {
            // CRITICAL: Unexpected error - DO NOT issue credentials with fake data!
            log.error("❌ Unexpected error for user {}: {} - Workflow will FAIL (no credentials issued)", 
                     userName, e.getMessage(), e);
            publishError(correlationId, userName, 
                "Failed to retrieve student data from ULHT API: " + e.getMessage(),
                ErrorCodes.INTERNAL_SERVER_ERROR);
            return buildErrorReply(correlationId, userName, e.getMessage(), ErrorCodes.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Builds error reply message for request-reply pattern
     */
    private Map<String, Object> buildErrorReply(String correlationId, String userName, 
                                                String errorMessage, ErrorCodes errorCode) {
        Map<String, Object> errorReply = new HashMap<>();
        errorReply.put("status", "WORKFLOW_FAILED");
        errorReply.put("correlationId", correlationId);
        errorReply.put("userName", userName);
        errorReply.put("errorCode", errorCode.getCode());
        errorReply.put("errorName", errorCode.getFunName());
        errorReply.put("errorMessage", "Failed to retrieve student data: " + errorMessage);
        errorReply.put("timestamp", System.currentTimeMillis());
        
        log.info("📤 Sending ERROR reply for correlationId: {} - {}", correlationId, errorCode.getCode());
        return errorReply;
    }

    /**
     * Publishes error to Kafka for Fulfilment Service to track
     * This ensures the workflow fails properly and user gets notified
     */
    private void publishError(String correlationId, String userName, String errorMessage, ErrorCodes errorCode) {
        try {
            Map<String, Object> errorEvent = new HashMap<>();
            errorEvent.put("correlationId", correlationId);
            errorEvent.put("userName", userName);
            errorEvent.put("status", "FAILED");
            errorEvent.put("errorCode", errorCode.getCode());
            errorEvent.put("errorName", errorCode.getFunName());
            errorEvent.put("errorMessage", errorMessage);
            errorEvent.put("timestamp", System.currentTimeMillis());
            
            // Publish to credential.error topic (Fulfilment Service listens to this)
            kafkaTemplate.send("credential.error", correlationId, errorEvent);
            
            log.info("✅ Published error to credential.error topic: {} - {} for correlationId: {}", 
                    errorCode.getCode(), errorCode.getFunName(), correlationId);
        } catch (Exception e) {
            log.error("❌ Failed to publish error event for correlationId: {}", correlationId, e);
        }
    }
    
    /**
     * Maps LusofonaErrorCode to our ErrorCodes enum
     */
    private ErrorCodes mapLusofonaErrorCode(pt.ulusofona.digital.wallet.exception.lusofona.LusofonaErrorCode lusofonaErrorCode) {
        return switch (lusofonaErrorCode) {
            case OK -> ErrorCodes.LUSOFONA_OK;
            case GENERIC_ERROR -> ErrorCodes.LUSOFONA_GENERIC_ERROR;
            case AUTHENTICATION_FAILURE -> ErrorCodes.LUSOFONA_AUTH_FAILURE;
            case IMEI_ALREADY_REGISTERED -> ErrorCodes.LUSOFONA_IMEI_REGISTERED;
            case APPLICATION_NOT_REGISTERED -> ErrorCodes.LUSOFONA_APP_NOT_REGISTERED;
            case OPERATION_NOT_AVAILABLE -> ErrorCodes.LUSOFONA_OPERATION_UNAVAILABLE;
            case INCORRECT_FORMAT -> ErrorCodes.LUSOFONA_INCORRECT_FORMAT;
            case DATA_NOT_FOUND -> ErrorCodes.LUSOFONA_DATA_NOT_FOUND;
            case INVALID_INSTALLATION_KEY -> ErrorCodes.LUSOFONA_INVALID_KEY;
            case INVALID_REQUEST -> ErrorCodes.LUSOFONA_INVALID_REQUEST;
            case BUSINESS_RULE_VALIDATION -> ErrorCodes.LUSOFONA_BUSINESS_RULE;
        };
    }
    
}


