package pt.ulusofona.ulht.credential.domain.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * Login response from credential service.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login response from credential service")
public class LoginResponse {
    
    @Schema(description = "Session cookie for authentication")
    private String sessionCookie;
    
    @Schema(description = "User ID")
    private String userId;
    
    @Schema(description = "Login timestamp")
    private Instant loginTime;
    
    @Schema(description = "Session expiration time")
    private Instant expiresAt;
    
    @Schema(description = "Login status")
    private String status;
    
    @Schema(description = "Additional metadata")
    private String metadata;
}
