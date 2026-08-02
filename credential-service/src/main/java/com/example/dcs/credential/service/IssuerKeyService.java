package com.example.dcs.credential.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.dcs.credential.dto.waltid.OnboardIssuerResponse;
import com.example.dcs.credential.exception.ExternalServiceException;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages issuer keys and DIDs for the institution
 * In production, this should use external KMS or secure storage
 */
@Slf4j
@Service
public class IssuerKeyService {
    
    private final WaltidService waltidService;
    private final ObjectMapper objectMapper;
    
    @Value("${waltid.issuer.defaults.key-type:secp256r1}")
    private String defaultKeyType;
    
    @Value("${waltid.issuer.defaults.did-method:jwk}")
    private String defaultDidMethod;
    
    // In-memory storage (for development/testing)
    // In production, use external KMS or secure database
    private final AtomicReference<OnboardIssuerResponse> institutionIssuerCredentials = new AtomicReference<>();
    
    public IssuerKeyService(WaltidService waltidService) {
        this.waltidService = waltidService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Initialize issuer credentials on startup (optional)
     */
    @PostConstruct
    public void initialize() {
        log.info("IssuerKeyService initialized. Use getOrCreateIssuerCredentials() to create institution credentials.");
        log.warn("⚠️  Using in-memory issuer key storage. For production, integrate with external KMS!");
    }
    
    /**
     * Gets existing issuer credentials or creates new ones
     */
    public OnboardIssuerResponse getOrCreateIssuerCredentials() throws ExternalServiceException {
        OnboardIssuerResponse existing = institutionIssuerCredentials.get();
        
        if (existing != null) {
            log.debug("Using existing issuer credentials with DID: {}", existing.getIssuerDid());
            return existing;
        }
        
        log.info("No issuer credentials found, creating new ones...");
        return createAndStoreIssuerCredentials();
    }
    
    /**
     * Creates and stores new issuer credentials
     */
    public synchronized OnboardIssuerResponse createAndStoreIssuerCredentials() throws ExternalServiceException {
        // Double-check in case another thread created it
        OnboardIssuerResponse existing = institutionIssuerCredentials.get();
        if (existing != null) {
            return existing;
        }
        
        log.info("Creating new issuer credentials with keyType: {} and didMethod: {}", 
                defaultKeyType, defaultDidMethod);
        
        OnboardIssuerResponse response = waltidService.onboardIssuer(defaultKeyType, defaultDidMethod);
        
        if (response != null && response.getIssuerDid() != null) {
            institutionIssuerCredentials.set(response);
            log.info("✅ Institution issuer credentials created and stored. DID: {}", response.getIssuerDid());
            log.warn("⚠️  IMPORTANT: Issuer credentials are stored in memory and will be lost on restart!");
            log.warn("⚠️  For production, integrate with external KMS (Hashicorp Vault, Oracle KMS, etc.)");
        } else {
            log.error("Failed to create issuer credentials - response was null or invalid");
            throw new ExternalServiceException("Failed to create issuer credentials", "IssuerKeyService");
        }
        
        return response;
    }
    
    /**
     * Gets the current issuer DID
     */
    public String getIssuerDid() throws ExternalServiceException {
        OnboardIssuerResponse credentials = getOrCreateIssuerCredentials();
        return credentials.getIssuerDid();
    }
    
    /**
     * Gets the current issuer key
     */
    public Object getIssuerKey() throws ExternalServiceException {
        OnboardIssuerResponse credentials = getOrCreateIssuerCredentials();
        return credentials.getIssuerKey();
    }
    
    /**
     * Manually set issuer credentials (for testing or manual configuration)
     */
    public void setIssuerCredentials(OnboardIssuerResponse credentials) {
        if (credentials != null && credentials.getIssuerDid() != null) {
            institutionIssuerCredentials.set(credentials);
            log.info("Issuer credentials manually set with DID: {}", credentials.getIssuerDid());
        } else {
            log.warn("Attempted to set invalid issuer credentials");
        }
    }
    
    /**
     * Clears stored credentials (useful for testing or key rotation)
     */
    public void clearCredentials() {
        institutionIssuerCredentials.set(null);
        log.info("Issuer credentials cleared");
    }
    
    /**
     * Checks if issuer credentials are available
     */
    public boolean hasCredentials() {
        return institutionIssuerCredentials.get() != null;
    }
}
