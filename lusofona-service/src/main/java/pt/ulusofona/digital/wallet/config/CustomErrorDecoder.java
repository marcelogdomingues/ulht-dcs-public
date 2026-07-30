package pt.ulusofona.digital.wallet.config;

import feign.Response;
import feign.codec.ErrorDecoder;
import pt.ulusofona.digital.wallet.exception.ValidationException;
import pt.ulusofona.digital.wallet.exception.http.*;

/**
 * Custom error decoder for Feign client calls
 * 
 * This class maps HTTP status codes from external service calls to our custom exceptions
 * with fun error names and descriptions. It handles all HTTP status codes comprehensively.
 */
public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        
        return switch (status) {
            // 4xx Client Errors
            case 400 -> new BadRequestException("Bad request from external service: " + methodKey);
            case 401 -> new UnauthorizedException("Unauthorized access to external service: " + methodKey);
            case 403 -> new ForbiddenException("Access forbidden to external service: " + methodKey);
            case 404 -> new NotFoundException("Resource not found in external service: " + methodKey);
            case 405 -> new BadRequestException("Method not allowed in external service: " + methodKey);
            case 406 -> new BadRequestException("Not acceptable response from external service: " + methodKey);
            case 407 -> new UnauthorizedException("Proxy authentication required: " + methodKey);
            case 408 -> new TimeoutException("Request timeout from external service: " + methodKey);
            case 409 -> new ConflictException("Resource conflict in external service: " + methodKey);
            case 410 -> new NotFoundException("Resource gone from external service: " + methodKey);
            case 411 -> new BadRequestException("Length required by external service: " + methodKey);
            case 412 -> new BadRequestException("Precondition failed in external service: " + methodKey);
            case 413 -> new BadRequestException("Payload too large for external service: " + methodKey);
            case 414 -> new BadRequestException("URI too long for external service: " + methodKey);
            case 415 -> new BadRequestException("Unsupported media type by external service: " + methodKey);
            case 416 -> new BadRequestException("Range not satisfiable by external service: " + methodKey);
            case 417 -> new BadRequestException("Expectation failed in external service: " + methodKey);
            case 418 -> new BadRequestException("I'm a teapot - external service joke: " + methodKey);
            case 421 -> new BadRequestException("Misdirected request to external service: " + methodKey);
            case 422 -> new ValidationException("Unprocessable entity from external service: " + methodKey);
            case 423 -> new ForbiddenException("Resource locked by external service: " + methodKey);
            case 424 -> new BadRequestException("Failed dependency in external service: " + methodKey);
            case 425 -> new BadRequestException("Too early request to external service: " + methodKey);
            case 426 -> new BadRequestException("Upgrade required by external service: " + methodKey);
            case 428 -> new BadRequestException("Precondition required by external service: " + methodKey);
            case 429 -> new TimeoutException("Too many requests to external service: " + methodKey);
            case 431 -> new BadRequestException("Request header fields too large for external service: " + methodKey);
            case 451 -> new ForbiddenException("Unavailable for legal reasons from external service: " + methodKey);
            
            // 5xx Server Errors
            case 500 -> new ExternalServiceException("Internal server error in external service: " + methodKey);
            case 501 -> new ExternalServiceException("Not implemented by external service: " + methodKey);
            case 502 -> new ExternalServiceException("Bad gateway from external service: " + methodKey);
            case 503 -> new ExternalServiceException("Service unavailable from external service: " + methodKey);
            case 504 -> new TimeoutException("Gateway timeout from external service: " + methodKey);
            case 505 -> new ExternalServiceException("HTTP version not supported by external service: " + methodKey);
            case 506 -> new ExternalServiceException("Variant also negotiates in external service: " + methodKey);
            case 507 -> new ExternalServiceException("Insufficient storage in external service: " + methodKey);
            case 508 -> new ExternalServiceException("Loop detected in external service: " + methodKey);
            case 510 -> new ExternalServiceException("Not extended by external service: " + methodKey);
            case 511 -> new ExternalServiceException("Network authentication required by external service: " + methodKey);
            
            // Unknown status codes
            default -> {
                if (status >= 400 && status < 500) {
                    yield new BadRequestException("Client error from external service: " + methodKey + " (Status: " + status + ")");
                } else if (status >= 500) {
                    yield new ExternalServiceException("Server error from external service: " + methodKey + " (Status: " + status + ")");
                } else {
                    yield new ExternalServiceException("Unexpected error from external service: " + methodKey + " (Status: " + status + ")");
                }
            }
        };
    }
}
