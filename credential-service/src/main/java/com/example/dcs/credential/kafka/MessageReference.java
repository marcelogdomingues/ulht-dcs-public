package com.example.dcs.credential.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a reference to a large message stored externally.
 * This keeps Kafka messages small while allowing large data to be transmitted.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageReference {
    
    /**
     * Unique identifier for the referenced message
     */
    private String referenceId;
    
    /**
     * Type of the original message
     */
    private String messageType;
    
    /**
     * Size of the original message in bytes
     */
    private int messageSize;
    
    /**
     * Timestamp when the message was stored
     */
    private long storedAt;
    
    /**
     * Storage location (e.g., "redis", "s3", "database")
     */
    private String storageLocation;
    
    /**
     * Additional metadata about the stored message
     */
    private String metadata;
} 