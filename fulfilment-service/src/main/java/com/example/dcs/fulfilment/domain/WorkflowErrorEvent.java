package com.example.dcs.fulfilment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event sent when a workflow fails
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workflow error event")
public class WorkflowErrorEvent {
    
    @Schema(description = "Unique correlation ID for the workflow", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Schema(description = "Error status", example = "FAILED")
    @JsonProperty("status")
    private String status;
    
    @Schema(description = "Error code", example = "VALIDATION_ERROR")
    @JsonProperty("errorCode")
    private String errorCode;
    
    @Schema(description = "Error message")
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @Schema(description = "Event timestamp")
    @JsonProperty("timestamp")
    private Long timestamp;
}
