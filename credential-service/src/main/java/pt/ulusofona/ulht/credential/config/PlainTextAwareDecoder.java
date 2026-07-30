package pt.ulusofona.ulht.credential.config;

import feign.FeignException;
import feign.Response;
import feign.codec.Decoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Custom decoder that can handle both JSON and plain text responses.
 * Falls back to plain text decoding when JSON parsing fails.
 */
public class PlainTextAwareDecoder implements Decoder {

    private static final Logger logger = LoggerFactory.getLogger(PlainTextAwareDecoder.class);
    private final Decoder jsonDecoder;

    public PlainTextAwareDecoder(Decoder jsonDecoder) {
        this.jsonDecoder = jsonDecoder;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException, FeignException {
        logger.debug("PlainTextAwareDecoder.decode called with type: {}", type);
        logger.debug("Response status: {}, headers: {}", response.status(), response.headers());
        
        // Check if the response indicates plain text content
        boolean isPlainTextResponse = isPlainTextResponse(response);
        logger.debug("Is plain text response: {}", isPlainTextResponse);
        
        // Handle ResponseEntity types specially
        if (type instanceof ParameterizedType && 
            ((ParameterizedType) type).getRawType() == org.springframework.http.ResponseEntity.class) {
            return handleResponseEntityType(response, (ParameterizedType) type, isPlainTextResponse);
        }
        
        // If the expected type is String or it's a plain text response, try plain text first
        if (type == String.class || isPlainTextResponse) {
            try {
                String plainText = decodeAsPlainText(response);
                logger.debug("Decoded plain text response: {}", plainText);
                
                // If we expect a String, return it directly
                if (type == String.class) {
                    return plainText;
                }
            } catch (Exception e) {
                logger.debug("Failed to decode as plain text, trying JSON: {}", e.getMessage());
            }
        }

        // Try JSON decoding first
        try {
            logger.debug("Attempting JSON decoding");
            Object result = jsonDecoder.decode(response, type);
            logger.debug("JSON decoding successful: {}", result);
            return result;
        } catch (Exception e) {
            logger.debug("JSON decoding failed: {}", e.getMessage());
            
            // If JSON fails and we expect a String, try plain text
            if (type == String.class) {
                return decodeAsPlainText(response);
            }
            
            // Re-throw the original exception if we can't handle it
            logger.error("Failed to decode response for type: {}", type, e);
            throw e;
        }
    }

    private Object handleResponseEntityType(Response response, ParameterizedType type, boolean isPlainTextResponse) throws IOException {
        Type bodyType = type.getActualTypeArguments()[0];
        logger.debug("Handling ResponseEntity with body type: {}", bodyType);
        
        Object body;
        
        // If it's a plain text response or the body type is String, try plain text first
        if (isPlainTextResponse || bodyType == String.class) {
            try {
                body = decodeAsPlainText(response);
                logger.debug("Decoded plain text body for ResponseEntity: {}", body);
            } catch (Exception e) {
                logger.debug("Failed to decode as plain text, trying JSON: {}", e.getMessage());
                body = decodeBodyAsJson(response, bodyType);
            }
        } else {
            // Try JSON decoding for the body
            body = decodeBodyAsJson(response, bodyType);
        }
        
        // Convert Feign Response headers to Spring HttpHeaders
        HttpHeaders httpHeaders = convertHeaders(response.headers());
        logger.debug("Converted {} headers from Feign Response to Spring HttpHeaders", httpHeaders.size());
        
        // Create ResponseEntity with the decoded body AND headers
        return new org.springframework.http.ResponseEntity<>(body, httpHeaders, 
            HttpStatus.valueOf(response.status()));
    }
    
    /**
     * Converts Feign Response headers to Spring HttpHeaders.
     * This ensures all headers (including Set-Cookie) are preserved.
     */
    private HttpHeaders convertHeaders(java.util.Map<String, Collection<String>> feignHeaders) {
        HttpHeaders httpHeaders = new HttpHeaders();
        
        if (feignHeaders != null) {
            for (String headerName : feignHeaders.keySet()) {
                Collection<String> headerValues = feignHeaders.get(headerName);
                if (headerValues != null && !headerValues.isEmpty()) {
                    // HttpHeaders.addAll() handles multiple values correctly
                    httpHeaders.addAll(headerName, headerValues instanceof List ? 
                        (List<String>) headerValues : List.copyOf(headerValues));
                }
            }
        }
        
        return httpHeaders;
    }
    
    private Object decodeBodyAsJson(Response response, Type bodyType) throws IOException {
        try {
            // Use the original response directly instead of creating a new one
            // This avoids the "original request is required" error
            return jsonDecoder.decode(response, bodyType);
        } catch (Exception e) {
            logger.debug("JSON decoding of body failed: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isPlainTextResponse(Response response) {
        Collection<String> contentTypeHeaders = response.headers().get("Content-Type");
        if (contentTypeHeaders != null) {
            for (String contentType : contentTypeHeaders) {
                logger.debug("Content-Type header: {}", contentType);
                if (contentType != null && contentType.toLowerCase().contains("text/plain")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String decodeAsPlainText(Response response) throws IOException {
        if (response.body() == null) {
            logger.debug("Response body is null");
            return null;
        }
        
        byte[] bodyData = response.body().asInputStream().readAllBytes();
        String plainText = new String(bodyData, StandardCharsets.UTF_8);
        logger.debug("Decoded plain text: {}", plainText);
        return plainText;
    }
} 