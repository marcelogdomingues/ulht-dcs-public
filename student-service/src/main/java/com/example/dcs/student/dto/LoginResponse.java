package com.example.dcs.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Simple login response with correlation ID for tracking
 */
@Schema(description = "Student login response with tracking information")
public class LoginResponse {

    @Schema(description = "Correlation ID for tracking the workflow", example = "550e8400-e29b-41d4-a716-446655440000")
    public String correlationId;

    @Schema(description = "Current status", example = "PROCESSING")
    public String status;

    @Schema(description = "Human-readable message", example = "Student login received, processing...")
    public String message;

    @Schema(description = "Endpoint to monitor progress", example = "/student/status/550e8400-e29b-41d4-a716-446655440000")
    public String monitorAt;

    @Schema(description = "Endpoint to get final credentials", example = "/student/credentials/550e8400-e29b-41d4-a716-446655440000")
    public String credentialsAt;
    
    public LoginResponse() {}

    public LoginResponse(String correlationId, String status, String message) {
        this.correlationId = correlationId;
        this.status = status;
        this.message = message;
        this.monitorAt = "/student/status/" + correlationId;
        this.credentialsAt = "/student/credentials/" + correlationId;
    }
    
    public LoginResponse(String correlationId, String status, String message, String monitorAt, String credentialsAt) {
        this.correlationId = correlationId;
        this.status = status;
        this.message = message;
        this.monitorAt = monitorAt;
        this.credentialsAt = credentialsAt;
    }
}

