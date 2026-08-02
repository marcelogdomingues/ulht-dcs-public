package com.example.dcs.sis.dto.common;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all request DTOs providing common fields and validation.
 * This ensures consistency across all API requests and provides built-in
 * correlation and tracing capabilities.
 */
@Data
@EqualsAndHashCode
@ToString
@Schema(description = "Base request structure with common fields")
public abstract class BaseRequest {
    
    /**
     * Unique identifier for this request instance.
     * Used for tracing and logging purposes.
     */
    @NotNull(message = "Request ID is required")
    @Schema(
        description = "Unique request identifier for tracing",
        example = "req_12345678-1234-1234-1234-123456789abc"
    )
    private String requestId;
    
    /**
     * Timestamp when the request was created on the client side.
     * Used for latency measurement and debugging.
     */
    @NotNull(message = "Request timestamp is required")
    @Schema(
        description = "Request creation timestamp",
        example = "2025-01-01T12:00:00"
    )
    private LocalDateTime requestTimestamp;
    
    /**
     * Correlation ID for tracing requests across service boundaries.
     * Should be propagated through all service calls.
     */
    @Schema(
        description = "Correlation ID for distributed tracing",
        example = "corr_87654321-4321-4321-4321-cba987654321"
    )
    private String correlationId;
    
    /**
     * Client application version making the request.
     * Used for API versioning and compatibility checks.
     */
    @Schema(
        description = "Client application version",
        example = "1.2.3"
    )
    private String clientVersion;
    
    /**
     * Default constructor that initializes common fields.
     */
    public BaseRequest() {
        this.requestId = generateRequestId();
        this.requestTimestamp = LocalDateTime.now();
        this.correlationId = UUID.randomUUID().toString();
    }
    
    /**
     * Constructor with explicit correlation ID for request tracing.
     * 
     * @param correlationId The correlation ID to use for this request
     */
    public BaseRequest(String correlationId) {
        this.requestId = generateRequestId();
        this.requestTimestamp = LocalDateTime.now();
        this.correlationId = correlationId;
    }
    
    /**
     * Generates a unique request ID with timestamp prefix.
     * 
     * @return A unique request identifier
     */
    private String generateRequestId() {
        return "req_" + System.currentTimeMillis() + "_" + 
               UUID.randomUUID().toString().substring(0, 8);
    }
}

