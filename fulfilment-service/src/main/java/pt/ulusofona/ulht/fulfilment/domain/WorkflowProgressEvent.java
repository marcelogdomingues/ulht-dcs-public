package pt.ulusofona.ulht.fulfilment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event sent for workflow progress updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workflow progress update event")
public class WorkflowProgressEvent {
    
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
    
    @Schema(description = "Event timestamp")
    @JsonProperty("timestamp")
    private Long timestamp;
}
