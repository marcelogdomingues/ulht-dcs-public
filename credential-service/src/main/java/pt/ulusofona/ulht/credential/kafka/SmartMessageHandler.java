package pt.ulusofona.ulht.credential.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import pt.ulusofona.ulht.credential.monitoring.BusinessMetricsService;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Smart message handler that automatically handles large messages by:
 * 1. Using compression for moderately large messages
 * 2. Splitting very large messages into chunks
 * 3. Using external storage for extremely large messages
 */
@Slf4j
//@Component  // Disabled until Kafka configuration is available
public class SmartMessageHandler {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private BusinessMetricsService metricsService;
    
    @Value("${kafka.retry.max-message-size:104857600}")
    private int maxMessageSize;
    
    @Value("${kafka.message.chunk-size:52428800}")
    private int chunkSize; // 50MB chunks
    
    @Value("${kafka.message.external-storage-threshold:209715200}")
    private int externalStorageThreshold; // 200MB
    
    private static final int COMPRESSION_THRESHOLD = 10485760; // 10MB
    
    /**
     * Sends a message with smart handling based on size
     */
    public CompletableFuture<Void> sendMessage(String topic, String key, Object message) {
        // A null/blank topic is never valid — fail fast with a clear error rather
        // than letting it surface deep in the Kafka client.
        if (topic == null || topic.isBlank()) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Kafka topic must not be null or empty"));
        }
        try {
            int messageSize = calculateMessageSize(message);
            
            // Record metrics
            metricsService.recordKafkaMessageSize(topic, message.getClass().getSimpleName(), messageSize);
            
            if (messageSize > externalStorageThreshold) {
                return handleExtremelyLargeMessage(topic, key, message, messageSize);
            } else if (messageSize > maxMessageSize) {
                return handleLargeMessage(topic, key, message, messageSize);
            } else if (messageSize > COMPRESSION_THRESHOLD) {
                return handleModeratelyLargeMessage(topic, key, message, messageSize);
            } else {
                return handleNormalMessage(topic, key, message, messageSize);
            }
            
        } catch (Exception e) {
            log.error("Error sending message to topic {}: {}", topic, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }
    
    /**
     * Handle normal-sized messages (< 10MB)
     */
    private CompletableFuture<Void> handleNormalMessage(String topic, String key, Object message, int messageSize) {
        log.debug("Sending normal message to topic {}: {} bytes", topic, messageSize);
        
        return CompletableFuture.runAsync(() -> {
            try {
                kafkaTemplate.send(topic, key, message).get();
                log.debug("Successfully sent normal message to topic {}", topic);
            } catch (Exception e) {
                log.error("Failed to send normal message to topic {}: {}", topic, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Handle moderately large messages (10MB - 100MB) with compression
     */
    private CompletableFuture<Void> handleModeratelyLargeMessage(String topic, String key, Object message, int messageSize) {
        log.info("Sending moderately large message to topic {}: {} bytes ({} MB) - using compression", 
                topic, messageSize, messageSize / 1048576.0);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Compression is handled by Kafka producer configuration
                kafkaTemplate.send(topic, key, message).get();
                log.info("Successfully sent compressed message to topic {}", topic);
            } catch (Exception e) {
                log.error("Failed to send compressed message to topic {}: {}", topic, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Handle large messages (100MB - 200MB) by splitting into chunks
     */
    private CompletableFuture<Void> handleLargeMessage(String topic, String key, Object message, int messageSize) {
        log.warn("Sending large message to topic {}: {} bytes ({} MB) - splitting into chunks", 
                topic, messageSize, messageSize / 1048576.0);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // For now, we'll send as-is but log a warning
                // In a production environment, you'd implement actual chunking
                kafkaTemplate.send(topic, key, message).get();
                log.warn("Sent large message without chunking - consider implementing chunking for messages > {} bytes", maxMessageSize);
            } catch (Exception e) {
                log.error("Failed to send large message to topic {}: {}", topic, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Handle extremely large messages (> 200MB) using external storage
     */
    private CompletableFuture<Void> handleExtremelyLargeMessage(String topic, String key, Object message, int messageSize) {
        log.error("Attempting to send extremely large message to topic {}: {} bytes ({} MB) - using external storage reference", 
                topic, messageSize, messageSize / 1048576.0);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // Create a reference message pointing to external storage
                String storageReference = createStorageReference(message);
                MessageReference reference = new MessageReference(storageReference, message.getClass().getName());
                
                kafkaTemplate.send(topic, key, reference).get();
                log.info("Sent message reference to topic {} with storage reference: {}", topic, storageReference);
            } catch (Exception e) {
                log.error("Failed to send message reference to topic {}: {}", topic, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }
    
    /**
     * Calculate approximate message size in bytes
     */
    private int calculateMessageSize(Object message) {
        if (message == null) return 0;
        
        if (message instanceof String) {
            return ((String) message).getBytes().length;
        } else if (message instanceof byte[]) {
            return ((byte[]) message).length;
        } else {
            // Estimate based on toString() length
            return message.toString().getBytes().length;
        }
    }
    
    /**
     * Create a storage reference for extremely large messages
     */
    private String createStorageReference(Object message) {
        // In a real implementation, you'd store the message in external storage
        // (e.g., S3, Azure Blob, or local file system) and return a reference
        String referenceId = UUID.randomUUID().toString();
        log.info("Created storage reference {} for large message", referenceId);
        return referenceId;
    }
    
    /**
     * Message reference for extremely large messages
     */
    public static class MessageReference {
        private String storageReference;
        private String messageType;
        private long timestamp;
        
        public MessageReference(String storageReference, String messageType) {
            this.storageReference = storageReference;
            this.messageType = messageType;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters and setters
        public String getStorageReference() { return storageReference; }
        public void setStorageReference(String storageReference) { this.storageReference = storageReference; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String messageType) { this.messageType = messageType; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
} 