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
import com.example.dcs.credential.builder.GenericCredentialBuilder;
import com.example.dcs.credential.config.CredentialTemplate;
import com.example.dcs.credential.config.CredentialTemplateConfig;
import com.example.dcs.credential.dto.waltid.IssueCredentialRequest;
import com.example.dcs.credential.dto.waltid.IssuerKey;
import com.example.dcs.credential.service.IssuerKeyService;
import com.example.dcs.credential.service.StudentWalletService;
import com.example.dcs.credential.service.WaltidService;
import com.example.dcs.credential.kafka.WalletWorkflowProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Kafka consumer for credential workflow requests from SIS Service.
 * Automatically issues W3C Verifiable Credentials based on configured templates.
 * NEW: Uses generic template-based system - add credential types via application.yml!
 * NEW: Integrates with student wallets - each student has their own wallet and DID
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialWorkflowConsumer {
    
    private final WaltidService waltidService;
    private final IssuerKeyService issuerKeyService;
    private final StudentWalletService studentWalletService;
    private final WalletWorkflowProducer walletWorkflowProducer;
    private final KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate;
    private final ObjectMapper objectMapper;
    private final CredentialTemplateConfig credentialTemplateConfig;
    private final GenericCredentialBuilder genericCredentialBuilder;
    private final com.example.dcs.credential.service.CredentialExpirationChecker credentialExpirationChecker;
    private final com.example.dcs.credential.service.ProcessedEventService processedEventService;
    private final com.example.dcs.credential.service.CredentialStatusService credentialStatusService;

    /**
     * Public base URL of THIS credential-service, used to build the
     * {@code statusListCredential} URL embedded in issued VCs (ADR 0007).
     * A verifier must be able to GET {baseUrl}/status-list/{listId}. Defaults to
     * the in-cluster hostname; override via {@code credentials.status.base-url}.
     */
    @org.springframework.beans.factory.annotation.Value(
            "${credentials.status.base-url:http://dcs-credential-service:8086/api/v1}")
    private String statusBaseUrl;

    /** Identifier of the single default status list published by this service. */
    @org.springframework.beans.factory.annotation.Value(
            "${credentials.status.list-id:credential-status-list-1}")
    private String statusListId;

    // Map to store pending wallet requests (correlationId -> CompletableFuture)
    private final Map<String, CompletableFuture<StudentWalletService.StudentWalletInfo>> pendingWalletRequests = 
            new ConcurrentHashMap<>();
    
    /**
     * Consumes wallet completion events from WalletWorkflowConsumer
     */
    @KafkaListener(
        topics = "wallet.completed",
        groupId = "credential-service-wallet-completion-group",
        containerFactory = "workflowKafkaListenerContainerFactory"
    )
    public void handleWalletCompleted(@Payload Map<String, Object> event,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset) {
        
        String correlationId = (String) event.get("correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            log.info("📨 Received wallet completion event for correlationId: {} (topic: {}, partition: {}, offset: {})",
                    correlationId, topic, partition, offset);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) event.get("result");

            if (result == null) {
                log.error("❌ Wallet completion event missing 'result' field for correlationId: {}", correlationId);
                return;
            }

            if (correlationId == null) {
                log.error("❌ Wallet completion event missing 'correlationId' field");
                return;
            }

            // Extract wallet info from result (including sessionCookie for credential acceptance)
            StudentWalletService.StudentWalletInfo walletInfo =
                StudentWalletService.StudentWalletInfo.builder()
                    .email((String) result.get("email"))
                    .walletId((String) result.get("walletId"))
                    .subjectDid((String) result.get("subjectDid"))
                    .sessionCookie((String) result.get("sessionCookie"))  // Required for accepting credentials into wallet
                    .build();

            // Log session cookie status for debugging
            if (walletInfo.getSessionCookie() == null || walletInfo.getSessionCookie().isEmpty()) {
                log.warn("⚠️  Session cookie is null/empty in wallet completion event for correlationId: {}", correlationId);
            } else {
                log.debug("✅ Session cookie included in wallet completion event (length: {})",
                        walletInfo.getSessionCookie().length());
            }

            // Complete the pending future
            CompletableFuture<StudentWalletService.StudentWalletInfo> future =
                pendingWalletRequests.remove(correlationId);
            if (future != null) {
                future.complete(walletInfo);
                log.info("✅ Completed wallet request for correlationId: {} (email: {}, walletId: {}, subjectDid: {})",
                        correlationId, walletInfo.getEmail(), walletInfo.getWalletId(), walletInfo.getSubjectDid());
            } else {
                log.warn("⚠️  No pending wallet request found for correlationId: {}. Available keys: {}",
                        correlationId, pendingWalletRequests.keySet());
            }
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Consumes wallet error events
     */
    @KafkaListener(
        topics = "wallet.error",
        groupId = "credential-service-wallet-error-group",
        containerFactory = "workflowKafkaListenerContainerFactory"
    )
    public void handleWalletError(@Payload Map<String, Object> event,
                                 @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                 @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                 @Header(KafkaHeaders.OFFSET) long offset) {
        
        String correlationId = (String) event.get("correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            String errorMessage = (String) event.get("errorMessage");
            String errorCode = (String) event.get("errorCode");

            log.error("📨 Received wallet error event for correlationId: {} - {} (code: {}, topic: {}, partition: {}, offset: {})",
                    correlationId, errorMessage, errorCode, topic, partition, offset);

            // Complete the pending future with exception
            if (correlationId != null) {
                CompletableFuture<StudentWalletService.StudentWalletInfo> future =
                    pendingWalletRequests.remove(correlationId);
                if (future != null) {
                    future.completeExceptionally(
                        new RuntimeException("Wallet workflow failed: " + errorMessage + " (code: " + errorCode + ")"));
                    log.info("✅ Completed wallet request with exception for correlationId: {}", correlationId);
                } else {
                    log.warn("⚠️  No pending wallet request found for correlationId: {} (error). Available keys: {}",
                            correlationId, pendingWalletRequests.keySet());
                }
            } else {
                log.error("❌ Wallet error event missing 'correlationId' field");
            }
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Consumes credential workflow requests from SIS Service
     */
    @KafkaListener(
        topics = "credential.requests",
        groupId = "credential-service-workflow-group",
        containerFactory = "workflowKafkaListenerContainerFactory"
    )
    public void handleCredentialWorkflowRequest(@Payload Map<String, Object> request,
                                              @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                              @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                              @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received credential workflow request from topic: {} (partition: {}, offset: {})",
                topic, partition, offset);

        // Idempotency guard: skip records that have already been processed (at-least-once safety).
        if (processedEventService.isDuplicate(topic, partition, offset, "credential-workflow")) {
            return;
        }

        String correlationId = (String) request.get("correlationId");
        if (correlationId != null) {
            MDC.put("correlationId", correlationId);
        }
        try {
            String userId = (String) request.get("userId");
            Object studentData = request.get("studentData");

            if (correlationId == null || userId == null) {
                log.error("Invalid workflow request - missing correlationId or userId: {}", request);
                publishWorkflowError(correlationId, "INVALID_REQUEST", "Missing required fields");
                return;
            }

            try {
                processCredentialWorkflow(correlationId, userId, studentData);
            } catch (Exception e) {
                log.error("Error processing credential workflow for correlationId: {}", correlationId, e);
                publishWorkflowError(correlationId, "WORKFLOW_ERROR", e.getMessage());
            }
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Processes the complete credential workflow
     * Issues W3C Verifiable Credentials based on student data from SIS Service
     */
    private void processCredentialWorkflow(String correlationId, String userId, Object studentData) {
        log.info("Starting W3C credential issuance workflow for user: {} (correlationId: {})", userId, correlationId);
        
        try {
            // Step 1: Validate student data (10%)
            publishWorkflowProgress(correlationId, "VALIDATING", 10, "Validating student data from Sis");
            validateStudentData(studentData);
            
            // Step 2: Prepare issuer credentials (20%)
            publishWorkflowProgress(correlationId, "PREPARING_ISSUER", 20, "Preparing institution issuer credentials");
            prepareIssuerCredentials();
            
            // Step 3: Ensure wallet exists and get student DID (40%)
            publishWorkflowProgress(correlationId, "WALLET_OPERATIONS", 40, "Ensuring wallet exists and retrieving student DID");
            
            // Step 4: Issue W3C credentials (80%)
            publishWorkflowProgress(correlationId, "ISSUING_CREDENTIALS", 80, "Issuing W3C Verifiable Credentials");
            Map<String, Object> credentialResult = issueCredentials(correlationId, userId, studentData);
            
            // Step 4: Complete workflow (100%)
            publishWorkflowProgress(correlationId, "COMPLETED", 100, "All credentials issued successfully");
            publishWorkflowCompleted(correlationId, credentialResult);
            
            log.info("✅ Credential workflow completed successfully for user: {} (correlationId: {}). Issued {} credentials.", 
                    userId, correlationId, credentialResult.get("credentialsIssued"));
            
        } catch (Exception e) {
            log.error("❌ Credential workflow failed for user: {} (correlationId: {}): {}", 
                    userId, correlationId, e.getMessage(), e);
            publishWorkflowError(correlationId, "WORKFLOW_FAILED", e.getMessage());
        }
    }
    
    /**
     * Validates student data
     */
    private void validateStudentData(Object studentData) throws Exception {
        if (studentData == null) {
            throw new IllegalArgumentException("Student data is required");
        }
        
        // Convert to Map if needed
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (studentData instanceof Map) ? 
            (Map<String, Object>) studentData : 
            objectMapper.convertValue(studentData, Map.class);
        
        // Validate required fields
        String studentId = com.example.dcs.credential.mapper.StudentDataMapper.extractString(dataMap, "studentId", "studentCode", "id");
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID is required in student data");
        }
        
        log.debug("Student data validation completed for student: {}", studentId);
    }
    
    /**
     * Prepares issuer credentials (gets or creates)
     */
    private void prepareIssuerCredentials() throws Exception {
        try {
            issuerKeyService.getOrCreateIssuerCredentials();
            log.debug("Issuer credentials ready");
        } catch (Exception e) {
            log.error("Failed to prepare issuer credentials: {}", e.getMessage(), e);
            throw new Exception("Could not prepare issuer credentials: " + e.getMessage());
        }
    }
    
    /**
     * Issues W3C Verifiable Credentials for the student
     * NEW: Uses generic template-based system - credentials are defined in application.yml
     * NEW: Integrates with student wallet - each student has their own wallet and DID
     */
    private Map<String, Object> issueCredentials(String correlationId, String userId, Object studentData) throws Exception {
        log.info("🚀 Issuing W3C credentials for user: {} using {} template(s)", 
            userId, credentialTemplateConfig.getEnabledTemplates().size());
        
        // Convert student data to Map
        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (studentData instanceof Map) ? 
            (Map<String, Object>) studentData : 
            objectMapper.convertValue(studentData, Map.class);
        
        // Get issuer credentials
        String issuerDid = issuerKeyService.getIssuerDid();
        Object issuerKeyObj = issuerKeyService.getIssuerKey();
        IssuerKey issuerKey = convertToIssuerKey(issuerKeyObj);
        
        // Extract student ID for reference
        String studentId = com.example.dcs.credential.mapper.StudentDataMapper.extractString(
            dataMap, "studentId", "studentCode", "id");
        
        // NEW: Publish wallet workflow request via Kafka and wait for completion (fully async)
        log.info("📱 Publishing wallet workflow request via Kafka (correlationId: {})...", correlationId);
        String walletCorrelationId = correlationId + "-wallet";
        
        // Create future to wait for wallet completion
        CompletableFuture<StudentWalletService.StudentWalletInfo> walletFuture = new CompletableFuture<>();
        pendingWalletRequests.put(walletCorrelationId, walletFuture);
        log.debug("📝 Stored pending wallet request with correlationId: {} (total pending: {})", 
                walletCorrelationId, pendingWalletRequests.size());
        
        // Publish wallet request to Kafka
        try {
            walletWorkflowProducer.publishWalletRequest(
                walletCorrelationId, userId, "ENSURE_WALLET", dataMap);
            log.info("✅ Published wallet workflow request to Kafka (walletCorrelationId: {})", walletCorrelationId);
        } catch (Exception e) {
            // Clean up on publish failure
            pendingWalletRequests.remove(walletCorrelationId);
            log.error("❌ Failed to publish wallet workflow request: {}", e.getMessage(), e);
            throw new Exception("Failed to publish wallet workflow request: " + e.getMessage(), e);
        }
        
        // Wait for wallet completion (with timeout)
        log.info("📱 Waiting for wallet workflow to complete (walletCorrelationId: {}, timeout: 30s)...", walletCorrelationId);
        StudentWalletService.StudentWalletInfo walletInfo;
        try {
            walletInfo = walletFuture.get(30, TimeUnit.SECONDS);
            log.info("✅ Wallet workflow completed successfully, retrieved wallet info (email: {}, walletId: {}, subjectDid: {})", 
                    walletInfo.getEmail(), walletInfo.getWalletId(), walletInfo.getSubjectDid());
        } catch (java.util.concurrent.TimeoutException e) {
            // Clean up
            pendingWalletRequests.remove(walletCorrelationId);
            log.error("❌ Wallet workflow timed out after 30 seconds for walletCorrelationId: {}", walletCorrelationId);
            // Fallback: try synchronous call as backup
            log.warn("⚠️  Falling back to synchronous wallet operation");
            walletInfo = studentWalletService.ensureWalletExists(dataMap);
        } catch (Exception e) {
            // Clean up
            pendingWalletRequests.remove(walletCorrelationId);
            log.error("❌ Wallet workflow failed for walletCorrelationId: {} - {}", walletCorrelationId, e.getMessage(), e);
            // Fallback: try synchronous call as backup
            log.warn("⚠️  Falling back to synchronous wallet operation");
            try {
                walletInfo = studentWalletService.ensureWalletExists(dataMap);
            } catch (Exception fallbackException) {
                log.error("❌ Synchronous wallet fallback also failed: {}", fallbackException.getMessage(), fallbackException);
                throw new Exception("Both async and sync wallet operations failed. Last error: " + fallbackException.getMessage(), fallbackException);
            }
        }
        
        // Use student's DID from their wallet (not generated)
        String subjectDid = walletInfo.getSubjectDid();
        
        if (subjectDid == null || subjectDid.isEmpty()) {
            log.warn("⚠️  No DID found in student wallet. Wallet may need initialization.");
            log.warn("⚠️  Generating fallback DID (credentials will need manual wallet linking)");
            // Fallback: generate DID if wallet doesn't have one yet
            subjectDid = "did:jwk:" + generateDidSuffix(studentId);
        } else {
            log.info("✅ Using student's DID from wallet: {}", subjectDid);
        }
        
        List<String> credentialOfferUrls = new ArrayList<>();
        List<String> issuedCredentialTypes = new ArrayList<>();
        List<String> skippedCredentialTypes = new ArrayList<>();
        List<String> failedCredentialTypes = new ArrayList<>();
        
        // Get all enabled templates, sorted by priority
        List<CredentialTemplate> templates = credentialTemplateConfig.getEnabledTemplates();
        
        log.info("📋 Processing {} enabled credential template(s)", templates.size());
        
        // Issue credentials for each enabled template
        for (CredentialTemplate template : templates) {
            try {
                // Check condition if specified
                if (!genericCredentialBuilder.evaluateCondition(template.getCondition(), dataMap)) {
                    log.info("⏭️  Skipping {} - condition not met: {}", 
                        template.getType(), template.getCondition());
                    skippedCredentialTypes.add(template.getType());
                    continue;
                }
                
                // Check if credential should be issued (check for existing and expiration)
                boolean shouldIssue = credentialExpirationChecker.shouldIssueCredential(
                    walletInfo.getWalletId(),
                    template.getType(),
                    walletInfo.getSessionCookie(),
                    walletInfo.getEmail()
                );
                
                if (!shouldIssue) {
                    log.info("⏭️  Skipping {} - valid credential already exists in wallet", template.getType());
                    skippedCredentialTypes.add(template.getType());
                    continue;
                }
                
                log.info("📝 Issuing credential: {} ({})", template.getType(), template.getDisplayName());
                
                // Build credential data using template
                Map<String, Object> credentialData = genericCredentialBuilder.buildCredential(
                    template, dataMap, subjectDid, issuerDid);

                // --- ADR 0007 follow-up: make the issued VC revocation-aware ---------
                // Register the credential in the status registry FIRST so we obtain a
                // stable id + statusListIndex, then embed a W3C `credentialStatus`
                // entry (BitstringStatusListEntry) in the credential data walt.id will
                // sign. A verifier reading the signed VC is then directed to
                // GET /api/v1/status-list/{listId} to resolve revocation.
                // Failure-isolated: any registry/embedding error must NOT break issuance.
                String credentialId = java.util.UUID.randomUUID().toString();
                try {
                    var statusEntity = credentialStatusService.record(
                            credentialId, template.getType(), subjectDid);
                    embedCredentialStatus(credentialData, statusEntity.getStatusListIndex());
                } catch (Exception statusException) {
                    log.warn("⚠️  Failed to embed credentialStatus for {} (issuance continues without it): {}",
                            template.getType(), statusException.getMessage());
                }

                // Build credential mapping for dynamic fields (id, timestamps, DIDs)
                // Note: We don't include issuer mapping since issuer is already set correctly in credentialData
                // The mapping would override it with <issuerDid> placeholder which might not be replaced correctly
                Map<String, Object> subjectMapping = new HashMap<>();
                subjectMapping.put("id", "<subjectDid>");
                com.example.dcs.credential.dto.waltid.CredentialMapping mapping = 
                    com.example.dcs.credential.dto.waltid.CredentialMapping.builder()
                        .id("<uuid>")
                        .credentialSubject(subjectMapping)
                        .issuanceDate("<timestamp>")
                        .expirationDate("<timestamp-in:365d>")
                        .build();
                
                // Build selective disclosure configuration
                Map<String, Object> selectiveDisclosure = buildGenericSelectiveDisclosure(credentialData);
                
                // Create issuance request with selective disclosure
                IssueCredentialRequest request = IssueCredentialRequest.builder()
                    .issuerKey(issuerKey)
                    .issuerDid(issuerDid)
                    .credentialConfigurationId(template.getWaltidConfigId())
                    .credentialData(credentialData)
                    .mapping(mapping)  // Required for dynamic value insertion (<uuid>, <timestamp>, etc.)
                    .authenticationMethod("PRE_AUTHORIZED")
                    .standardVersion("DRAFT13")
                    .selectiveDisclosure(selectiveDisclosure)
                    .build();
                
                // Issue credential via WaltID using SD-JWT for selective disclosure support
                String offerUrl = waltidService.issueSdJwtCredential(request);
                
                // Accept credential offer into student's wallet
                try {
                    log.info("💾 Accepting credential offer into wallet for {}...", template.getType());
                    waltidService.acceptCredentialOffer(
                        walletInfo.getWalletId(),
                        subjectDid,
                        offerUrl,
                        walletInfo.getSessionCookie(),
                        walletInfo.getEmail()
                    );
                    log.info("✅ {} accepted into wallet successfully", template.getType());
                    
                    // Record issuance in cache to prevent immediate duplicates
                    credentialExpirationChecker.recordIssuance(walletInfo.getWalletId(), template.getType());
                } catch (Exception acceptException) {
                    log.error("❌ Failed to accept {} into wallet: {}", template.getType(), acceptException.getMessage(), acceptException);
                    // Don't fail the entire workflow if acceptance fails - credential was issued
                    // The user can manually accept it later via the offer URL
                    log.warn("⚠️  Credential {} was issued but not automatically stored in wallet. Offer URL available for manual acceptance.", template.getType());
                }
                
                credentialOfferUrls.add(offerUrl);
                issuedCredentialTypes.add(template.getType());

                // Ensure the issued credential is registered as VALID in the status
                // registry. This is idempotent by id: if it was already recorded above
                // (to embed the credentialStatus entry) this is a no-op; if that step
                // failed, this is the fallback so the credential is still trackable.
                // Failure-isolated: a registry error must never break the issuance happy path.
                try {
                    credentialStatusService.record(credentialId, template.getType(), subjectDid);
                } catch (Exception statusException) {
                    log.warn("⚠️  Failed to record credential status for {} (issuance still succeeded): {}",
                            template.getType(), statusException.getMessage());
                }

                log.info("✅ {} issued and stored in wallet successfully", template.getType());
                
            } catch (Exception e) {
                String errorMsg = String.format("Failed to issue %s: %s", 
                    template.getType(), e.getMessage());
                log.error(errorMsg, e);
                failedCredentialTypes.add(template.getType());
                
                // If required credential fails, throw exception
                if (template.isRequired()) {
                    throw new Exception("Required credential " + template.getType() + " failed: " + e.getMessage());
                }
            }
        }
        
        // Calculate total credential types attempted
        List<String> allCredentialTypes = new ArrayList<>();
        allCredentialTypes.addAll(issuedCredentialTypes);
        allCredentialTypes.addAll(skippedCredentialTypes);
        allCredentialTypes.addAll(failedCredentialTypes);
        
        // Build result
        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("userId", userId);  // Add userId for reference
        result.put("subjectDid", subjectDid);
        result.put("credentialsIssued", credentialOfferUrls.size());
        result.put("issuedCredentialTypes", issuedCredentialTypes);  // ✅ Successfully issued
        result.put("credentialTypes", allCredentialTypes);  // All types attempted
        result.put("credentialOfferUrls", credentialOfferUrls);
        result.put("skippedCredentialTypes", skippedCredentialTypes);
        result.put("failedCredentialTypes", failedCredentialTypes);
        result.put("issuedAt", System.currentTimeMillis());
        result.put("status", "COMPLETED");
        result.put("institutionName", dataMap.getOrDefault("institutionName", "DCS"));
        result.put("institutionPIC", dataMap.getOrDefault("institutionPIC", "997605425"));
        
        // Add summary statistics
        Map<String, Integer> summary = new HashMap<>();
        summary.put("total", allCredentialTypes.size());
        summary.put("issued", issuedCredentialTypes.size());
        summary.put("skipped", skippedCredentialTypes.size());
        summary.put("failed", failedCredentialTypes.size());
        result.put("summary", summary);
        
        log.info("✅ Issued {} credential(s) for student {} ({} skipped, {} failed)", 
            credentialOfferUrls.size(), studentId, 
            skippedCredentialTypes.size(), failedCredentialTypes.size());
        
        return result;
    }
    
    /**
     * Converts issuer key object to IssuerKey DTO
     */
    private IssuerKey convertToIssuerKey(Object issuerKeyObj) {
        if (issuerKeyObj instanceof IssuerKey) {
            return (IssuerKey) issuerKeyObj;
        }
        
        if (issuerKeyObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> keyMap = (Map<String, Object>) issuerKeyObj;
            return objectMapper.convertValue(keyMap, IssuerKey.class);
        }
        
        // Fallback: try to convert
        return objectMapper.convertValue(issuerKeyObj, IssuerKey.class);
    }
    
    /**
     * Helper: Generate DID suffix from student ID
     */
    private String generateDidSuffix(String studentId) {
        // In production, use proper DID generation or let students create their own DIDs
        // For now, using a hash-based approach
        if (studentId == null) {
            studentId = "student-" + System.currentTimeMillis();
        }
        
        // Simple base64-like encoding of student ID
        return java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("student:" + studentId).getBytes());
    }
    
    /**
     * Builds selective disclosure configuration for generic credentials
     * Creates a general-purpose configuration that makes most fields selectively disclosable
     */
    private Map<String, Object> buildGenericSelectiveDisclosure(Map<String, Object> credentialData) {
        Map<String, Object> sdConfig = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        
        // IssuanceDate - can be selectively disclosed
        Map<String, Object> issuanceDate = new HashMap<>();
        issuanceDate.put("sd", true);
        fields.put("issuanceDate", issuanceDate);
        
        // CredentialSubject - has nested fields
        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("sd", false);  // Don't hide the whole subject
        
        Map<String, Object> children = new HashMap<>();
        Map<String, Object> subjectFields = new HashMap<>();
        
        // Get the actual credentialSubject from the credential data
        @SuppressWarnings("unchecked")
        Map<String, Object> actualSubject = (Map<String, Object>) credentialData.get("credentialSubject");
        if (actualSubject != null) {
            // Make all fields in credentialSubject selectively disclosable
            for (String key : actualSubject.keySet()) {
                // Skip the 'id' field as it should always be disclosed
                if ("id".equals(key)) {
                    continue;
                }
                
                Object value = actualSubject.get(key);
                
                // If the value is a map (nested object), handle it recursively
                if (value instanceof Map) {
                    Map<String, Object> nestedField = new HashMap<>();
                    nestedField.put("sd", false);  // Don't hide the whole nested object
                    
                    Map<String, Object> nestedChildren = new HashMap<>();
                    Map<String, Object> nestedFields = new HashMap<>();
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedMap = (Map<String, Object>) value;
                    for (String nestedKey : nestedMap.keySet()) {
                        Map<String, Object> innerField = new HashMap<>();
                        innerField.put("sd", true);
                        nestedFields.put(nestedKey, innerField);
                    }
                    
                    nestedChildren.put("fields", nestedFields);
                    nestedField.put("children", nestedChildren);
                    subjectFields.put(key, nestedField);
                } else {
                    // Regular field - make it selectively disclosable
                    Map<String, Object> field = new HashMap<>();
                    field.put("sd", true);
                    subjectFields.put(key, field);
                }
            }
        }
        
        children.put("fields", subjectFields);
        credentialSubject.put("children", children);
        fields.put("credentialSubject", credentialSubject);
        
        sdConfig.put("fields", fields);
        return sdConfig;
    }

    /**
     * Embeds a W3C {@code credentialStatus} entry (BitstringStatusListEntry) into the
     * credential data that walt.id will sign, so the issued VC is revocation-aware
     * and points a verifier at this service's Bitstring Status List (ADR 0007).
     *
     * <p>The entry follows the
     * <a href="https://www.w3.org/TR/vc-bitstring-status-list/">W3C Bitstring Status
     * List</a> shape:
     * <pre>
     * "credentialStatus": {
     *   "id": "{baseUrl}/status-list/{listId}#{index}",
     *   "type": "BitstringStatusListEntry",
     *   "statusPurpose": "revocation",
     *   "statusListIndex": "{index}",
     *   "statusListCredential": "{baseUrl}/status-list/{listId}"
     * }
     * </pre>
     * Skips silently if the credential data is missing/immutable.</p>
     *
     * @param credentialData   the mutable credential map passed to walt.id
     * @param statusListIndex  this credential's bit position in the status list
     */
    private void embedCredentialStatus(Map<String, Object> credentialData, long statusListIndex) {
        if (credentialData == null) {
            return;
        }
        String statusListCredential = statusBaseUrl + "/status-list/" + statusListId;

        Map<String, Object> credentialStatus = new java.util.LinkedHashMap<>();
        credentialStatus.put("id", statusListCredential + "#" + statusListIndex);
        credentialStatus.put("type", "BitstringStatusListEntry");
        credentialStatus.put("statusPurpose", "revocation");
        credentialStatus.put("statusListIndex", String.valueOf(statusListIndex));
        credentialStatus.put("statusListCredential", statusListCredential);

        try {
            credentialData.put("credentialStatus", credentialStatus);
            log.info("🔗 Embedded credentialStatus (index={}) -> {}", statusListIndex, statusListCredential);
        } catch (UnsupportedOperationException immutable) {
            log.warn("⚠️  credentialData is immutable; skipping credentialStatus embedding");
        }
    }

    /**
     * Publishes credential progress events (standardized topic name)
     */
    private void publishWorkflowProgress(String correlationId, String status, int progress, String message) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("status", status);
        event.put("progress", progress);
        event.put("message", message);
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send("credential.progress", correlationId, event);
            log.debug("Published progress to credential.progress topic (correlationId: {})", correlationId);
        } catch (Exception e) {
            log.error("Failed to publish progress for correlationId: {}", correlationId, e);
        }
    }
    
    /**
     * Publishes credential completion events (standardized topic name)
     */
    private void publishWorkflowCompleted(String correlationId, Object result) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("status", "COMPLETED");
        event.put("progress", 100);
        event.put("message", "Credentials issued successfully");
        event.put("result", result);
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send("credential.completed", correlationId, event);
            log.info("Published completion to credential.completed topic (correlationId: {})", correlationId);
        } catch (Exception e) {
            log.error("Failed to publish completion for correlationId: {}", correlationId, e);
        }
    }
    
    /**
     * Publishes credential error events (standardized topic name)
     */
    private void publishWorkflowError(String correlationId, String errorCode, String errorMessage) {
        Map<String, Object> event = new HashMap<>();
        event.put("correlationId", correlationId);
        event.put("status", "FAILED");
        event.put("errorCode", errorCode);
        event.put("errorMessage", errorMessage);
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send("credential.error", correlationId, event);
            log.error("Published error to credential.error topic (correlationId: {})", correlationId);
        } catch (Exception e) {
            log.error("Failed to publish error for correlationId: {}", correlationId, e);
        }
    }
}
