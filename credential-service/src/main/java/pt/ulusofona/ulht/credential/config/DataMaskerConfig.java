package pt.ulusofona.ulht.credential.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for data masking functionality.
 * Reads settings from logging.security section in application.yml
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "logging.security")
public class DataMaskerConfig {
    
    /**
     * Whether data masking is enabled
     */
    private boolean enabled = true;
    
    /**
     * List of sensitive field names to mask (case-insensitive)
     */
    private List<String> sensitiveFields = new ArrayList<>();
    
    /**
     * String to use for masking (default: "###")
     */
    private String maskString = "###";
    
    /**
     * Whether to mask request bodies
     */
    private boolean maskRequestBody = true;
    
    /**
     * Whether to mask response bodies
     */
    private boolean maskResponseBody = true;
    
    /**
     * Whether to mask headers
     */
    private boolean maskHeaders = true;
    
    /**
     * List of sensitive header names to mask (case-insensitive)
     */
    private List<String> sensitiveHeaders = new ArrayList<>();
}





