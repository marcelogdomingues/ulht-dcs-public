package pt.ulusofona.digital.wallet.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import pt.ulusofona.digital.wallet.domain.workflow.CredentialWorkflowRequest;

import java.util.concurrent.CompletableFuture;

@Configuration
public class KafkaFallbackConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaFallbackConfig.class);

    @Bean
    @ConditionalOnMissingBean(name = "kafkaTemplate")
    public KafkaTemplate<String, String> kafkaTemplate() {
        logger.warn("Kafka is not configured. Using mock KafkaTemplate for development/testing.");
        
        return new KafkaTemplate<String, String>(null) {
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String data) {
                logger.info("Mock Kafka: Would send message to topic '" + topic + "': " + data);
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public CompletableFuture<SendResult<String, String>> send(String topic, String key, String data) {
                logger.info("Mock Kafka: Would send message to topic '" + topic + "' with key '" + key + "': " + data);
                return CompletableFuture.completedFuture(null);
            }
        };
    }
    
    @Bean
    @ConditionalOnMissingBean(name = "credentialWorkflowKafkaTemplate")
    public KafkaTemplate<String, CredentialWorkflowRequest> credentialWorkflowKafkaTemplate() {
        logger.warn("Kafka is not configured. Using mock CredentialWorkflowKafkaTemplate for development/testing.");
        
        return new KafkaTemplate<String, CredentialWorkflowRequest>(null) {
            @Override
            public CompletableFuture<SendResult<String, CredentialWorkflowRequest>> send(String topic, CredentialWorkflowRequest data) {
                String correlationId = data != null ? data.getCorrelationId() : "null";
                logger.info("Mock Kafka: Would send CredentialWorkflowRequest to topic '" + topic + "': correlationId=" + correlationId);
                return CompletableFuture.completedFuture(null);
            }
            
            @Override
            public CompletableFuture<SendResult<String, CredentialWorkflowRequest>> send(String topic, String key, CredentialWorkflowRequest data) {
                String correlationId = data != null ? data.getCorrelationId() : "null";
                logger.info("Mock Kafka: Would send CredentialWorkflowRequest to topic '" + topic + "' with key '" + key + "': correlationId=" + correlationId);
                return CompletableFuture.completedFuture(null);
            }
        };
    }
} 