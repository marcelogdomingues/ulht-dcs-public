package com.example.dcs.credential.domain.authentication;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request for credential service.
 * Independent model for the credential service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Registration request for credential service")
public class RegisterRequest {
    
    @Schema(description = "User email address", example = "user@email.com")
    @NotBlank(message = "Email is required")
    @Email(message = "Email format is invalid")
    private String email;
    
    @Schema(description = "User password")
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
    
    @Schema(description = "User first name")
    private String firstName;
    
    @Schema(description = "User last name")
    private String lastName;
    
    @Schema(description = "Additional user metadata")
    private String metadata;
    
    @Schema(description = "User type", example = "student")
    @NotBlank(message = "Type is required")
    private String type;
    
    @Schema(description = "User name", example = "John Doe")
    @NotBlank(message = "Name is required")
    private String name;
}
