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
@Schema(description = "Student registration information with academic and personal details")
public class StudentRegistration extends BaseEntity {
    
    @Schema(description = "Web service profile ID", example = "12345")
    private Integer wsIdProfile;
    
    @Schema(description = "Account activation timestamp")
    private LocalDateTime activationTime;
    
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Schema(description = "Student display name", example = "João Silva")
    private String name;
    
    @Min(value = 0, message = "Agree plus must be non-negative")
    @Max(value = 1, message = "Agree plus must be 0 or 1")
    @Schema(description = "Agreement plus flag", example = "1")
    private Integer agreePlus;
    
    @NotBlank(message = "Install key is required")
    @Schema(description = "Device installation key", example = "00000_0000000000000")
    private String installKey;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Schema(description = "Student email address", example = "joao.silva@usis.pt")
    private String email;
    
    @ValidStudentCode
    @Schema(description = "Student code", example = "a12345678")
    private String studentCode;
    
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
    @Schema(description = "Institution name", example = "Example University de Humanidades e Tecnologias")
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
    
    @Schema(description = "Error code if registration failed", example = "REG_001")
    private String errorCode;
    
    @Schema(description = "Registration status")
    private RegistrationStatus registrationStatus;
    
    @Schema(description = "Additional metadata")
    private String metadata;
    
    /**
     * Registration status enumeration
     */
    public enum RegistrationStatus {
        PENDING("Registration pending approval"),
        APPROVED("Registration approved"),
        REJECTED("Registration rejected"),
        COMPLETED("Registration completed"),
        EXPIRED("Registration expired");
        
        private final String description;
        
        RegistrationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}