package pt.ulusofona.ulht.credential.domain.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;

/**
 * Registration response from credential service.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registration response from credential service")
public class RegisterResponse {
    
    @Schema(description = "User ID")
    private String userId;
    
    @Schema(description = "Registration status")
    private String status;
    
    @Schema(description = "Registration timestamp")
    private Instant registrationTime;
    
    @Schema(description = "Session cookie for immediate authentication")
    private String sessionCookie;
    
    @Schema(description = "Additional metadata")
    private String metadata;
}
