package pt.ulusofona.digital.wallet.domain.authentication;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import pt.ulusofona.digital.wallet.dto.common.BaseResponse;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Login response containing user authentication token and session information")
public class LoginResponse extends BaseResponse {
    
    @Schema(description = "User unique identifier", example = "539f8209-80ad-4f6c-bc8f-b5a6c670633f")
    @JsonProperty("id")
    private String id;
    
    @Schema(description = "User email/username", example = "user@email.com")
    @JsonProperty("username")
    private String username;
    
    @Schema(description = "JWT authentication token")
    @JsonProperty("token")
    private String token;
    
    @Schema(description = "Session cookie for wallet operations")
    @JsonProperty("sessionCookie")
    private String sessionCookie;
    
    @Schema(description = "Correlation ID for tracking async workflow", example = "550e8400-e29b-41d4-a716-446655440000")
    @JsonProperty("correlationId")
    private String correlationId;
    
    @Schema(description = "Token expiration timestamp")
    @JsonProperty("expiresAt")
    private LocalDateTime expiresAt;
    
    @Schema(description = "Token type", example = "Bearer")
    @JsonProperty("tokenType")
    private String tokenType;
    
    @Schema(description = "User roles and permissions")
    @JsonProperty("roles")
    private List<String> roles;
    
    @Schema(description = "Session timeout in minutes", example = "30")
    @JsonProperty("sessionTimeout")
    private Integer sessionTimeout;
    
    @Schema(description = "Refresh token for token renewal")
    @JsonProperty("refreshToken")
    private String refreshToken;
    
    @Schema(description = "User profile information")
    @JsonProperty("userProfile")
    private UserProfile userProfile;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "User profile information")
    public static class UserProfile {
        
        @Schema(description = "User full name", example = "João Silva")
        @JsonProperty("fullName")
        private String fullName;
        
        @Schema(description = "User email", example = "joao.silva@ulusofona.pt")
        @JsonProperty("email")
        private String email;
        
        @Schema(description = "Student code", example = "a12345678")
        @JsonProperty("studentCode")
        private String studentCode;
        
        @Schema(description = "Course name", example = "Computer Science")
        @JsonProperty("courseName")
        private String courseName;
        
        @Schema(description = "Institution name", example = "ULHT")
        @JsonProperty("institutionName")
        private String institutionName;
    }
} 