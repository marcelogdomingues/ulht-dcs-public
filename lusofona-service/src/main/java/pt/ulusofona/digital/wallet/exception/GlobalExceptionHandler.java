package pt.ulusofona.digital.wallet.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import pt.ulusofona.digital.wallet.generated.model.ErrorResponse;
import pt.ulusofona.digital.wallet.generated.model.ValidationError;
import pt.ulusofona.digital.wallet.exception.http.*;
import pt.ulusofona.digital.wallet.exception.lusofona.LusofonaApiException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private boolean isActuatorEndpoint(WebRequest request) {
        String path = request.getDescription(false);
        return path != null && (path.contains("/actuator") || path.contains("/api/v1/actuator"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed for request: {}", request.getDescription(false));
        
        List<ValidationError> validationErrors = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new ValidationError(error.getField(), error.getDefaultMessage(), (String) error.getRejectedValue()))
            .collect(Collectors.toList());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage("Validation failed");
        errorResponse.setErrorCode("VALIDATION_ERROR");
        errorResponse.setStatusCode(400);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        errorResponse.setValidationErrors(validationErrors);
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex, WebRequest request) {
        log.warn("Validation exception: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("VALIDATION_ERROR");
        errorResponse.setStatusCode(400);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex, WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("NOT_FOUND");
        errorResponse.setStatusCode(404);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("UNAUTHORIZED");
        errorResponse.setStatusCode(401);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequestException(BadRequestException ex, WebRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("BAD_REQUEST");
        errorResponse.setStatusCode(400);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(ConflictException ex, WebRequest request) {
        log.warn("Conflict: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("CONFLICT");
        errorResponse.setStatusCode(409);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        log.warn("Forbidden: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("FORBIDDEN");
        errorResponse.setStatusCode(403);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

    @ExceptionHandler(TimeoutException.class)
    public ResponseEntity<ErrorResponse> handleTimeoutException(TimeoutException ex, WebRequest request) {
        log.warn("Timeout: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getMessage());
        errorResponse.setErrorCode("TIMEOUT");
        errorResponse.setStatusCode(408);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body(errorResponse);
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ErrorResponse> handleExternalServiceException(ExternalServiceException ex, WebRequest request) {
        log.error("External service error: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage("External service is currently unavailable");
        errorResponse.setErrorCode("EXTERNAL_SERVICE_ERROR");
        errorResponse.setStatusCode(500);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(LusofonaApiException.class)
    public ResponseEntity<ErrorResponse> handleLusofonaApiException(LusofonaApiException ex, WebRequest request) {
        log.error("Lusofona API error: {} - {} (Context: {})", 
                ex.getErrorCode().getCode(), 
                ex.getErrorCode().getName(), 
                ex.getOperationContext());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage(ex.getErrorMessage());
        errorResponse.setErrorCode("LUSOFONA_API_ERROR_" + ex.getErrorCode().getCode());
        
        // Map Lusofona error codes to appropriate HTTP status codes
        HttpStatus httpStatus = switch (ex.getErrorCode()) {
            case AUTHENTICATION_FAILURE, INVALID_INSTALLATION_KEY -> {
                errorResponse.setStatusCode(401);
                yield HttpStatus.UNAUTHORIZED;
            }
            case OPERATION_NOT_AVAILABLE, APPLICATION_NOT_REGISTERED -> {
                errorResponse.setStatusCode(403);
                yield HttpStatus.FORBIDDEN;
            }
            case DATA_NOT_FOUND -> {
                errorResponse.setStatusCode(404);
                yield HttpStatus.NOT_FOUND;
            }
            case INCORRECT_FORMAT, INVALID_REQUEST -> {
                errorResponse.setStatusCode(400);
                yield HttpStatus.BAD_REQUEST;
            }
            case BUSINESS_RULE_VALIDATION -> {
                errorResponse.setStatusCode(422);
                yield HttpStatus.UNPROCESSABLE_ENTITY;
            }
            default -> {
                errorResponse.setStatusCode(500);
                yield HttpStatus.INTERNAL_SERVER_ERROR;
            }
        };
        
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(httpStatus).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        // Skip actuator endpoints to avoid interfering with Spring Boot Actuator
        if (isActuatorEndpoint(request)) {
            log.debug("Skipping exception handling for actuator endpoint: {}", request.getDescription(false));
            throw new RuntimeException(ex);
        }
        
        log.error("Unexpected error occurred", ex);
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage("An unexpected error occurred");
        errorResponse.setErrorCode("INTERNAL_ERROR");
        errorResponse.setStatusCode(500);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        log.warn("Unsupported media type: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage("Content-Type is not supported");
        errorResponse.setErrorCode("UNSUPPORTED_MEDIA_TYPE");
        errorResponse.setStatusCode(400);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("Unsupported HTTP method: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage("Request method '" + ex.getMethod() + "' is not supported");
        errorResponse.setErrorCode("METHOD_NOT_ALLOWED");
        errorResponse.setStatusCode(405);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Message not readable: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus("error");
        errorResponse.setMessage("Required request body is missing or invalid");
        errorResponse.setErrorCode("BAD_REQUEST");
        errorResponse.setStatusCode(400);
        errorResponse.setTimestamp(OffsetDateTime.now());
        errorResponse.setPath(request.getDescription(false));
        
        return ResponseEntity.badRequest().body(errorResponse);
    }
}