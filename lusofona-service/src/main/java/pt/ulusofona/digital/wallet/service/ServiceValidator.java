package pt.ulusofona.digital.wallet.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import feign.FeignException;
import pt.ulusofona.digital.wallet.exception.ErrorCodes;
import pt.ulusofona.digital.wallet.exception.ErrorUtils;
import pt.ulusofona.digital.wallet.exception.ValidationException;
import pt.ulusofona.digital.wallet.exception.http.*;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ServiceValidator {

    private static final long TIMEOUT = 10000L; // Timeout duration in milliseconds (10 seconds)
    // Reserved for future logging needs
    @SuppressWarnings("unused")
    private final Logger log = LoggerFactory.getLogger(ServiceValidator.class);

    protected <T> T executeAsync(Supplier<T> supplier, String errorMessage) throws ExternalServiceException, TimeoutException, ConflictException, BadRequestException, ValidationException, UnauthorizedException, ForbiddenException, NotFoundException {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(supplier)
                .orTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        ErrorUtils.logError(ErrorCodes.TIMEOUT, "Request timed out: " + errorMessage);
                        throw new CompletionException(new TimeoutException("Operation timed out after " + TIMEOUT + "ms"));
                    } else {
                        ErrorUtils.logError(ErrorCodes.EXTERNAL_SERVICE_ERROR, "Service error: " + errorMessage);
                        // Preserve the original exception type instead of wrapping everything
                        if (ex instanceof CompletionException) {
                            throw (CompletionException) ex;
                        } else {
                            throw new CompletionException(ex);
                        }
                    }
                });

        try {
            T result = future.join();
            if (Objects.isNull(result)) {
                ErrorUtils.logError(ErrorCodes.EXTERNAL_SERVICE_ERROR, "Service returned null response: " + errorMessage);
                throw new ExternalServiceException("Service returned null response - " + errorMessage);
            }
            return result;
        } catch (CompletionException ex) {
            Throwable cause = ex.getCause();

            // Map FeignException$Conflict to ConflictException
            if (cause instanceof FeignException.Conflict) {
                throw new ConflictException("Resource conflict: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.BadRequest) {
                throw new BadRequestException("Bad request: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.Unauthorized) {
                throw new UnauthorizedException("Unauthorized: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.Forbidden) {
                throw new ForbiddenException("Forbidden: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.NotFound) {
                throw new NotFoundException("Not found: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.TooManyRequests) {
                throw new TimeoutException("Too many requests: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.UnprocessableEntity) {
                throw new ValidationException("Unprocessable entity: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException.ServiceUnavailable) {
                throw new ExternalServiceException("Service unavailable: " + cause.getMessage(), cause);
            } else if (cause instanceof FeignException) {
                // Generic FeignException
                throw new ExternalServiceException("External service error: " + cause.getMessage(), cause);
            } else if (cause instanceof TimeoutException) {
                throw (TimeoutException) cause;
            } else if (cause instanceof ConflictException) {
                throw (ConflictException) cause;
            } else if (cause instanceof BadRequestException) {
                throw (BadRequestException) cause;
            } else if (cause instanceof ValidationException) {
                throw (ValidationException) cause;
            } else if (cause instanceof UnauthorizedException) {
                throw (UnauthorizedException) cause;
            } else if (cause instanceof ForbiddenException) {
                throw (ForbiddenException) cause;
            } else if (cause instanceof NotFoundException) {
                throw (NotFoundException) cause;
            } else if (cause instanceof ExternalServiceException) {
                throw (ExternalServiceException) cause;
            } else {
                ErrorUtils.logError(ErrorCodes.EXTERNAL_SERVICE_ERROR, "Unexpected error during async operation: " + errorMessage);
                throw new ExternalServiceException("Unexpected error during async operation: " + errorMessage, ex);
            }
        }
    }

    protected void validateRequest(Map<String, ?> requestBody) throws ValidationException {
        if (requestBody == null || requestBody.isEmpty()) {
            ErrorUtils.logError(ErrorCodes.VALIDATION_ERROR, "Request validation failed: Request body cannot be null or empty");
            throw new ValidationException("Request body cannot be null or empty");
        }
    }
}
