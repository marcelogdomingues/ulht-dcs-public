package com.example.dcs.credential.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Loads credential templates from application.yml
 * 
 * Example configuration:
 * <pre>
 * credentials:
 *   templates:
 *     - id: educational-id
 *       type: EducationalID
 *       enabled: true
 *       waltidConfigId: UniversityDegree_jwt_vc_json
 *       fieldMappings:
 *         studentId: [studentId, studentCode]
 *         givenName: [firstName, givenName]
 * </pre>
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "credentials")
public class CredentialTemplateConfig {
    
    /**
     * List of credential templates configured in application.yml
     */
    private List<CredentialTemplate> templates = new ArrayList<>();
    
    @PostConstruct
    public void init() {
        log.info("📋 Loaded {} credential template(s)", templates.size());
        
        List<CredentialTemplate> enabled = getEnabledTemplates();
        log.info("✅ {} credential template(s) enabled:", enabled.size());
        
        enabled.forEach(template -> 
            log.info("   - {} (type: {}, waltid: {})", 
                template.getId(), 
                template.getType(), 
                template.getWaltidConfigId())
        );
        
        List<CredentialTemplate> disabled = templates.stream()
            .filter(t -> !t.isEnabled())
            .toList();
        
        if (!disabled.isEmpty()) {
            log.info("⏸️  {} credential template(s) disabled:", disabled.size());
            disabled.forEach(template -> log.info("   - {}", template.getId()));
        }
    }
    
    /**
     * Get all enabled credential templates, sorted by priority
     */
    public List<CredentialTemplate> getEnabledTemplates() {
        return templates.stream()
            .filter(CredentialTemplate::isEnabled)
            .sorted((a, b) -> Integer.compare(a.getPriority(), b.getPriority()))
            .collect(Collectors.toList());
    }
    
    /**
     * Get a specific template by ID
     */
    public CredentialTemplate getTemplate(String id) {
        return templates.stream()
            .filter(t -> t.getId().equals(id))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if a template is enabled
     */
    public boolean isTemplateEnabled(String id) {
        return templates.stream()
            .anyMatch(t -> t.getId().equals(id) && t.isEnabled());
    }
}

