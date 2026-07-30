package pt.ulusofona.student.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for Student Service
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StudentServiceException.class)
    public ResponseEntity<Map<String, Object>> handleStudentServiceException(
            StudentServiceException ex, WebRequest request) {
        
        log.error("StudentServiceException: {} - {}", ex.getErrorCode().getCode(), ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("errorCode", ex.getErrorCode().getCode());
        errorResponse.put("errorName", ex.getErrorCode().getFunName());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", OffsetDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        
        HttpStatus status = determineHttpStatus(ex.getErrorCode());
        errorResponse.put("statusCode", status.value());
        
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<Map<String, Object>> handleExternalServiceException(
            ExternalServiceException ex, WebRequest request) {
        
        log.error("ExternalServiceException: {} - {}", ex.getErrorCode().getCode(), ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("errorCode", ex.getErrorCode().getCode());
        errorResponse.put("errorName", ex.getErrorCode().getFunName());
        errorResponse.put("message", ex.getMessage());
        errorResponse.put("timestamp", OffsetDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        errorResponse.put("statusCode", HttpStatus.SERVICE_UNAVAILABLE.value());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(feign.FeignException.NotFound.class)
    public ResponseEntity<Map<String, Object>> handleFeignNotFoundException(
            feign.FeignException.NotFound ex, WebRequest request) {
        
        // Extract correlationId from request path
        String path = request.getDescription(false);
        String correlationId = extractCorrelationIdFromPath(path);
        
        log.warn("Workflow not found or not yet started: correlationId={}", correlationId);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("errorCode", ErrorCodes.WORKFLOW_NOT_FOUND.getCode());
        errorResponse.put("errorName", ErrorCodes.WORKFLOW_NOT_FOUND.getFunName());
        errorResponse.put("message", String.format(
            "Workflow not found or not yet started. " +
            "The workflow may still be processing. " +
            "Please check status at: /student/status/%s", 
            correlationId != null ? correlationId : "{correlationId}"));
        errorResponse.put("timestamp", OffsetDateTime.now());
        errorResponse.put("path", path);
        errorResponse.put("statusCode", HttpStatus.NOT_FOUND.value());
        errorResponse.put("suggestion", "Wait 10-15 seconds after issuing credentials, then try again. Or check /student/status/{correlationId} first.");
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(feign.FeignException.class)
    public ResponseEntity<Map<String, Object>> handleFeignException(
            feign.FeignException ex, WebRequest request) {
        
        log.error("Feign client exception: status={}, message={}", ex.status(), ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("errorCode", ErrorCodes.FULFILMENT_SERVICE_ERROR.getCode());
        errorResponse.put("errorName", ErrorCodes.FULFILMENT_SERVICE_ERROR.getFunName());
        errorResponse.put("message", "Error communicating with internal service: " + ex.getMessage());
        errorResponse.put("timestamp", OffsetDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        errorResponse.put("statusCode", ex.status() > 0 ? ex.status() : HttpStatus.SERVICE_UNAVAILABLE.value());
        
        HttpStatus status = ex.status() > 0 && ex.status() < 600 
            ? HttpStatus.valueOf(ex.status()) 
            : HttpStatus.SERVICE_UNAVAILABLE;
        
        return new ResponseEntity<>(errorResponse, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("errorCode", ErrorCodes.INTERNAL_SERVER_ERROR.getCode());
        errorResponse.put("errorName", ErrorCodes.INTERNAL_SERVER_ERROR.getFunName());
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("timestamp", OffsetDateTime.now());
        errorResponse.put("path", request.getDescription(false));
        errorResponse.put("statusCode", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Extract correlationId from request path
     */
    private String extractCorrelationIdFromPath(String path) {
        if (path == null) return null;
        
        // Extract from paths like: uri=/api/v1/student/credentials/{correlationId}
        String[] parts = path.split("/");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            // Check if it looks like a UUID
            if (lastPart.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
                return lastPart;
            }
        }
        return null;
    }

    private HttpStatus determineHttpStatus(ErrorCodes errorCode) {
        if (errorCode.isValidationError()) {
            return HttpStatus.BAD_REQUEST;
        } else if (errorCode.isKafkaError()) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else if (errorCode.isExternalServiceError()) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        } else if (errorCode == ErrorCodes.WORKFLOW_NOT_FOUND || 
                   errorCode == ErrorCodes.CREDENTIALS_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        } else if (errorCode == ErrorCodes.CREDENTIALS_NOT_READY) {
            return HttpStatus.ACCEPTED;
        } else {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }
}

