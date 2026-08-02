package com.example.dcs.sis.exception;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Standard error response format for the DCS system
 * 
 * This class provides a consistent way to return error information across all APIs.
 * It includes the error code, error name (security identifier), error description, 
 * error message, and optional trace information.
 */
@Getter
@Setter
public class ErrorResponse {
    private String errorCode;
    private String errorName;
    private String errorDescription;
    private String errorMessage;
    private String trace;
    private String timestamp;

    public ErrorResponse(ErrorCodes errorCode, String errorMessage) {
        this.errorCode = errorCode.getCode();
        this.errorName = errorCode.getFunName();
        this.errorDescription = errorCode.getDescription();
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public ErrorResponse(ErrorCodes errorCode, String errorMessage, String trace) {
        this.errorCode = errorCode.getCode();
        this.errorName = errorCode.getFunName();
        this.errorDescription = errorCode.getDescription();
        this.errorMessage = errorMessage;
        this.trace = trace;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public ErrorResponse(String errorCode, String errorName, String errorDescription, String errorMessage) {
        this.errorCode = errorCode;
        this.errorName = errorName;
        this.errorDescription = errorDescription;
        this.errorMessage = errorMessage;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    public ErrorResponse(String errorCode, String errorName, String errorDescription, String errorMessage, String trace) {
        this.errorCode = errorCode;
        this.errorName = errorName;
        this.errorDescription = errorDescription;
        this.errorMessage = errorMessage;
        this.trace = trace;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}