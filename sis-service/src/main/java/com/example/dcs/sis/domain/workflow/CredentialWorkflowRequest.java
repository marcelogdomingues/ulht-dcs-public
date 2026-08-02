package com.example.dcs.sis.domain.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Request object for starting credential workflows.
 * Sent from SIS Service to Credential Service via Kafka.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to start a credential workflow")
public class CredentialWorkflowRequest {
    
    @Schema(description = "Unique correlation ID for tracking the workflow", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Schema(description = "User ID requesting the credential", example = "a12345678")
    @JsonProperty("userId")
    private String userId;
    
    @Schema(description = "Student data from external DCS services")
    @JsonProperty("studentData")
    private Object studentData;
    
    @Schema(description = "Timestamp when the request was created", example = "2024-01-15T10:30:00Z")
    @JsonProperty("timestamp")
    private OffsetDateTime timestamp;
    
    @Schema(description = "Source service that initiated the request", example = "dcs-proxy")
    @JsonProperty("source")
    private String source;
    
    @Schema(description = "Type of credential workflow", example = "STUDENT_CREDENTIAL_ISSUANCE")
    @JsonProperty("workflowType")
    private String workflowType;
    
    @Schema(description = "Priority of the workflow", example = "NORMAL")
    @JsonProperty("priority")
    private String priority;
}
