package pt.ulusofona.ulht.credential.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pt.ulusofona.ulht.credential.exception.ExternalServiceException;

/**
 * WaltID client configuration for credential service.
 * Independent configuration for the credential service.
 */
@Slf4j
@Configuration
public class WaltidClientConfiguration {

    @Bean
    public Logger.Level feignLoggerLevel() {
        // BASIC avoids logging request/response headers and bodies (which may
        // contain credentials, cookies, or PII) while still logging method/url/status.
        return Logger.Level.BASIC;
    }

    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(
            10000,  // connect timeout in milliseconds
            java.util.concurrent.TimeUnit.MILLISECONDS,
            60000,  // read timeout in milliseconds
            java.util.concurrent.TimeUnit.MILLISECONDS,
            true    // follow redirects
        );
    }

    @Bean
    public Retryer retryer() {
        return new Retryer.Default(
            1000L,    // initial interval
            3000L,    // max interval
            3         // max attempts
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new WaltidErrorDecoder();
    }

    /**
     * Custom error decoder for WaltID clients
     */
    private static class WaltidErrorDecoder implements ErrorDecoder {

        @Override
        public Exception decode(String methodKey, feign.Response response) {
            String errorMessage = extractErrorMessage(response);
            log.error("WaltID API error: {} {} - {} | Message: {}", 
                    response.status(), methodKey, response.reason(), errorMessage);
            
            switch (response.status()) {
                case 400:
                    return new ExternalServiceException(
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_BAD_REQUEST.getDescription() + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_BAD_REQUEST.getCode()
                    );
                case 401:
                    // Include the actual error message (e.g., "Unknown user") in the exception
                    String unauthorizedMsg = pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_UNAUTHORIZED.getDescription();
                    if (errorMessage != null && !errorMessage.isEmpty()) {
                        unauthorizedMsg += " - " + errorMessage;
                    }
                    return new ExternalServiceException(
                        unauthorizedMsg, 
                        "WaltID", 
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_UNAUTHORIZED.getCode()
                    );
                case 403:
                    return new ExternalServiceException(
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_FORBIDDEN.getDescription() + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_FORBIDDEN.getCode()
                    );
                case 404:
                    return new ExternalServiceException(
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_NOT_FOUND.getDescription() + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_NOT_FOUND.getCode()
                    );
                case 409:
                    return new ExternalServiceException(
                        "WaltID service error: 409 Conflict - Resource already exists (e.g., user already registered)" + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        "CRED-WALTID-409"
                    );
                case 500:
                    return new ExternalServiceException(
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_SERVER_ERROR.getDescription() + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_SERVER_ERROR.getCode()
                    );
                case 503:
                    return new ExternalServiceException(
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_UNAVAILABLE.getDescription() + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        pt.ulusofona.ulht.credential.exception.ErrorCodes.WALTID_UNAVAILABLE.getCode()
                    );
                default:
                    return new ExternalServiceException(
                        String.format("WaltID service error: %d", response.status()) + 
                        (errorMessage != null ? " - " + errorMessage : ""), 
                        "WaltID", 
                        String.format("CRED-WALTID-%d", response.status())
                    );
            }
        }
        
        /**
         * Extracts error message from response body
         */
        private String extractErrorMessage(feign.Response response) {
            try {
                if (response.body() != null) {
                    try (java.io.InputStream bodyStream = response.body().asInputStream()) {
                        byte[] bodyData = bodyStream.readAllBytes();
                        if (bodyData != null && bodyData.length > 0) {
                            String bodyText = new String(bodyData, java.nio.charset.StandardCharsets.UTF_8);
                            log.debug("Extracted response body: {}", bodyText);
                            
                            // Try to parse as JSON and extract meaningful fields
                            try {
                                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(bodyText);
                                
                                // Check for various error message fields
                                if (jsonNode.has("message")) {
                                    String msg = jsonNode.get("message").asText();
                                    if (msg != null && !msg.isEmpty()) {
                                        return msg;
                                    }
                                }
                                if (jsonNode.has("error")) {
                                    String err = jsonNode.get("error").asText();
                                    if (err != null && !err.isEmpty()) {
                                        return err;
                                    }
                                }
                                // Check for "id" field (e.g., "Unauth")
                                if (jsonNode.has("id")) {
                                    String id = jsonNode.get("id").asText();
                                    if (id != null && !id.isEmpty()) {
                                        return id;
                                    }
                                }
                                // Check for exception field
                                if (jsonNode.has("exception") && jsonNode.get("exception").asBoolean()) {
                                    // If there's an id field, use it
                                    if (jsonNode.has("id")) {
                                        return jsonNode.get("id").asText();
                                    }
                                    return "Exception occurred";
                                }
                                
                                // Return the full JSON if no specific field found
                                return bodyText.length() > 200 ? bodyText.substring(0, 200) + "..." : bodyText;
                            } catch (Exception e) {
                                // Not JSON, check for plain text error messages
                                String bodyLower = bodyText.toLowerCase();
                                if (bodyLower.contains("unknown user")) {
                                    return bodyText.trim();
                                }
                                // Return as plain text (truncated if too long)
                                return bodyText.length() > 200 ? bodyText.substring(0, 200) + "..." : bodyText;
                            }
                        }
                    }
                }
            } catch (java.io.IOException e) {
                log.warn("Failed to read response body (may have been consumed already): {}", e.getMessage());
            } catch (Exception e) {
                log.warn("Failed to extract error message from response body: {}", e.getMessage());
            }
            return null;
        }
    }
}
