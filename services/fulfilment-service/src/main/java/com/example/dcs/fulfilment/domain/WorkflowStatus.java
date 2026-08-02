package com.example.dcs.fulfilment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the current status of a workflow
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Current status of a workflow")
public class WorkflowStatus {
    
    @Schema(description = "Unique correlation ID for the workflow", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Schema(description = "Current workflow status", example = "PROCESSING")
    @JsonProperty("status")
    private String status;
    
    @Schema(description = "Progress percentage (0-100)", example = "75")
    @JsonProperty("progress")
    private Integer progress;
    
    @Schema(description = "Status message", example = "Issuing credential...")
    @JsonProperty("message")
    private String message;
    
    @Schema(description = "Workflow result (when completed)")
    @JsonProperty("result")
    private Object result;
    
    @Schema(description = "Error code (when failed)", example = "SIS-004")
    @JsonProperty("errorCode")
    private String errorCode;
    
    @Schema(description = "Fun error name (when failed)", example = "Missing Membership")
    @JsonProperty("errorName")
    private String errorName;
    
    @Schema(description = "Error message (when failed)")
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @Schema(description = "Workflow creation timestamp")
    @JsonProperty("timestamp")
    private Long timestamp;
    
    @Schema(description = "Last update timestamp")
    @JsonProperty("lastUpdated")
    private Long lastUpdated;
}
