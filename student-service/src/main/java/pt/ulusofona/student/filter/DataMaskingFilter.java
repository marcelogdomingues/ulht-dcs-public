package pt.ulusofona.student.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import pt.ulusofona.student.config.DataMaskerConfig;
import pt.ulusofona.student.util.DataMasker;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * HTTP filter that masks sensitive data in request and response bodies and headers.
 * Works with the DataMasker to mask fields defined in application.yml
 */
@Slf4j
@Component
public class DataMaskingFilter extends OncePerRequestFilter {
    
    private final DataMasker dataMasker;
    private final DataMaskerConfig config;
    
    public DataMaskingFilter(DataMasker dataMasker, DataMaskerConfig config) {
        this.dataMasker = dataMasker;
        this.config = config;
    }
    
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                   @NonNull HttpServletResponse response, 
                                   @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        if (!config.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Wrap request and response to cache content.
        // Spring Framework 7 removed the single-arg constructor; a content-cache
        // limit (bytes) is now required. 1 MB is ample for these JSON bodies.
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request, 1024 * 1024);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            // Log masked request
            logRequest(wrappedRequest);
            
            // Log masked response
            logResponse(wrappedResponse);
            
            // Copy response back to original
            wrappedResponse.copyBodyToResponse();
        }
    }
    
    /**
     * Logs the request with masked sensitive data
     */
    private void logRequest(ContentCachingRequestWrapper request) {
        if (log.isDebugEnabled()) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Request: ").append(request.getMethod()).append(" ").append(request.getRequestURI());
            
            // Log masked headers
            if (config.isMaskHeaders()) {
                logMessage.append("\nHeaders: ");
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    String maskedValue = dataMasker.maskHeader(headerName, headerValue);
                    logMessage.append(headerName).append("=").append(maskedValue);
                    if (headerNames.hasMoreElements()) {
                        logMessage.append(", ");
                    }
                }
            }
            
            // Log masked body
            if (config.isMaskRequestBody()) {
                byte[] content = request.getContentAsByteArray();
                if (content.length > 0) {
                    try {
                        String encoding = request.getCharacterEncoding();
                        if (encoding == null) {
                            encoding = StandardCharsets.UTF_8.name();
                        }
                        String body = new String(content, encoding);
                        String maskedBody = dataMasker.maskJson(body);
                        logMessage.append("\nBody: ").append(maskedBody);
                    } catch (UnsupportedEncodingException e) {
                        // Fallback to UTF-8
                        String body = new String(content, StandardCharsets.UTF_8);
                        String maskedBody = dataMasker.maskJson(body);
                        logMessage.append("\nBody: ").append(maskedBody);
                    }
                }
            }
            
            log.debug(logMessage.toString());
        }
    }
    
    /**
     * Logs the response with masked sensitive data
     */
    private void logResponse(ContentCachingResponseWrapper response) {
        if (log.isDebugEnabled()) {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("Response: ").append(response.getStatus());
            
            // Log masked headers
            if (config.isMaskHeaders()) {
                logMessage.append("\nHeaders: ");
                response.getHeaderNames().forEach(headerName -> {
                    String headerValue = response.getHeader(headerName);
                    String maskedValue = dataMasker.maskHeader(headerName, headerValue);
                    logMessage.append(headerName).append("=").append(maskedValue).append(", ");
                });
            }
            
            // Log masked body
            if (config.isMaskResponseBody()) {
                byte[] content = response.getContentAsByteArray();
                if (content.length > 0) {
                    try {
                        String encoding = response.getCharacterEncoding();
                        if (encoding == null) {
                            encoding = StandardCharsets.UTF_8.name();
                        }
                        String body = new String(content, encoding);
                        String maskedBody = dataMasker.maskJson(body);
                        logMessage.append("\nBody: ").append(maskedBody);
                    } catch (UnsupportedEncodingException e) {
                        // Fallback to UTF-8
                        String body = new String(content, StandardCharsets.UTF_8);
                        String maskedBody = dataMasker.maskJson(body);
                        logMessage.append("\nBody: ").append(maskedBody);
                    }
                }
            }
            
            log.debug(logMessage.toString());
        }
    }
}





