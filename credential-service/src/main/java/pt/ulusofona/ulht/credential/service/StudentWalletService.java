package pt.ulusofona.ulht.credential.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pt.ulusofona.ulht.credential.domain.authentication.LoginRequest;
import pt.ulusofona.ulht.credential.domain.authentication.RegisterRequest;
import pt.ulusofona.ulht.credential.domain.wallet.WalletAccountResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletDidResponse;
import pt.ulusofona.ulht.credential.exception.ExternalServiceException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing student wallets.
 * 
 * This service handles:
 * - Wallet creation (lazy - on first credential request)
 * - Wallet authentication
 * - Retrieving student's DID from their wallet
 * 
 * Each student gets their own wallet in WaltID Wallet service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentWalletService {

    private final WaltidService waltidService;
    
    @Value("${wallet.password.secret}")
    private String walletPasswordSecret;

    @Value("${wallet.password.salt}")
    private String walletPasswordSalt;
    
    // Track emails currently being processed to prevent duplicate registration attempts
    private final Set<String> processingEmails = ConcurrentHashMap.newKeySet();

    /**
     * Masks an email for logging so PII is not written to logs.
     * e.g. "a12345@students.example.edu" -> "a1***@students.example.edu"
     */
    private static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "<none>";
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        String visible = local.length() <= 2 ? local.substring(0, 1) : local.substring(0, 2);
        return visible + "***" + domain;
    }
    
    /**
     * Ensures a wallet exists for the student.
     * Creates the wallet if it doesn't exist (lazy creation).
     * 
     * @param studentData Student data map containing email, name, etc.
     * @return StudentWalletInfo with wallet details
     */
    public StudentWalletInfo ensureWalletExists(Map<String, Object> studentData) throws ExternalServiceException {
        // Extract student information
        String studentId = extractStudentId(studentData);
        String fullName = extractFullName(studentData);
        
        // IMPORTANT: Always use studentId-based email for wallet operations to ensure consistency
        // This ensures the same email is used for wallet registration and credential fetching
        String email;
        if (studentId != null && !studentId.isEmpty()) {
            email = studentId + "@students.example.edu";
            log.info("Using student email for wallet operations: {} (studentId: {})", maskEmail(email), studentId);
        } else {
            // Fallback to extracted email if studentId is not available
            email = extractEmail(studentData);
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Either studentId or email is required for wallet creation");
            }
            log.warn("Using extracted email (no studentId): {} - this may cause wallet access issues", maskEmail(email));
        }
        
        log.info("Ensuring wallet exists for student: {} ({})", maskEmail(email), studentId);
        
        // Prevent concurrent processing of the same email
        boolean alreadyProcessing = !processingEmails.add(email);
        if (alreadyProcessing) {
            log.warn("⚠️  Wallet creation already in progress for student: {}, waiting briefly...", maskEmail(email));
            // Wait a bit for the other process to complete
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Try login again - maybe the other process created it
            try {
                String password = generateWalletPassword(studentId, email);
                LoginRequest loginRequest = LoginRequest.builder()
                        .email(email)
                        .password(password)
                        .type("email")
                        .build();
                var loginResponse = waltidService.walletLogin(loginRequest);
                if (loginResponse != null && loginResponse.getSessionCookie() != null) {
                    log.info("✅ Concurrent wallet creation succeeded, login successful for: {}", maskEmail(email));
                    processingEmails.remove(email);
                    return getStudentWalletInfo(email, loginResponse.getSessionCookie());
                }
            } catch (Exception e) {
                log.debug("Concurrent login attempt failed, continuing with normal flow: {}", e.getMessage());
            }
        }
        
        // Generate password from student data (deterministic)
        String password = generateWalletPassword(studentId, email);
        
        // Try to login first (if wallet exists) - always try login before registration
        try {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .type("email")
                    .build();
            
            log.debug("Attempting login for student: {}", maskEmail(email));
            var loginResponse = waltidService.walletLogin(loginRequest);
            
            // Validate login response
            if (loginResponse == null) {
                log.warn("Login response is null for student: {}, will try registration", maskEmail(email));
                throw new ExternalServiceException("Login response is null", "StudentWalletService");
            }
            
            String sessionCookie = loginResponse.getSessionCookie();
            if (sessionCookie == null || sessionCookie.trim().isEmpty()) {
                log.warn("Session cookie is null/empty after login for student: {}, will try registration", maskEmail(email));
                throw new ExternalServiceException("Session cookie is null or empty", "StudentWalletService");
            }
            
            log.info("✅ Login successful - wallet already exists for student: {} (session cookie length: {})",
                    maskEmail(email), sessionCookie.length());
            
            // Wallet exists, get wallet info - this should NEVER trigger registration
            try {
                StudentWalletInfo walletInfo = getStudentWalletInfo(email, sessionCookie);
                log.info("✅ Successfully retrieved wallet info for student: {} (walletId: {}, subjectDid: {})",
                        maskEmail(email), walletInfo.getWalletId(), walletInfo.getSubjectDid());
                processingEmails.remove(email);
                return walletInfo;
            } catch (Exception e) {
                // If getting wallet info fails, don't try to register - wallet exists, this is a different error
                log.error("❌ Failed to get wallet info for existing wallet (student: {}): {}", maskEmail(email), e.getMessage(), e);
                throw new ExternalServiceException(
                    "Wallet exists but failed to retrieve wallet information: " + e.getMessage(), 
                    "StudentWalletService");
            }
            
        } catch (ExternalServiceException e) {
            // Check if it's a login failure that indicates wallet doesn't exist
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            String lowerErrorMsg = errorMsg.toLowerCase();
            if (errorMsg.contains("401") || 
                errorMsg.contains("Unauthorized") ||
                errorMsg.contains("404") ||
                errorMsg.contains("Not Found") ||
                errorMsg.contains("Invalid credentials") ||
                errorMsg.contains("Login response is null") ||
                errorMsg.contains("Session cookie is null") ||
                lowerErrorMsg.contains("unknown user") ||
                lowerErrorMsg.contains("user not found") ||
                lowerErrorMsg.contains("user does not exist") ||
                lowerErrorMsg.contains("unauth")) {
                // Wallet doesn't exist or login failed, try to create it
                log.info("Login failed or incomplete (wallet may not exist) for student: {}, will create new wallet... Error: {}",
                        maskEmail(email), errorMsg);
                try {
                    return createWalletForStudent(email, fullName, password, studentId);
                } finally {
                    processingEmails.remove(email);
                }
            } else {
                // Other errors (network, server error, etc.) - rethrow
                log.error("Login failed with unexpected error for student: {}: {}", maskEmail(email), errorMsg, e);
                processingEmails.remove(email);
                throw e;
            }
        } catch (Exception e) {
            // Other unexpected exceptions - check if it's a login failure
            String errorMsg = e.getMessage() != null ? e.getMessage() : "";
            String lowerErrorMsg = errorMsg.toLowerCase();
            if (errorMsg.contains("401") || 
                errorMsg.contains("Unauthorized") ||
                errorMsg.contains("404") ||
                errorMsg.contains("Not Found") ||
                lowerErrorMsg.contains("unknown user") ||
                lowerErrorMsg.contains("user not found") ||
                lowerErrorMsg.contains("user does not exist") ||
                lowerErrorMsg.contains("unauth")) {
                // Wallet doesn't exist, try to create it
                log.info("Login failed (wallet may not exist) for student: {}, will create new wallet... Error: {}",
                        maskEmail(email), errorMsg);
                try {
                    return createWalletForStudent(email, fullName, password, studentId);
                } finally {
                    processingEmails.remove(email);
                }
            } else {
                // Unexpected error - rethrow
                log.error("Unexpected error during login for student: {}: {}", maskEmail(email), errorMsg, e);
                processingEmails.remove(email);
                throw new ExternalServiceException(
                    "Unexpected error during wallet login: " + errorMsg, "StudentWalletService");
            }
        } finally {
            // Ensure we always remove from processing set
            processingEmails.remove(email);
        }
    }
    
    /**
     * Creates a new wallet for a student
     */
    private StudentWalletInfo createWalletForStudent(
            String email, String fullName, String password, String studentId) 
            throws ExternalServiceException {
        
        log.info("🔄 Creating new wallet for student: {} ({})", maskEmail(email), studentId);
        
        // Parse name into first and last
        String[] nameParts = parseFullName(fullName);
        String firstName = nameParts[0];
        String lastName = nameParts[1];
        
        // Register wallet
        RegisterRequest registerRequest = RegisterRequest.builder()
                .email(email)
                .password(password)
                .firstName(firstName)
                .lastName(lastName)
                .type("email")
                .name(fullName)
                .build();
        
        try {
            waltidService.walletRegister(registerRequest);
            log.info("✅ Wallet registered successfully for student: {}", maskEmail(email));
            
        } catch (ExternalServiceException e) {
            // Handle 409 Conflict - wallet already exists (race condition or concurrent request)
            if (e.getMessage() != null && (e.getMessage().contains("409") || 
                e.getMessage().contains("Conflict") || 
                e.getMessage().contains("already exists"))) {
                log.warn("⚠️  Wallet registration returned 409 Conflict - wallet already exists (likely created by concurrent request). Attempting login...");
                // Don't throw - proceed to login instead
                // This is expected in case of concurrent requests
            } else {
                // Other registration errors - rethrow
                log.error("Failed to register wallet for student: {}: {}", maskEmail(email), e.getMessage(), e);
                throw new ExternalServiceException(
                    "Failed to register wallet for student: " + e.getMessage(), "StudentWalletService");
            }
        } catch (Exception e) {
            // Check if it's a 409 Conflict in the exception message
            if (e.getMessage() != null && (e.getMessage().contains("409") || 
                e.getMessage().contains("Conflict") || 
                e.getMessage().contains("already exists"))) {
                log.warn("⚠️  Wallet registration failed with 409 Conflict - wallet already exists. Attempting login...");
                // Proceed to login
            } else {
                // Other unexpected errors - rethrow
                log.error("Unexpected error during wallet registration for student: {}: {}", maskEmail(email), e.getMessage(), e);
                throw new ExternalServiceException(
                    "Failed to create wallet for student: " + e.getMessage(), "StudentWalletService");
            }
        }
        
        // Login to get session cookie (whether wallet was just created or already existed)
        try {
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(email)
                    .password(password)
                    .type("email")
                    .build();
            
            var loginResponse = waltidService.walletLogin(loginRequest);
            log.info("✅ Successfully logged into wallet for student: {}", maskEmail(email));
            
            // Get wallet info
            return getStudentWalletInfo(email, loginResponse.getSessionCookie());
            
        } catch (Exception e) {
            log.error("Failed to login to wallet for student: {}", maskEmail(email), e);
            throw new ExternalServiceException(
                "Failed to login to wallet for student: " + e.getMessage(), "StudentWalletService");
        }
    }
    
    /**
     * Gets wallet information including DID for a student
     */
    private StudentWalletInfo getStudentWalletInfo(String email, String sessionCookie) 
            throws ExternalServiceException {
        
        try {
            // Get wallet accounts (pass email as userId for Kafka event tracking)
            WalletAccountResponse accountResponse = waltidService.getWalletAccounts(sessionCookie, email);
            
            if (accountResponse == null || accountResponse.getAccounts() == null || 
                accountResponse.getAccounts().isEmpty()) {
                throw new ExternalServiceException(
                    "No wallet accounts found for student: " + email, "StudentWalletService");
            }
            
            List<WalletAccountResponse.WalletAccount> wallets = accountResponse.getAccounts();
            log.info("Found {} wallet(s) for student: {}", wallets.size(), maskEmail(email));
            
            // Use the most recently created wallet (likely to have credentials)
            // Sort by createdOn descending, or use first wallet if createdOn is not available
            String walletId = wallets.stream()
                .sorted((w1, w2) -> {
                    String c1 = w1.getCreatedOn();
                    String c2 = w2.getCreatedOn();
                    if (c1 != null && c2 != null) {
                        return c2.compareTo(c1); // Descending order (newest first)
                    }
                    return 0; // Keep original order if createdOn not available
                })
                .findFirst()
                .map(WalletAccountResponse.WalletAccount::getWalletId)
                .orElse(wallets.get(0).getWalletId());
            
            log.info("Using wallet {} for student: {} (selected from {} wallet(s))",
                    walletId, maskEmail(email), wallets.size());
            
            // Get DIDs from wallet (pass email as userId for Kafka event tracking)
            List<WalletDidResponse> didResponses = waltidService.getWalletDids(walletId, sessionCookie, email);
            
            if (didResponses == null || didResponses.isEmpty()) {
                log.warn("No DIDs found in wallet for student: {}. Wallet may need initialization.", maskEmail(email));
                // Return info with null DID - caller can handle this
                return StudentWalletInfo.builder()
                        .email(email)
                        .walletId(walletId)
                        .sessionCookie(sessionCookie)
                        .subjectDid(null)  // No DID yet
                        .build();
            }
            
            // Extract DID strings from responses and use first DID as subject DID
            List<String> dids = didResponses.stream()
                    .map(WalletDidResponse::getDid)
                    .filter(did -> did != null && !did.isEmpty())
                    .collect(Collectors.toList());
            
            if (dids.isEmpty()) {
                log.warn("No valid DIDs found in wallet responses for student: {}", maskEmail(email));
                return StudentWalletInfo.builder()
                        .email(email)
                        .walletId(walletId)
                        .sessionCookie(sessionCookie)
                        .subjectDid(null)
                        .build();
            }
            
            String subjectDid = dids.get(0);
            log.info("✅ Retrieved DID from wallet for student: {} - DID: {}", maskEmail(email), subjectDid);
            
            return StudentWalletInfo.builder()
                    .email(email)
                    .walletId(walletId)
                    .sessionCookie(sessionCookie)
                    .subjectDid(subjectDid)
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to get wallet info for student: {}", maskEmail(email), e);
            throw new ExternalServiceException(
                "Failed to get wallet information: " + e.getMessage(), "StudentWalletService");
        }
    }
    
    /**
     * Generates a deterministic password for student wallet.
     * Uses studentId + email + secret to create a consistent password.
     * 
     * In production, this should be stored securely (KMS) or use OAuth.
     */
    private String generateWalletPassword(String studentId, String email) {
        try {
            // Combine student data with secret
            String input = (studentId != null ? studentId : email) + ":" + email + ":" + walletPasswordSecret + ":" + walletPasswordSalt;
            
            // Hash using SHA-256
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string and take first 16 characters
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            // Take first 16 characters and add special chars to meet password requirements
            String password = hexString.substring(0, 12).toUpperCase() + "!@#";

            // Do not log the generated password (or any prefix of it) - it is a secret.
            log.debug("Generated wallet password for student (studentId: {})", studentId);

            return password;
            
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate wallet password", e);
            // Fallback: use simple hash
            return (studentId != null ? studentId : email.substring(0, 8)) + "!@#Wallet2024";
        }
    }
    
    /**
     * Extracts email from student data
     */
    private String extractEmail(Map<String, Object> studentData) {
        return pt.ulusofona.ulht.credential.mapper.StudentDataMapper.extractString(
            studentData, "email", "mail", "emailAddress");
    }
    
    /**
     * Extracts full name from student data
     */
    private String extractFullName(Map<String, Object> studentData) {
        String name = pt.ulusofona.ulht.credential.mapper.StudentDataMapper.extractString(
            studentData, "fullName", "name", "displayName");
        return name != null ? name : "Student User";
    }
    
    /**
     * Extracts student ID from student data and normalizes it
     * Ensures all student IDs have 'a' prefix (adds if missing, keeps if present)
     */
    private String extractStudentId(Map<String, Object> studentData) {
        String studentId = pt.ulusofona.ulht.credential.mapper.StudentDataMapper.extractString(
            studentData, "studentId", "studentCode", "id");
        
        // Normalize: Add 'a' prefix if missing
        String normalized = pt.ulusofona.ulht.credential.mapper.StudentDataMapper.normalizeStudentId(studentId);
        if (normalized != null && !normalized.equals(studentId)) {
            log.debug("Normalized studentId: {} -> {} (added 'a' prefix)", studentId, normalized);
        }
        
        return normalized;
    }
    
    /**
     * Parses full name into first and last name
     */
    private String[] parseFullName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return new String[]{"Student", "User"};
        }
        
        String[] parts = fullName.trim().split("\\s+", 2);
        if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return new String[]{parts[0], parts[1]};
    }
    
    /**
     * Gets credentials from student's wallet
     * 
     * @param studentData Student data map containing email, name, etc.
     * @return List of credentials stored in the wallet
     */
    public List<Map<String, Object>> getWalletCredentials(Map<String, Object> studentData) 
            throws ExternalServiceException {
        String studentId = extractStudentId(studentData);
        
        // IMPORTANT: Always use studentId-based email for wallet operations
        // This matches the email used during wallet registration
        String email;
        if (studentId != null && !studentId.isEmpty()) {
            email = studentId + "@students.example.edu";
            log.info("Using student email for wallet access: {} (studentId: {})", maskEmail(email), studentId);
        } else {
            // Fallback to extracted email if studentId is not available
            email = extractEmail(studentData);
            if (email == null || email.isEmpty()) {
                throw new IllegalArgumentException("Either studentId or email is required");
            }
            log.warn("Using extracted email (no studentId): {} - wallet may not be accessible", maskEmail(email));
        }
        
        log.info("Getting wallet credentials for student: {} ({})", maskEmail(email), studentId);
        
        // Ensure wallet exists and get wallet info
        StudentWalletInfo walletInfo = ensureWalletExists(studentData);
        String sessionCookie = walletInfo.getSessionCookie();
        
        // Try to get credentials from the selected wallet
        try {
            List<Map<String, Object>> credentials = waltidService.getWalletCredentials(
                walletInfo.getWalletId(), 
                sessionCookie, 
                email
            );
            
            // If we got credentials, return them
            if (credentials != null && !credentials.isEmpty()) {
                log.info("✅ Found {} credential(s) in wallet {} for student: {}",
                        credentials.size(), walletInfo.getWalletId(), maskEmail(email));
                return credentials;
            }
            
            log.debug("No credentials found in wallet {}, checking other wallets...", walletInfo.getWalletId());
        } catch (Exception e) {
            log.warn("Failed to get credentials from wallet {}: {}", walletInfo.getWalletId(), e.getMessage());
        }
        
        // If no credentials found in primary wallet, try other wallets
        // But only if we have multiple wallets
        try {
            WalletAccountResponse accountResponse = waltidService.getWalletAccounts(sessionCookie, email);
            if (accountResponse != null && accountResponse.getAccounts() != null && 
                accountResponse.getAccounts().size() > 1) {
                
                log.info("Trying other wallets for student: {} ({} total wallets)", maskEmail(email), accountResponse.getAccounts().size());
                
                // Try other wallets (skip the one we already tried)
                for (WalletAccountResponse.WalletAccount wallet : accountResponse.getAccounts()) {
                    String walletId = wallet.getWalletId();
                    if (walletId.equals(walletInfo.getWalletId())) {
                        continue; // Skip the one we already tried
                    }
                    
                    try {
                        List<Map<String, Object>> credentials = waltidService.getWalletCredentials(
                            walletId, sessionCookie, email);
                        
                        if (credentials != null && !credentials.isEmpty()) {
                            log.info("✅ Found {} credential(s) in wallet {} for student: {}",
                                    credentials.size(), walletId, maskEmail(email));
                            return credentials;
                        }
                    } catch (Exception e) {
                        log.debug("Failed to get credentials from wallet {}: {}", walletId, e.getMessage());
                        // Continue to next wallet
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to check other wallets: {}", e.getMessage());
        }
        
        // Return empty list if no credentials found in any wallet
        log.info("No credentials found in any wallet for student: {}", maskEmail(email));
        return new ArrayList<>();
    }
    
    /**
     * Handles a presentation request (verification) from a scanned QR code
     * 
     * @param presentationRequestUrl The openid4vp:// URL from the verifier
     * @param studentData Student data containing email/studentId for wallet access
     * @return Result of the presentation request
     */
    public Map<String, Object> handlePresentationRequest(String presentationRequestUrl, Map<String, Object> studentData) throws ExternalServiceException {
        String studentId = extractStudentId(studentData);
        String email = studentId != null && !studentId.isEmpty() 
            ? studentId + "@students.example.edu" 
            : extractEmail(studentData);
        
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Either studentId or email is required");
        }
        
        log.info("Handling presentation request for student: {} ({})", maskEmail(email), studentId);
        
        // Ensure wallet exists and get wallet info
        StudentWalletInfo walletInfo = ensureWalletExists(studentData);
        String sessionCookie = walletInfo.getSessionCookie();
        String walletId = walletInfo.getWalletId();
        
        // Step 1: Resolve the presentation request - get the presentation definition and query string
        Map<String, Object> resolveResult = waltidService.resolvePresentationRequest(
            walletId, presentationRequestUrl, sessionCookie);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> presentationDefinition = (Map<String, Object>) resolveResult.get("presentationDefinition");
        String queryString = (String) resolveResult.get("queryString");
        
        if (presentationDefinition == null) {
            throw new ExternalServiceException("Failed to extract presentation definition from response", "StudentWalletService");
        }
        
        // Step 2: Match credentials against the presentation definition
        List<Map<String, Object>> matchedCredentials = waltidService.matchCredentialsForPresentationDefinition(
            walletId, presentationDefinition, sessionCookie);
        
        if (matchedCredentials == null || matchedCredentials.isEmpty()) {
            log.warn("No matching credentials found for presentation request (wallet: {})", walletId);
            return Map.of(
                "success", false,
                "message", "No matching credentials found",
                "walletId", walletId,
                "matchedCredentials", new ArrayList<>()
            );
        }
        
        log.info("✅ Found {} matching credential(s) for presentation request (wallet: {})", 
                matchedCredentials.size(), walletId);
        
        // Step 3: Build the full presentation request URL with presentation_definition included
        // Use the query string from the resolve response (it already has presentation_definition)
        String fullPresentationRequestUrl = buildFullPresentationRequestUrl(
            presentationRequestUrl, queryString);
        
        // Step 4: Extract credential IDs from matched credentials
        List<String> credentialIds = matchedCredentials.stream()
            .map(cred -> {
                Object id = cred.get("id");
                return id != null ? id.toString() : null;
            })
            .filter(id -> id != null)
            .collect(java.util.stream.Collectors.toList());
        
        if (credentialIds.isEmpty()) {
            log.warn("No credential IDs found in matched credentials");
            return Map.of(
                "success", false,
                "message", "No credential IDs found in matched credentials",
                "walletId", walletId,
                "matchedCredentials", matchedCredentials
            );
        }
        
        // Step 5: Use the presentation request to actually present the credentials
        Map<String, Object> presentationResult = waltidService.usePresentationRequest(
            walletId, fullPresentationRequestUrl, credentialIds, sessionCookie);
        
        log.info("✅ Successfully presented credentials for verification (wallet: {}, credentials: {})", 
                walletId, credentialIds);
        
        return Map.of(
            "success", true,
            "walletId", walletId,
            "matchedCredentials", matchedCredentials,
            "presentedCredentials", credentialIds,
            "presentationResult", presentationResult
        );
    }
    
    /**
     * Matches credentials for a presentation request without presenting them
     * Returns matched credentials with disclosure information for selective disclosure UI
     * 
     * @param presentationRequestUrl The openid4vp:// URL from the verifier
     * @param studentData Student data containing email/studentId for wallet access
     * @return Map containing walletId, matchedCredentials, and presentationDefinition
     */
    public Map<String, Object> matchCredentialsForPresentation(String presentationRequestUrl, Map<String, Object> studentData) throws ExternalServiceException {
        String studentId = extractStudentId(studentData);
        String email = studentId != null && !studentId.isEmpty() 
            ? studentId + "@students.example.edu" 
            : extractEmail(studentData);
        
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Either studentId or email is required");
        }
        
        log.info("Matching credentials for presentation request for student: {} ({})", maskEmail(email), studentId);
        
        // Ensure wallet exists and get wallet info
        StudentWalletInfo walletInfo = ensureWalletExists(studentData);
        String sessionCookie = walletInfo.getSessionCookie();
        String walletId = walletInfo.getWalletId();
        
        // Step 1: Resolve the presentation request - get the presentation definition and query string
        Map<String, Object> resolveResult = waltidService.resolvePresentationRequest(
            walletId, presentationRequestUrl, sessionCookie);
        
        @SuppressWarnings("unchecked")
        Map<String, Object> presentationDefinition = (Map<String, Object>) resolveResult.get("presentationDefinition");
        String queryString = (String) resolveResult.get("queryString");
        
        if (presentationDefinition == null) {
            throw new ExternalServiceException("Failed to extract presentation definition from response", "StudentWalletService");
        }
        
        // Step 2: Match credentials against the presentation definition
        List<Map<String, Object>> matchedCredentials = waltidService.matchCredentialsForPresentationDefinition(
            walletId, presentationDefinition, sessionCookie);
        
        if (matchedCredentials == null || matchedCredentials.isEmpty()) {
            log.warn("No matching credentials found for presentation request (wallet: {})", walletId);
            return Map.of(
                "success", false,
                "message", "No matching credentials found",
                "walletId", walletId,
                "matchedCredentials", new ArrayList<>(),
                "presentationDefinition", presentationDefinition
            );
        }
        
        log.info("✅ Found {} matching credential(s) for presentation request (wallet: {})", 
                matchedCredentials.size(), walletId);
        
        // Build the full presentation request URL for later use
        String fullPresentationRequestUrl = buildFullPresentationRequestUrl(
            presentationRequestUrl, queryString);
        
        return Map.of(
            "success", true,
            "walletId", walletId,
            "matchedCredentials", matchedCredentials,
            "presentationDefinition", presentationDefinition,
            "fullPresentationRequestUrl", fullPresentationRequestUrl
        );
    }
    
    /**
     * Builds the full presentation request URL using the query string from resolvePresentationRequest
     */
    private String buildFullPresentationRequestUrl(String originalUrl, String queryString) {
        try {
            // Check if queryString is already a full URL (starts with a scheme like openid4vp://)
            if (queryString != null && (queryString.startsWith("openid4vp://") || queryString.startsWith("openid4vci://") || 
                queryString.startsWith("http://") || queryString.startsWith("https://"))) {
                // queryString is already a complete URL, use it directly
                log.debug("Query string is already a full URL, using it directly: {}", queryString);
                return queryString;
            }
            
            // Parse the original URL to get scheme, authority, and path
            java.net.URI uri = new java.net.URI(originalUrl);
            
            // The queryString from resolvePresentationRequest has all parameters including presentation_definition
            // Just combine the original URL base with the new query string
            String fullUrl = uri.getScheme() + "://" + uri.getAuthority() + uri.getPath() + "?" + queryString;
            log.debug("Built full presentation request URL: {}", fullUrl);
            return fullUrl;
        } catch (Exception e) {
            log.error("Failed to build full presentation request URL: {}", e.getMessage(), e);
            // Fallback: if queryString looks like a full URL, use it; otherwise return original URL
            if (queryString != null && (queryString.startsWith("openid4vp://") || queryString.startsWith("openid4vci://"))) {
                return queryString;
            }
            return originalUrl;
        }
    }

    /**
     * Student wallet information
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class StudentWalletInfo {
        private String email;
        private String walletId;
        private String sessionCookie;
        private String subjectDid;  // Student's DID from their wallet
    }
}
