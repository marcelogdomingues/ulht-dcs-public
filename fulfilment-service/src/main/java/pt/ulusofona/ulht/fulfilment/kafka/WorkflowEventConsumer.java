package pt.ulusofona.ulht.fulfilment.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import pt.ulusofona.ulht.fulfilment.service.FulfilmentService;

import java.util.Map;

/**
 * Kafka consumer for credential and verification workflow events.
 * Listens to STANDARDIZED topic names (credential.*, verification.*)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEventConsumer {
    
    private final FulfilmentService fulfilmentService;
    
    /**
     * Consumes credential progress events (STANDARDIZED TOPIC)
     */
    @KafkaListener(
        topics = "credential.progress",
        groupId = "fulfilment-service-workflow-group"
    )
    public void handleWorkflowProgress(@Payload Map<String, Object> event,
                                     @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                     @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                     @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received workflow progress event from topic: {} (partition: {}, offset: {})", 
                topic, partition, offset);
        
        try {
            String correlationId = (String) event.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            String status = (String) event.get("status");
            Integer progress = (Integer) event.get("progress");
            String message = (String) event.get("message");

            if (correlationId != null) {
                fulfilmentService.publishProgress(correlationId, status, progress, message);
                log.debug("Processed progress event for correlationId: {}", correlationId);
            } else {
                log.warn("Received workflow progress event without correlationId: {}", event);
            }
        } catch (Exception e) {
            log.error("Error processing workflow progress event: {}", event, e);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Consumes credential completion events (STANDARDIZED TOPIC)
     */
    @KafkaListener(
        topics = "credential.completed",
        groupId = "fulfilment-service-workflow-group"
    )
    public void handleWorkflowCompleted(@Payload Map<String, Object> event,
                                      @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                      @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                      @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received workflow completion event from topic: {} (partition: {}, offset: {})", 
                topic, partition, offset);
        
        try {
            String correlationId = (String) event.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            Object result = event.get("result");

            if (correlationId != null) {
                fulfilmentService.publishCompletion(correlationId, result);
                log.info("Processed completion event for correlationId: {}", correlationId);
            } else {
                log.warn("Received workflow completion event without correlationId: {}", event);
            }
        } catch (Exception e) {
            log.error("Error processing workflow completion event: {}", event, e);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Consumes credential error events (STANDARDIZED TOPIC)
     */
    @KafkaListener(
        topics = "credential.error",
        groupId = "fulfilment-service-workflow-group"
    )
    public void handleWorkflowError(@Payload Map<String, Object> event,
                                  @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                  @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                  @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("Received workflow error event from topic: {} (partition: {}, offset: {})", 
                topic, partition, offset);
        
        try {
            String correlationId = (String) event.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            String errorCode = (String) event.get("errorCode");
            String errorName = (String) event.get("errorName");
            String errorMessage = (String) event.get("errorMessage");

            if (correlationId != null) {
                fulfilmentService.publishError(correlationId, errorMessage, errorCode, errorName);
                log.error("Processed error event for correlationId: {} - {} ({})",
                         correlationId, errorCode, errorName);
            } else {
                log.warn("Received workflow error event without correlationId: {}", event);
            }
        } catch (Exception e) {
            log.error("Error processing workflow error event: {}", event, e);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    // ==================== VERIFICATION WORKFLOW EVENTS ====================
    
    /**
     * Consumes verification progress events (STANDARDIZED TOPIC)
     */
    @KafkaListener(
        topics = "verification.progress",
        groupId = "fulfilment-service-workflow-group"
    )
    public void handleVerificationProgress(@Payload Map<String, Object> event,
                                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                          @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("📨 Received verification progress event from topic: {} (partition: {}, offset: {})", 
                topic, partition, offset);
        
        try {
            String correlationId = (String) event.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            String status = (String) event.get("status");
            Integer progress = (Integer) event.get("progress");
            String message = (String) event.get("message");

            log.info("📥 Processing verification progress event: correlationId={}, status={}, progress={}%",
                    correlationId, status, progress);

            if (correlationId != null) {
                fulfilmentService.publishProgress(correlationId, status, progress, message);
                log.info("✅ Processed verification progress event for correlationId: {} - status: {}, progress: {}%",
                        correlationId, status, progress);
            } else {
                log.warn("⚠️ Received verification progress event without correlationId: {}", event);
            }
        } catch (Exception e) {
            log.error("❌ Error processing verification progress event: {}", event, e);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Consumes verification completion events (STANDARDIZED TOPIC)
     */
    @KafkaListener(
        topics = "verification.completed",
        groupId = "fulfilment-service-workflow-group"
    )
    public void handleVerificationCompleted(@Payload Map<String, Object> event,
                                           @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                           @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                           @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("📨 Received verification completion event from topic: {} (partition: {}, offset: {})", 
                topic, partition, offset);
        
        try {
            String correlationId = (String) event.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            Object result = event.get("result");

            if (correlationId != null) {
                fulfilmentService.publishCompletion(correlationId, result);
                log.info("✅ Processed verification completion event for correlationId: {}", correlationId);
            } else {
                log.warn("⚠️ Received verification completion event without correlationId: {}", event);
            }
        } catch (Exception e) {
            log.error("❌ Error processing verification completion event: {}", event, e);
        } finally {
            MDC.remove("correlationId");
        }
    }
    
    /**
     * Consumes verification error events (STANDARDIZED TOPIC)
     */
    @KafkaListener(
        topics = "verification.error",
        groupId = "fulfilment-service-workflow-group"
    )
    public void handleVerificationError(@Payload Map<String, Object> event,
                                       @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                                       @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.info("📨 Received verification error event from topic: {} (partition: {}, offset: {})", 
                topic, partition, offset);
        
        try {
            String correlationId = (String) event.get("correlationId");
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            String errorCode = (String) event.get("errorCode");
            String errorName = (String) event.get("errorName");
            String errorMessage = (String) event.get("errorMessage");

            if (correlationId != null) {
                fulfilmentService.publishError(correlationId, errorMessage, errorCode, errorName);
                log.error("✅ Processed verification error event for correlationId: {} - {} ({})",
                         correlationId, errorCode, errorName);
            } else {
                log.warn("⚠️ Received verification error event without correlationId: {}", event);
            }
        } catch (Exception e) {
            log.error("❌ Error processing verification error event: {}", event, e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
