package com.example.dcs.sis.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Base class for all response DTOs providing common fields and status information.
 * This ensures consistency across all API responses and provides built-in
 * status tracking and metadata capabilities.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Schema(description = "Base response structure with common fields")
public abstract class BaseResponse extends BaseRequest {
    
    /**
     * HTTP status code of the response.
     * Provides quick access to the operation result.
     */
    @Schema(
        description = "HTTP status code",
        example = "200"
    )
    private Integer statusCode;
    
    /**
     * Human-readable message describing the operation result.
     * Provides context for success or failure scenarios.
     */
    @Schema(
        description = "Response message",
        example = "Operation completed successfully"
    )
    private String message;
    
    /**
     * Timestamp when the response was generated on the server side.
     * Used for latency measurement and debugging.
     */
    @Schema(
        description = "Response generation timestamp",
        example = "2025-01-01T12:00:01"
    )
    private LocalDateTime responseTimestamp;
    
    /**
     * Processing time in milliseconds for this request.
     * Useful for performance monitoring and optimization.
     */
    @Schema(
        description = "Request processing time in milliseconds",
        example = "150"
    )
    private Long processingTimeMs;
    
    /**
     * Default constructor that initializes response-specific fields.
     */
    public BaseResponse() {
        super();
        this.responseTimestamp = LocalDateTime.now();
        this.statusCode = 200;
        this.message = "Success";
        calculateProcessingTime();
    }
    
    /**
     * Constructor with explicit correlation ID for request tracing.
     * 
     * @param correlationId The correlation ID to use for this response
     */
    public BaseResponse(String correlationId) {
        super(correlationId);
        this.responseTimestamp = LocalDateTime.now();
        this.statusCode = 200;
        this.message = "Success";
        calculateProcessingTime();
    }
    
    /**
     * Constructor for error responses.
     * 
     * @param correlationId The correlation ID to use for this response
     * @param statusCode The HTTP status code
     * @param message The error message
     */
    public BaseResponse(String correlationId, Integer statusCode, String message) {
        super(correlationId);
        this.responseTimestamp = LocalDateTime.now();
        this.statusCode = statusCode;
        this.message = message;
        calculateProcessingTime();
    }
    
    /**
     * Calculates the processing time based on request and response timestamps.
     */
    private void calculateProcessingTime() {
        if (getRequestTimestamp() != null && responseTimestamp != null) {
            this.processingTimeMs = java.time.Duration.between(
                getRequestTimestamp(), 
                responseTimestamp
            ).toMillis();
        }
    }
    
    /**
     * Indicates whether the response represents a successful operation.
     * 
     * @return true if status code is in 200-299 range
     */
    public boolean isSuccess() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Indicates whether the response represents an error.
     * 
     * @return true if status code is >= 400
     */
    public boolean isError() {
        return statusCode != null && statusCode >= 400;
    }
}

