package com.example.dcs.credential.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service to check credential expiration and prevent duplicate issuance.
 * 
 * This service queries the wallet for existing credentials and checks if they are expired.
 * Only issues new credentials if:
 * - No credential of the same type exists, OR
 * - All existing credentials of that type are expired
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialExpirationChecker {
    
    private final WaltidService waltidService;
    
    // Simple in-memory cache to track recently issued credentials (within last 1 minute)
    // This prevents immediate duplicate issuance within the same workflow run
    // Key: walletId:credentialType, Value: timestamp when issued
    private final Map<String, Long> recentlyIssuedCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 60 * 1000; // 1 minute (reduced from 5 minutes)
    
    /**
     * Checks if a credential of the given type should be issued.
     * 
     * @param walletId Wallet ID to check
     * @param credentialType Type of credential to check (e.g., "EuropeanStudentCard")
     * @param sessionCookie Session cookie for wallet authentication
     * @param userId User identifier for logging
     * @return true if credential should be issued (missing or expired), false if valid credential exists
     */
    public boolean shouldIssueCredential(String walletId, String credentialType, 
                                        String sessionCookie, String userId) {
        log.debug("Checking if credential {} should be issued for wallet {} (user: {})", 
                credentialType, walletId, userId);
        
        // First check: Query wallet for existing credentials
        try {
            List<Map<String, Object>> credentials = waltidService.getWalletCredentials(
                walletId, sessionCookie, userId);
            
            if (credentials == null || credentials.isEmpty()) {
                log.debug("No credentials found in wallet - will issue new credential");
                // Clear cache entry if credentials don't exist in wallet
                String cacheKey = walletId + ":" + credentialType;
                recentlyIssuedCache.remove(cacheKey);
                return true; // No credentials, issue new one
            }
            
            // Filter credentials by type (check both top-level and parsedDocument)
            List<Map<String, Object>> matchingCredentials = credentials.stream()
                .filter(cred -> {
                    // Check top-level credential
                    if (matchesCredentialType(cred, credentialType)) {
                        return true;
                    }
                    // Check parsedDocument if it exists
                    Object parsedDocObj = cred.get("parsedDocument");
                    if (parsedDocObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> parsedDoc = (Map<String, Object>) parsedDocObj;
                        return matchesCredentialType(parsedDoc, credentialType);
                    }
                    return false;
                })
                .collect(Collectors.toList());
            
            if (matchingCredentials.isEmpty()) {
                log.debug("No credentials of type {} found - will issue new credential", credentialType);
                // Clear cache entry if no matching credentials exist
                String cacheKey = walletId + ":" + credentialType;
                recentlyIssuedCache.remove(cacheKey);
                return true; // No matching credential type, issue new one
            }
            
            // Check if any existing credential is still valid (not expired)
            for (Map<String, Object> credential : matchingCredentials) {
                // Try to get expiration/issuance dates from parsedDocument first, then top-level
                Map<String, Object> credentialData = credential;
                Object parsedDocObj = credential.get("parsedDocument");
                if (parsedDocObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> parsedDoc = (Map<String, Object>) parsedDocObj;
                    credentialData = parsedDoc; // Use parsedDocument for date extraction
                }
                
                String expirationDate = extractExpirationDate(credentialData);
                String issuanceDate = extractIssuanceDate(credentialData);
                
                if (expirationDate != null) {
                    // Has expiration date - check if expired
                    if (isExpired(expirationDate)) {
                        log.info("Found expired credential of type {} (expired: {}) - will issue new credential", 
                                credentialType, expirationDate);
                        continue; // This one is expired, check next
                    } else {
                        log.info("Found valid credential of type {} (expires: {}) - skipping issuance", 
                                credentialType, expirationDate);
                        return false; // Valid credential exists, don't issue
                    }
                } else {
                    // No expiration date - check if credential is very old (more than 1 year)
                    if (issuanceDate != null && isVeryOld(issuanceDate)) {
                        log.info("Found very old credential of type {} (issued: {}) without expiration - will issue new credential", 
                                credentialType, issuanceDate);
                        continue; // Very old, check next
                    } else {
                        // No expiration date but not very old - check cache to prevent rapid re-issuance
                        String cacheKey = walletId + ":" + credentialType;
                        Long lastIssued = recentlyIssuedCache.get(cacheKey);
                        long now = System.currentTimeMillis();
                        
                        // Only use cache if credential was issued very recently (within 30 seconds)
                        // This prevents duplicate issuance within the same workflow run
                        if (lastIssued != null && (now - lastIssued) < 30000) { // 30 seconds instead of 5 minutes
                            log.info("Skipping credential {} - recently issued {} seconds ago (within same workflow)", 
                                    credentialType, (now - lastIssued) / 1000);
                            return false; // Recently issued in same workflow, skip
                        }
                        
                        // No expiration date but not very old - treat as valid
                        log.info("Found valid credential of type {} without expiration date (issued: {}) - skipping issuance", 
                                credentialType, issuanceDate != null ? issuanceDate : "unknown");
                        return false; // Valid credential exists, don't issue
                    }
                }
            }
            
            // If we get here, all matching credentials are expired or very old
            log.info("All existing credentials of type {} are expired or very old - will issue new credential", credentialType);
            return true;
            
        } catch (Exception e) {
            log.warn("Failed to check existing credentials for wallet {} (user: {}): {}. Will issue new credential.", 
                    walletId, userId, e.getMessage());
            // On error, issue new credential to be safe
            return true;
        }
    }
    
    /**
     * Records that a credential was just issued (for duplicate prevention)
     * 
     * @param walletId Wallet ID
     * @param credentialType Credential type
     */
    public void recordIssuance(String walletId, String credentialType) {
        String cacheKey = walletId + ":" + credentialType;
        recentlyIssuedCache.put(cacheKey, System.currentTimeMillis());
        
        // Clean up old entries (older than cache duration)
        recentlyIssuedCache.entrySet().removeIf(entry -> 
            (System.currentTimeMillis() - entry.getValue()) > CACHE_DURATION_MS);
    }
    
    /**
     * Checks if a credential matches the given type
     * 
     * @param credential Credential map
     * @param credentialType Type to match (e.g., "EuropeanStudentCard")
     * @return true if credential matches the type
     */
    private boolean matchesCredentialType(Map<String, Object> credential, String credentialType) {
        if (credential == null || credentialType == null) {
            return false;
        }
        
        // Normalize credential type for comparison (remove spaces, convert to lowercase)
        String normalizedType = credentialType.toLowerCase().replaceAll("\\s+", "");
        
        // Check type field (can be a string or list)
        Object typeObj = credential.get("type");
        if (typeObj != null) {
            if (typeObj instanceof String) {
                String typeStr = ((String) typeObj).toLowerCase().replaceAll("\\s+", "");
                if (typeStr.contains(normalizedType) || normalizedType.contains(typeStr)) {
                    log.debug("Matched credential type: {} with {}", credentialType, typeStr);
                    return true;
                }
            } else if (typeObj instanceof List) {
                List<?> types = (List<?>) typeObj;
                for (Object t : types) {
                    if (t instanceof String) {
                        String typeStr = ((String) t).toLowerCase().replaceAll("\\s+", "");
                        // Skip "VerifiableCredential" as it's a base type present in all credentials
                        if (!"verifiablecredential".equals(typeStr) && 
                            (typeStr.contains(normalizedType) || normalizedType.contains(typeStr))) {
                            log.debug("Matched credential type: {} with {}", credentialType, typeStr);
                            return true;
                        }
                    }
                }
            }
        }
        
        // Also check credentialSubject for type hints
        Object credentialSubject = credential.get("credentialSubject");
        if (credentialSubject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subject = (Map<String, Object>) credentialSubject;
            Object subjType = subject.get("type");
            if (subjType instanceof String) {
                String typeStr = ((String) subjType).toLowerCase().replaceAll("\\s+", "");
                if (typeStr.contains(normalizedType) || normalizedType.contains(typeStr)) {
                    log.debug("Matched credential type via credentialSubject: {} with {}", credentialType, typeStr);
                    return true;
                }
            }
        }
        
        log.debug("No match found for credential type: {} in credential", credentialType);
        return false;
    }
    
    /**
     * Checks if an expiration date has passed
     * 
     * @param expirationDate ISO-8601 formatted expiration date string
     * @return true if expired, false if still valid
     */
    public boolean isExpired(String expirationDate) {
        if (expirationDate == null || expirationDate.isEmpty()) {
            return true; // No expiration date means we should treat as expired
        }
        
        try {
            Instant expiration = Instant.parse(expirationDate);
            Instant now = Instant.now();
            return now.isAfter(expiration);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse expiration date: {}. Treating as expired.", expirationDate);
            return true; // If we can't parse, treat as expired and issue new
        }
    }
    
    /**
     * Extracts expiration date from a credential object
     * 
     * @param credential Credential map (from wallet)
     * @return Expiration date string or null if not found
     */
    public String extractExpirationDate(Map<String, Object> credential) {
        if (credential == null) {
            return null;
        }
        
        // Try to get expirationDate from credential
        Object expirationDate = credential.get("expirationDate");
        if (expirationDate != null) {
            return expirationDate.toString();
        }
        
        // Try validThrough (alternative field name)
        Object validThrough = credential.get("validThrough");
        if (validThrough != null) {
            return validThrough.toString();
        }
        
        // Try validUntil (another alternative)
        Object validUntil = credential.get("validUntil");
        if (validUntil != null) {
            return validUntil.toString();
        }
        
        // Try to get from credentialSubject if it's nested
        Object credentialSubject = credential.get("credentialSubject");
        if (credentialSubject instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> subject = (Map<String, Object>) credentialSubject;
            Object subjExpiration = subject.get("expirationDate");
            if (subjExpiration != null) {
                return subjExpiration.toString();
            }
        }
        
        return null;
    }
    
    /**
     * Extracts issuance date from a credential object
     * 
     * @param credential Credential map (from wallet)
     * @return Issuance date string or null if not found
     */
    private String extractIssuanceDate(Map<String, Object> credential) {
        if (credential == null) {
            return null;
        }
        
        // Try issuanceDate
        Object issuanceDate = credential.get("issuanceDate");
        if (issuanceDate != null) {
            return issuanceDate.toString();
        }
        
        // Try issued (alternative field name)
        Object issued = credential.get("issued");
        if (issued != null) {
            return issued.toString();
        }
        
        // Try createdAt
        Object createdAt = credential.get("createdAt");
        if (createdAt != null) {
            return createdAt.toString();
        }
        
        return null;
    }
    
    /**
     * Checks if a credential is very old (more than 1 year)
     * Used for credentials without expiration dates
     * 
     * @param issuanceDate ISO-8601 formatted issuance date string
     * @return true if credential is more than 1 year old
     */
    private boolean isVeryOld(String issuanceDate) {
        if (issuanceDate == null || issuanceDate.isEmpty()) {
            return false; // Can't determine age, treat as not very old
        }
        
        try {
            Instant issued = Instant.parse(issuanceDate);
            Instant oneYearAgo = Instant.now().minusSeconds(365L * 24 * 60 * 60); // 1 year ago
            return issued.isBefore(oneYearAgo);
        } catch (DateTimeParseException e) {
            log.debug("Failed to parse issuance date: {}. Treating as not very old.", issuanceDate);
            return false; // If we can't parse, don't treat as very old
        }
    }
}

