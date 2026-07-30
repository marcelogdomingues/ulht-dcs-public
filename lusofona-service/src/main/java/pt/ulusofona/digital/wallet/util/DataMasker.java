package pt.ulusofona.digital.wallet.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import pt.ulusofona.digital.wallet.config.DataMaskerConfig;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for masking sensitive data in logs and messages.
 * Masks fields defined in application.yml with the configured mask string.
 */
@Slf4j
public class DataMasker {
    
    private final DataMaskerConfig config;
    private final ObjectMapper objectMapper;
    private final Set<String> sensitiveFieldsLower;
    private final Set<String> sensitiveHeadersLower;
    
    public DataMasker(DataMaskerConfig config, ObjectMapper objectMapper) {
        this.config = config;
        this.objectMapper = objectMapper;
        // Pre-compute lowercase sets for case-insensitive matching
        this.sensitiveFieldsLower = config.getSensitiveFields().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        this.sensitiveHeadersLower = config.getSensitiveHeaders().stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
    
    /**
     * Masks sensitive data in a JSON string
     */
    public String maskJson(String json) {
        if (!config.isEnabled() || json == null || json.isEmpty()) {
            return json;
        }
        
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode masked = maskJsonNode(root);
            return objectMapper.writeValueAsString(masked);
        } catch (Exception e) {
            log.debug("Failed to parse JSON for masking, returning original: {}", e.getMessage());
            // If JSON parsing fails, try to mask as plain text
            return maskPlainText(json);
        }
    }
    
    /**
     * Masks sensitive data in a Map (e.g., from Kafka messages)
     */
    public Map<String, Object> maskMap(Map<String, Object> map) {
        if (!config.isEnabled() || map == null || map.isEmpty()) {
            return map;
        }
        
        Map<String, Object> masked = new java.util.HashMap<>(map);
        maskMapRecursive(masked);
        return masked;
    }
    
    /**
     * Masks sensitive data in a plain text string
     */
    public String maskPlainText(String text) {
        if (!config.isEnabled() || text == null || text.isEmpty()) {
            return text;
        }
        
        // Simple pattern matching for key=value pairs
        String result = text;
        for (String field : config.getSensitiveFields()) {
            // Match patterns like: "field": "value" or field=value or field: value
            String pattern = "(?i)(\"?" + field + "\"?\\s*[:=]\\s*\"?)([^,\"}\\s]+)(\"?)";
            result = result.replaceAll(pattern, "$1" + config.getMaskString() + "$3");
        }
        return result;
    }
    
    /**
     * Masks a header value if the header name is sensitive
     */
    public String maskHeader(String headerName, String headerValue) {
        if (!config.isEnabled() || !config.isMaskHeaders() || headerValue == null) {
            return headerValue;
        }
        
        if (headerName != null && sensitiveHeadersLower.contains(headerName.toLowerCase())) {
            return config.getMaskString();
        }
        
        return headerValue;
    }
    
    /**
     * Recursively masks sensitive fields in a JSON node
     */
    private JsonNode maskJsonNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return node;
        }
        
        if (node.isObject()) {
            ObjectNode masked = objectMapper.createObjectNode();
            node.fieldNames().forEachRemaining(fieldName -> {
                JsonNode value = node.get(fieldName);
                
                if (isSensitiveField(fieldName)) {
                    masked.put(fieldName, config.getMaskString());
                } else if (value != null && (value.isObject() || value.isArray())) {
                    masked.set(fieldName, maskJsonNode(value));
                } else if (value != null) {
                    masked.set(fieldName, value);
                }
            });
            return masked;
        } else if (node.isArray()) {
            ArrayNode masked = objectMapper.createArrayNode();
            node.forEach(item -> masked.add(maskJsonNode(item)));
            return masked;
        }
        
        return node;
    }
    
    /**
     * Recursively masks sensitive fields in a Map
     */
    @SuppressWarnings("unchecked")
    private void maskMapRecursive(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (isSensitiveField(key)) {
                entry.setValue(config.getMaskString());
            } else if (value instanceof Map) {
                maskMapRecursive((Map<String, Object>) value);
            } else if (value instanceof List) {
                maskListRecursive((List<Object>) value);
            }
        }
    }
    
    /**
     * Recursively masks sensitive fields in a List
     */
    @SuppressWarnings("unchecked")
    private void maskListRecursive(List<Object> list) {
        for (int i = 0; i < list.size(); i++) {
            Object item = list.get(i);
            if (item instanceof Map) {
                Map<String, Object> maskedItem = new java.util.HashMap<>((Map<String, Object>) item);
                maskMapRecursive(maskedItem);
                list.set(i, maskedItem);
            } else if (item instanceof List) {
                maskListRecursive((List<Object>) item);
            }
        }
    }
    
    /**
     * Checks if a field name is sensitive (case-insensitive)
     */
    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        return sensitiveFieldsLower.contains(fieldName.toLowerCase());
    }
}





