package pt.ulusofona.student.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import pt.ulusofona.student.generated.model.LoginRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * Publishes student login requests to Kafka
 * This triggers the entire workflow automatically!
 * 
 * Uses generated LoginRequest model from OpenAPI specification.
 */
@Slf4j
@Service
public class StudentLoginProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.student-login-requested}")
    private String studentLoginTopic;
    
    @Value("${kafka.producer.send-timeout:10}")
    private int sendTimeoutSeconds;
    
    @Value("${kafka.producer.synchronous:true}")
    private boolean synchronousSend;
    
    public StudentLoginProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes student login to Kafka
     * 
     * @param correlationId Unique identifier for tracking
     * @param request Student login data (generated model)
     * @throws pt.ulusofona.student.exception.StudentServiceException if Kafka publishing fails
     */
    public void publishStudentLogin(String correlationId, LoginRequest request) {
        log.info("📤 Publishing student login to Kafka: correlationId={}, userName={}", 
                 correlationId, request.getUserName());

        try {
            // Create message payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("correlationId", correlationId);
            payload.put("userName", request.getUserName());
            payload.put("installKey", request.getInstallKey());
            payload.put("platform", request.getPlatform());
            payload.put("language", request.getLanguage());
            payload.put("application", request.getApplication());
            payload.put("versionCode", request.getVersionCode());
            payload.put("timestamp", System.currentTimeMillis());

            // Build message with headers
            Message<Map<String, Object>> message = MessageBuilder
                    .withPayload(payload)
                    .setHeader(KafkaHeaders.TOPIC, studentLoginTopic)
                    .setHeader(KafkaHeaders.KEY, correlationId)
                    .setHeader("X-Correlation-ID", correlationId)
                    .build();

            // Send to Kafka
            if (synchronousSend) {
                // Synchronous: Wait for confirmation (reliable but slower)
                kafkaTemplate.send(message).get(sendTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
                log.info("✅ Student login published successfully to topic: {} (sync, timeout: {}s)", 
                         studentLoginTopic, sendTimeoutSeconds);
            } else {
                // Asynchronous: Fire and forget (fast but unreliable)
                kafkaTemplate.send(message);
                log.info("✅ Student login sent to Kafka topic: {} (async, no confirmation)", 
                         studentLoginTopic);
            }
            
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("❌ Timeout publishing student login to Kafka: correlationId={}, timeout={}s, error={}", 
                     correlationId, sendTimeoutSeconds, e.getMessage(), e);
            throw new pt.ulusofona.student.exception.StudentServiceException(
                pt.ulusofona.student.exception.ErrorCodes.KAFKA_PRODUCE_ERROR,
                String.format("Timeout publishing to Kafka after %d seconds - message may not have been delivered. " +
                             "Consider increasing kafka.producer.send-timeout if network is slow.", 
                             sendTimeoutSeconds),
                e
            );
        } catch (Exception e) {
            log.error("❌ Failed to publish student login to Kafka: correlationId={}, error={}", 
                     correlationId, e.getMessage(), e);
            throw new pt.ulusofona.student.exception.StudentServiceException(
                pt.ulusofona.student.exception.ErrorCodes.KAFKA_PRODUCE_ERROR,
                "Failed to publish student login request to Kafka",
                e
            );
        }
    }
}

