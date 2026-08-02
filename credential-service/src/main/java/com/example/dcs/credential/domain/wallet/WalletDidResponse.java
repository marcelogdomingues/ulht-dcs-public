package com.example.dcs.credential.domain.wallet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Wallet DID response for credential service.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Wallet DID response")
public class WalletDidResponse {
    
    @JsonProperty("did")
    @Schema(description = "DID identifier")
    private String did;
    
    @JsonProperty("method")
    @Schema(description = "DID method")
    private String method;
    
    @JsonProperty("alias")
    @Schema(description = "DID alias")
    private String alias;
    
    @JsonProperty("document")
    @JsonDeserialize(using = DocumentDeserializer.class)
    @Schema(description = "DID document")
    private Map<String, Object> document;
    
    /**
     * Custom deserializer for document field.
     * Handles both JSON string and JSON object formats.
     */
    private static class DocumentDeserializer extends JsonDeserializer<Map<String, Object>> {
        private static final ObjectMapper objectMapper = new ObjectMapper();
        
        @Override
        public Map<String, Object> deserialize(JsonParser p, DeserializationContext ctxt) 
                throws IOException {
            // Check if the value is a string (JSON string) or an object
            JsonToken currentToken = p.getCurrentToken();
            if (currentToken == JsonToken.VALUE_STRING) {
                // Parse the JSON string
                String jsonString = p.getText();
                return objectMapper.readValue(jsonString, 
                    new TypeReference<Map<String, Object>>() {});
            } else {
                // Already an object, deserialize normally
                return objectMapper.readValue(p, 
                    new TypeReference<Map<String, Object>>() {});
            }
        }
    }
    
    @JsonProperty("verificationMethods")
    @Schema(description = "Verification methods")
    private List<VerificationMethod> verificationMethods;
    
    @JsonProperty("services")
    @Schema(description = "Services")
    private List<Service> services;
    
    @JsonProperty("createdAt")
    @Schema(description = "Creation timestamp")
    private String createdAt;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "Verification method")
    public static class VerificationMethod {
        
        @Schema(description = "Method ID")
        private String id;
        
        @Schema(description = "Method type")
        private String type;
        
        @Schema(description = "Controller")
        private String controller;
        
        @Schema(description = "Public key")
        private String publicKeyMultibase;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    @Schema(description = "DID service")
    public static class Service {
        
        @Schema(description = "Service ID")
        private String id;
        
        @Schema(description = "Service type")
        private String type;
        
        @Schema(description = "Service endpoint")
        private String serviceEndpoint;
    }
}
