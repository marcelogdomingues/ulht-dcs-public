package com.example.dcs.student.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import com.example.dcs.student.config.DataMaskerConfig;
import com.example.dcs.student.util.DataMasker;

import java.util.Map;

/**
 * Kafka interceptor that masks sensitive data in Kafka messages.
 * Works for both producer and consumer sides.
 */
@Slf4j
public class KafkaDataMaskingInterceptor implements ProducerInterceptor<String, Object>, ConsumerInterceptor<String, Object> {
    
    private DataMasker dataMasker;
    private DataMaskerConfig config;
    
    @Override
    public void configure(Map<String, ?> configs) {
        // This will be set by Spring via setter injection
    }
    
    public void setDataMasker(DataMasker dataMasker) {
        this.dataMasker = dataMasker;
    }
    
    public void setConfig(DataMaskerConfig config) {
        this.config = config;
    }
    
    // ========== Producer Interceptor ==========
    
    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        if (!config.isEnabled() || record.value() == null) {
            return record;
        }
        
        try {
            // Log the masked message
            if (log.isDebugEnabled()) {
                Object maskedValue = maskValue(record.value());
                log.debug("Kafka Producer - Topic: {}, Key: {}, Value: {}", 
                    record.topic(), record.key(), maskedValue);
            }
        } catch (Exception e) {
            log.warn("Failed to mask Kafka producer message: {}", e.getMessage());
        }
        
        // Don't modify the actual message, just log it masked
        return record;
    }
    
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (exception != null) {
            log.error("Kafka producer error for topic {}: {}", metadata != null ? metadata.topic() : "unknown", 
                exception.getMessage());
        }
    }
    
    // ========== Consumer Interceptor ==========
    
    @Override
    public ConsumerRecords<String, Object> onConsume(ConsumerRecords<String, Object> records) {
        if (!config.isEnabled()) {
            return records;
        }
        
        // Log masked messages
        if (log.isDebugEnabled()) {
            records.forEach(record -> {
                try {
                    Object maskedValue = maskValue(record.value());
                    log.debug("Kafka Consumer - Topic: {}, Partition: {}, Offset: {}, Key: {}, Value: {}", 
                        record.topic(), record.partition(), record.offset(), record.key(), maskedValue);
                } catch (Exception e) {
                    log.warn("Failed to mask Kafka consumer message: {}", e.getMessage());
                }
            });
        }
        
        // Don't modify the actual messages, just log them masked
        return records;
    }
    
    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        // No action needed
    }
    
    // ========== Common Methods ==========
    
    @SuppressWarnings("unchecked")
    private Object maskValue(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Map) {
            return dataMasker.maskMap((Map<String, Object>) value);
        } else if (value instanceof String) {
            return dataMasker.maskJson((String) value);
        } else {
            // For other types, try to convert to JSON string, mask, and parse back
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
                String json = mapper.writeValueAsString(value);
                String masked = dataMasker.maskJson(json);
                return mapper.readValue(masked, value.getClass());
            } catch (Exception e) {
                log.debug("Could not mask value of type {}: {}", value.getClass().getName(), e.getMessage());
                return value;
            }
        }
    }
    
    @Override
    public void close() {
        // No cleanup needed
    }
}





