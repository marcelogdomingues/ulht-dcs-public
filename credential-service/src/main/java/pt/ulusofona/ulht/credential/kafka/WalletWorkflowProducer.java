package pt.ulusofona.ulht.credential.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer for wallet workflow requests.
 * Publishes wallet operation requests to wallet.requests topic for async processing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletWorkflowProducer {

    private final KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate;

    /**
     * Publishes a wallet workflow request to Kafka
     * 
     * @param correlationId Correlation ID for tracking the workflow
     * @param userId User identifier
     * @param operationType Type of wallet operation (e.g., "ENSURE_WALLET")
     * @param studentData Student data map
     */
    public void publishWalletRequest(String correlationId, String userId, String operationType, Object studentData) {
        Map<String, Object> request = new HashMap<>();
        request.put("correlationId", correlationId);
        request.put("userId", userId);
        request.put("operationType", operationType);
        request.put("studentData", studentData);
        request.put("timestamp", Instant.now().toString());

        try {
            workflowKafkaTemplate.send("wallet.requests", correlationId, request);
            log.info("Published wallet workflow request to wallet.requests topic (correlationId: {}, operationType: {})",
                    correlationId, operationType);
        } catch (Exception e) {
            log.error("Failed to publish wallet workflow request for correlationId: {}", correlationId, e);
            throw new RuntimeException("Failed to publish wallet workflow request", e);
        }
    }
}
