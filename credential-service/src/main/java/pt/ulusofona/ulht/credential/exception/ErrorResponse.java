package pt.ulusofona.ulht.credential.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.Map;

/**
 * Error response model for the credential service.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Error response")
public class ErrorResponse {
    
    @Schema(description = "Error code")
    private String errorCode;
    
    @Schema(description = "Error message")
    private String message;
    
    @Schema(description = "Service name")
    private String service;
    
    @Schema(description = "Timestamp")
    private Instant timestamp;
    
    @Schema(description = "Request path")
    private String path;
    
    @Schema(description = "Additional error details")
    private Map<String, Object> details;
}
