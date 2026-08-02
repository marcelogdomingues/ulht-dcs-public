package com.example.dcs.sis.domain.student;

import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.example.dcs.sis.dto.common.BaseEntity;
import com.example.dcs.sis.validation.ValidStudentCode;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper=false)
@Schema(description = "Student login information and session details")
public class StudentLogin extends BaseEntity {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Student email address", example = "joao.silva@usis.pt")
    private String email;
    
    @ValidStudentCode
    @Schema(description = "Student code", example = "a12345678")
    private String studentCode;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Schema(description = "Student display name", example = "João Silva")
    private String name;
    
    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "Student full name", example = "João Silva Santos")
    private String fullName;
    
    @NotBlank(message = "Course name is required")
    @Size(max = 100, message = "Course name cannot exceed 100 characters")
    @Schema(description = "Course name", example = "Computer Science")
    private String courseName;
    
    @NotNull(message = "Course code is required")
    @Positive(message = "Course code must be positive")
    @Schema(description = "Course code", example = "101")
    private Integer courseCode;
    
    @NotNull(message = "Institution code is required")
    @Positive(message = "Institution code must be positive")
    @Schema(description = "Institution code", example = "001")
    private Integer institutionCode;
    
    @NotBlank(message = "Institution name is required")
    @Size(max = 100, message = "Institution name cannot exceed 100 characters")
    @Schema(description = "Institution name", example = "Example University")
    private String institutionName;
    
    @Builder.Default
    @Schema(description = "Welcome kit availability", example = "true")
    private Boolean welcomeKit = false;
    
    @Size(max = 100, message = "Degree name cannot exceed 100 characters")
    @Schema(description = "Degree name", example = "Bachelor of Science")
    private String degreeName;
    
    @Positive(message = "Degree code must be positive")
    @Schema(description = "Degree code", example = "201")
    private Integer degreeCode;
    
    @Builder.Default
    @Schema(description = "Digital card access permission", example = "true")
    private Boolean hasAccessDigitalCard = false;
    
    @Schema(description = "Privacy agreement status", example = "accepted")
    private String privacyAgreement;
    
    @Schema(description = "Notification agreement status", example = "accepted")
    private String notificationAgree;
    
    @Schema(description = "Server timezone", example = "Europe/Lisbon")
    private String serverTimezone;
    
    @Schema(description = "Error code if login failed", example = "LOGIN_001")
    private String errorCode;
    
    @Schema(description = "Login session token")
    private String sessionToken;
    
    @Schema(description = "Last login timestamp")
    private LocalDateTime lastLoginAt;
    
    @Schema(description = "Login status")
    private LoginStatus loginStatus;
    
    @Schema(description = "Client device information")
    private String deviceInfo;
    
    @Schema(description = "IP address of the login request")
    private String ipAddress;
    
    @Schema(description = "User agent string")
    private String userAgent;
    
    /**
     * Login status enumeration
     */
    public enum LoginStatus {
        SUCCESS("Login successful"),
        FAILED("Login failed"),
        PENDING("Login pending"),
        EXPIRED("Session expired"),
        BLOCKED("Account blocked");
        
        private final String description;
        
        LoginStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}