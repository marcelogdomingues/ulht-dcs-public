package pt.ulusofona.ulht.fulfilment.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event sent when a workflow is completed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workflow completion event")
public class WorkflowCompletionEvent {
    
    @Schema(description = "Unique correlation ID for the workflow", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Schema(description = "Final workflow status", example = "COMPLETED")
    @JsonProperty("status")
    private String status;
    
    @Schema(description = "Final progress percentage", example = "100")
    @JsonProperty("progress")
    private Integer progress;
    
    @Schema(description = "Completion message", example = "Workflow completed successfully")
    @JsonProperty("message")
    private String message;
    
    @Schema(description = "Workflow result")
    @JsonProperty("result")
    private Object result;
    
    @Schema(description = "Event timestamp")
    @JsonProperty("timestamp")
    private Long timestamp;
}
