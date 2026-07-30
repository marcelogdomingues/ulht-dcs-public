package pt.ulusofona.ulht.credential.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static pt.ulusofona.ulht.credential.exception.ErrorCodes.*;

/**
 * Global exception handler for the credential service.
 * Independent exception handling for the credential service.
 */
@Slf4j
@RestControllerAdvice(basePackages = "pt.ulusofona.ulht.credential.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        
        // Log the real (possibly sensitive) upstream detail server-side only.
        log.error("External service error [{}] from {}: {}",
                ex.getErrorCode(), ex.getServiceName(), ex.getMessage(), ex);

        // Return a generic, client-safe message. Do not leak raw upstream messages.
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ex.getErrorCode() != null ? ex.getErrorCode() : EXTERNAL_SERVICE_ERROR.getCode())
                .message(EXTERNAL_SERVICE_ERROR.getDescription())
                .service(ex.getServiceName())
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.warn("Validation error: {}", errors);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(VALIDATION_ERROR.getCode())
                .message(VALIDATION_ERROR.getDescription())
                .service("credential-service")
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(errors)
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        // Log the real detail server-side only; return a generic client-safe message.
        log.warn("Illegal argument: {}", ex.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(ILLEGAL_ARGUMENT.getCode())
                .message(ILLEGAL_ARGUMENT.getDescription())
                .service("credential-service")
                .timestamp(Instant.now())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, WebRequest request) {
        
        String path = request.getDescription(false).replace("uri=", "");
        
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(INTERNAL_SERVER_ERROR.getCode())
                .message(INTERNAL_SERVER_ERROR.getDescription())
                .service("credential-service")
                .timestamp(Instant.now())
                .path(path)
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
