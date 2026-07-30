package pt.ulusofona.ulht.credential.domain.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request for credential service authentication.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login request for credential service authentication")
public class LoginRequest {
    
    @Schema(description = "Authentication type", example = "email")
    @NotBlank(message = "Authentication type is required")
    private String type;
    
    @Schema(description = "User email address", example = "user@email.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    
    @Schema(description = "User password")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
