package com.example.dcs.credential.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import com.example.dcs.credential.config.DataMaskerConfig;
import com.example.dcs.credential.util.DataMasker;

import java.util.Map;

/**
 * Kafka interceptor that masks sensitive data in Kafka messages.
 * Works for both producer and consumer sides.
 * 
 * Note: Kafka instantiates interceptors via reflection, so we use a static holder
 * pattern to access Spring-managed beans.
 */
@Slf4j
public class KafkaDataMaskingInterceptor implements ProducerInterceptor<String, Object>, ConsumerInterceptor<String, Object> {
    
    // Static holder for Spring-managed beans (set by DataMaskingConfiguration)
    private static volatile DataMasker staticDataMasker;
    private static volatile DataMaskerConfig staticConfig;
    
    @Override
    public void configure(Map<String, ?> configs) {
        // Kafka calls this, but we get beans from static holder
        if (staticDataMasker == null || staticConfig == null) {
            log.warn("KafkaDataMaskingInterceptor configured but DataMasker/Config not initialized. " +
                    "Make sure DataMaskingConfiguration bean is created.");
        }
    }
    
    /**
     * Sets the static DataMasker instance (called by Spring configuration)
     */
    public static void setStaticDataMasker(DataMasker dataMasker) {
        staticDataMasker = dataMasker;
    }
    
    /**
     * Sets the static DataMaskerConfig instance (called by Spring configuration)
     */
    public static void setStaticConfig(DataMaskerConfig config) {
        staticConfig = config;
    }
    
    // Legacy setters for backward compatibility (not used by Kafka)
    public void setDataMasker(DataMasker dataMasker) {
        setStaticDataMasker(dataMasker);
    }
    
    public void setConfig(DataMaskerConfig config) {
        setStaticConfig(config);
    }
    
    // ========== Producer Interceptor ==========
    
    @Override
    public ProducerRecord<String, Object> onSend(ProducerRecord<String, Object> record) {
        if (staticConfig == null || staticDataMasker == null || !staticConfig.isEnabled() || record.value() == null) {
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
        if (staticConfig == null || staticDataMasker == null || !staticConfig.isEnabled()) {
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
        if (value == null || staticDataMasker == null) {
            return value;
        }
        
        if (value instanceof Map) {
            // Recursively mask the Map, converting nested POJOs to Maps first
            return maskMapWithPojos((Map<String, Object>) value);
        } else if (value instanceof String) {
            return staticDataMasker.maskJson((String) value);
        } else {
            // For POJOs and other types, convert to Map, mask, then return as Map for logging
            // This ensures nested objects are properly masked
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
                // Convert POJO to Map
                Map<String, Object> mapValue = mapper.convertValue(value, Map.class);
                // Mask the Map recursively
                return maskMapWithPojos(mapValue);
            } catch (Exception e) {
                log.debug("Could not mask value of type {}: {}", value.getClass().getName(), e.getMessage());
                // Fallback: try to mask as JSON string
                try {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = 
                        new com.fasterxml.jackson.databind.ObjectMapper();
                    String json = mapper.writeValueAsString(value);
                    return staticDataMasker.maskJson(json);
                } catch (Exception e2) {
                    return value;
                }
            }
        }
    }
    
    /**
     * Recursively masks a Map, converting nested POJOs to Maps before masking
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> maskMapWithPojos(Map<String, Object> map) {
        if (map == null) {
            return map;
        }
        
        Map<String, Object> masked = new java.util.HashMap<>(map);
        com.fasterxml.jackson.databind.ObjectMapper mapper = 
            new com.fasterxml.jackson.databind.ObjectMapper();
        
        for (Map.Entry<String, Object> entry : masked.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Check if this field should be masked
            if (staticDataMasker != null && staticConfig != null && isSensitiveField(key)) {
                entry.setValue(staticConfig.getMaskString());
            } else if (value instanceof Map) {
                // Recursively mask nested Maps
                entry.setValue(maskMapWithPojos((Map<String, Object>) value));
            } else if (value instanceof java.util.List) {
                // Mask items in lists
                java.util.List<Object> maskedList = new java.util.ArrayList<>();
                for (Object item : (java.util.List<?>) value) {
                    if (item instanceof Map) {
                        maskedList.add(maskMapWithPojos((Map<String, Object>) item));
                    } else if (item != null && !isPrimitiveOrWrapper(item.getClass())) {
                        // Convert POJO to Map and mask
                        try {
                            Map<String, Object> itemMap = mapper.convertValue(item, Map.class);
                            maskedList.add(maskMapWithPojos(itemMap));
                        } catch (Exception e) {
                            maskedList.add(item);
                        }
                    } else {
                        maskedList.add(item);
                    }
                }
                entry.setValue(maskedList);
            } else if (value != null && !isPrimitiveOrWrapper(value.getClass()) && !(value instanceof String)) {
                // Convert POJO to Map and mask recursively
                try {
                    Map<String, Object> pojoMap = mapper.convertValue(value, Map.class);
                    entry.setValue(maskMapWithPojos(pojoMap));
                } catch (Exception e) {
                    // If conversion fails, try JSON masking
                    try {
                        String json = mapper.writeValueAsString(value);
                        entry.setValue(staticDataMasker.maskJson(json));
                    } catch (Exception e2) {
                        // Keep original value if all masking fails
                        log.debug("Could not mask nested object of type {}: {}", value.getClass().getName(), e.getMessage());
                    }
                }
            }
        }
        
        return masked;
    }
    
    /**
     * Checks if a field name is sensitive (case-insensitive)
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null || staticConfig == null) {
            return false;
        }
        return staticConfig.getSensitiveFields().stream()
                .anyMatch(field -> field.equalsIgnoreCase(fieldName));
    }
    
    /**
     * Checks if a class is a primitive or wrapper type
     */
    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive() ||
               clazz == String.class ||
               clazz == Boolean.class ||
               clazz == Byte.class ||
               clazz == Character.class ||
               clazz == Short.class ||
               clazz == Integer.class ||
               clazz == Long.class ||
               clazz == Float.class ||
               clazz == Double.class ||
               java.time.temporal.Temporal.class.isAssignableFrom(clazz);
    }
    
    @Override
    public void close() {
        // No cleanup needed
    }
}





