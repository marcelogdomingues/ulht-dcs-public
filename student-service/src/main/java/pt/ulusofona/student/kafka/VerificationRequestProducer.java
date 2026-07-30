package pt.ulusofona.student.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka producer for verification requests.
 * Publishes verification requests to the verification.requested topic.
 */
@Slf4j
@Component
public class VerificationRequestProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "verification.requested";
    
    @org.springframework.beans.factory.annotation.Value("${kafka.producer.send-timeout:10}")
    private int sendTimeoutSeconds;
    
    @org.springframework.beans.factory.annotation.Value("${kafka.producer.synchronous:true}")
    private boolean synchronousSend;
    
    public VerificationRequestProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publishes a verification request to Kafka.
     * This triggers the verification workflow in Credential Service.
     *
     * @param correlationId Unique correlation ID for tracking
     * @param credentialType Type of credential to verify
     * @param format Credential format (jwt_vc_json, sd_jwt_vc, mso_mdoc)
     * @param userId User ID for tracking
     * @param vpPolicies Policies for Verifiable Presentation
     * @param vcPolicies Policies for Verifiable Credentials
     * @param successRedirectUri Optional success redirect URL
     * @param errorRedirectUri Optional error redirect URL
     * @param statusCallbackUri Optional status callback URL
     * @param statusCallbackApiKey Optional status callback API key
     * @param customStateId Optional custom state ID
     */
    public void publishVerificationRequest(
            String correlationId,
            String credentialType,
            String format,
            String userId,
            List<String> vpPolicies,
            List<String> vcPolicies,
            String successRedirectUri,
            String errorRedirectUri,
            String statusCallbackUri,
            String statusCallbackApiKey,
            String customStateId) {

        log.info("📤 Publishing verification request to Kafka: correlationId={}, type={}, userId={}",
                correlationId, credentialType, userId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("correlationId", correlationId);
        payload.put("credentialType", credentialType);
        payload.put("format", format != null ? format : "jwt_vc_json");
        payload.put("userId", userId);
        
        if (vpPolicies != null && !vpPolicies.isEmpty()) {
            payload.put("vpPolicies", vpPolicies);
        }
        if (vcPolicies != null && !vcPolicies.isEmpty()) {
            payload.put("vcPolicies", vcPolicies);
        }
        if (successRedirectUri != null) {
            payload.put("successRedirectUri", successRedirectUri);
        }
        if (errorRedirectUri != null) {
            payload.put("errorRedirectUri", errorRedirectUri);
        }
        if (statusCallbackUri != null) {
            payload.put("statusCallbackUri", statusCallbackUri);
        }
        if (statusCallbackApiKey != null) {
            payload.put("statusCallbackApiKey", statusCallbackApiKey);
        }
        if (customStateId != null) {
            payload.put("customStateId", customStateId);
        }

        try {
            // Send to Kafka
            if (synchronousSend) {
                // Synchronous: Wait for confirmation (reliable but slower)
                kafkaTemplate.send(TOPIC, correlationId, payload).get(sendTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
                log.info("✅ Verification request published successfully to topic: {} (sync, timeout: {}s)", 
                         TOPIC, sendTimeoutSeconds);
            } else {
                // Asynchronous: Fire and forget (fast but unreliable)
                kafkaTemplate.send(TOPIC, correlationId, payload);
                log.info("✅ Verification request sent to Kafka topic: {} (async, no confirmation)", TOPIC);
            }
        } catch (java.util.concurrent.TimeoutException e) {
            log.error("❌ Timeout publishing verification request to Kafka: correlationId={}, timeout={}s", 
                     correlationId, sendTimeoutSeconds, e);
            throw new RuntimeException(
                String.format("Timeout publishing verification request to Kafka after %d seconds - message may not have been delivered. " +
                             "Consider increasing kafka.producer.send-timeout if network is slow.", 
                             sendTimeoutSeconds), e);
        } catch (Exception e) {
            log.error("❌ Failed to publish verification request: correlationId={}", correlationId, e);
            throw new RuntimeException("Failed to publish verification request", e);
        }
    }
}

