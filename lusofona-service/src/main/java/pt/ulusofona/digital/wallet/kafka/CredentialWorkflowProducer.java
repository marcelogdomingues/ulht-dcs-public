package pt.ulusofona.digital.wallet.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pt.ulusofona.digital.wallet.domain.workflow.CredentialWorkflowRequest;

import java.time.OffsetDateTime;

/**
 * Kafka producer for sending credential workflow requests to the Credential Service.
 * This completes the missing integration between ULHT Proxy and Credential Service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialWorkflowProducer {
    
    private final KafkaTemplate<String, CredentialWorkflowRequest> kafkaTemplate;
    
    private static final String CREDENTIAL_REQUESTS_TOPIC = "credential.requests";
    
    /**
     * Sends a credential workflow request to the Credential Service
     * 
     * @param correlationId The correlation ID from Student Service (must be preserved!)
     * @param userId The user ID
     * @param studentData The student data from external ULHT services
     * @return The correlation ID for tracking the workflow
     */
    public String startCredentialWorkflow(String correlationId, String userId, Object studentData) {
        // IMPORTANT: Use the correlationId from Student Service, don't create a new one!
        // This ensures end-to-end traceability
        
        CredentialWorkflowRequest request = CredentialWorkflowRequest.builder()
            .correlationId(correlationId)
            .userId(userId)
            .studentData(studentData)
            .timestamp(OffsetDateTime.now())
            .source("ulht-proxy")
            .workflowType("STUDENT_CREDENTIAL_ISSUANCE")
            .priority("NORMAL")
            .build();
        
        try {
            kafkaTemplate.send(CREDENTIAL_REQUESTS_TOPIC, correlationId, request);
            log.info("Sent credential workflow request for user: {} with correlationId: {}", userId, correlationId);
            return correlationId;
        } catch (Exception e) {
            log.error("Failed to send credential workflow request for user: {}", userId, e);
            throw new RuntimeException("Failed to start credential workflow", e);
        }
    }
}
