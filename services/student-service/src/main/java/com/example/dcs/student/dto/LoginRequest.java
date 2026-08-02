package com.example.dcs.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Minimal student login request
 * Only what's needed to identify the student!
 */
@Schema(description = "Student login request - minimal data required")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Student username", example = "a12345678", required = true)
    public String userName;

    @NotBlank(message = "Install key is required")
    @Schema(description = "Mobile app install key", example = "00000_0000000000000", required = true)
    public String installKey;

    @Schema(description = "Platform", example = "ios", defaultValue = "ios")
    public String platform = "ios";

    @Schema(description = "Language", example = "PT", defaultValue = "PT")
    public String language = "PT";

    @Schema(description = "Application identifier", example = "com.example.dcs.mobile")
    public String application = "com.example.dcs.mobile";

    @Schema(description = "Version code", example = "1601206")
    public String versionCode = "1601206";
    
    public LoginRequest() {}
    
    public LoginRequest(String userName, String installKey, String platform, String language, String application, String versionCode) {
        this.userName = userName;
        this.installKey = installKey;
        this.platform = platform;
        this.language = language;
        this.application = application;
        this.versionCode = versionCode;
    }
}

