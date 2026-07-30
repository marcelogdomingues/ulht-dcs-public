package pt.ulusofona.ulht.credential.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import pt.ulusofona.ulht.credential.client.WaltidIssuerClient;
import pt.ulusofona.ulht.credential.client.WaltidWalletClient;
import pt.ulusofona.ulht.credential.domain.authentication.LoginRequest;
import pt.ulusofona.ulht.credential.domain.authentication.LoginResponse;
import pt.ulusofona.ulht.credential.domain.authentication.RegisterRequest;
import pt.ulusofona.ulht.credential.domain.authentication.RegisterResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletAccountResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletKeysResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletDidResponse;
import pt.ulusofona.ulht.credential.exception.ExternalServiceException;
import pt.ulusofona.ulht.credential.kafka.WalletEventProducer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Service for managing WaltID wallet operations including login, registration, and credential issuance.
 * Independent service for the credential microservice.
 */
@Service
@RequiredArgsConstructor
public class WaltidService {

    private static final Logger logger = LoggerFactory.getLogger(WaltidService.class);

    private final WaltidIssuerClient waltidIssuerClient;
    private final WaltidWalletClient waltidWalletClient;
    private final WalletEventProducer walletEventProducer;
    private final ObjectMapper objectMapper;

    /**
     * Executes an operation and handles errors appropriately.
     */
    private <T> T executeOperation(final Supplier<T> operation, final String serviceName)
            throws ExternalServiceException {
        try {
            return operation.get();
        } catch (Exception e) {
            logger.error("Error executing operation on {}: {}", serviceName, e.getMessage(), e);
            
            // Check if the exception or its cause is already an ExternalServiceException
            Throwable cause = e;
            while (cause != null) {
                if (cause instanceof ExternalServiceException) {
                    // If it's already an ExternalServiceException, preserve it (don't wrap)
                    throw (ExternalServiceException) cause;
                }
                cause = cause.getCause();
            }
            
            // Preserve the original exception message, or use a default if null
            String errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.trim().isEmpty()) {
                errorMsg = e.getClass().getSimpleName();
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    errorMsg += ": " + e.getCause().getMessage();
                }
            }
            throw new ExternalServiceException(
                String.format("Error calling %s service: %s", serviceName, errorMsg),
                serviceName
            );
        }
    }

    /**
     * Performs wallet login via WaltID Wallet API.
     * 
     * This is a wrapper around the external WaltID Wallet service (/wallet-api/auth/login).
     * Uses cookie-based authentication by default (preferred by WaltID Wallet API).
     * 
     * @param loginRequest Login credentials (email/password)
     * @return LoginResponse with session cookie for subsequent authenticated requests
     */
    public LoginResponse walletLogin(LoginRequest loginRequest) throws ExternalServiceException {
        logger.info("Attempting wallet login for user: {}", loginRequest.getEmail());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", loginRequest.getType());
        requestBody.put("email", loginRequest.getEmail());
        requestBody.put("password", loginRequest.getPassword());

        try {
            ResponseEntity<String> response = executeOperation(
                () -> waltidWalletClient.loginWallet(requestBody),
                "WaltID Wallet"
            );

            String sessionCookie = extractSessionCookie(response);
            LoginResponse loginResponse = LoginResponse.builder()
                    .sessionCookie(sessionCookie)
                    .userId(loginRequest.getEmail())
                    .loginTime(Instant.now())
                    .status("SUCCESS")
                    .build();

            // Publish wallet login event to Kafka
            walletEventProducer.publishWalletLogin(loginRequest.getEmail(), loginRequest, sessionCookie, true);
            
            return loginResponse;
        } catch (Exception e) {
            // Publish failed login event
            walletEventProducer.publishWalletLogin(loginRequest.getEmail(), loginRequest, null, false);
            throw e;
        }
    }

    /**
     * Performs wallet registration via WaltID Wallet API.
     * 
     * This is a wrapper around the external WaltID Wallet service (/wallet-api/auth/register).
     * Creates a new user account in WaltID Wallet with email/password authentication.
     * 
     * @param registerRequest Registration details (email, password, firstName, lastName)
     * @return RegisterResponse confirming successful registration
     */
    public RegisterResponse walletRegister(RegisterRequest registerRequest) throws ExternalServiceException {
        logger.info("Attempting wallet registration for user: {}", registerRequest.getEmail());
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("type", registerRequest.getType()); // Required by WaltID Wallet API
        requestBody.put("email", registerRequest.getEmail());
        requestBody.put("password", registerRequest.getPassword());
        requestBody.put("firstName", registerRequest.getFirstName());
        requestBody.put("lastName", registerRequest.getLastName());

        try {
            ResponseEntity<String> response = executeOperation(
                () -> waltidWalletClient.registerWallet(requestBody),
                "WaltID Wallet"
            );

            RegisterResponse registerResponse = RegisterResponse.builder()
                    .userId(registerRequest.getEmail())
                    .status("SUCCESS")
                    .registrationTime(Instant.now())
                    .build();

            // Publish wallet registration event to Kafka
            walletEventProducer.publishWalletRegister(registerRequest.getEmail(), registerRequest, true);
            
            return registerResponse;
        } catch (Exception e) {
            // Publish failed registration event
            walletEventProducer.publishWalletRegister(registerRequest.getEmail(), registerRequest, false);
            throw e;
        }
    }

    /**
     * Gets wallet accounts via WaltID Wallet API.
     * 
     * This is a wrapper around the external WaltID Wallet service (/wallet-api/wallet/accounts/wallets).
     * Requires authenticated session (cookie-based authentication).
     * 
     * @param sessionCookie Session cookie from previous login (cookie-based) or bearer token
     * @return WalletAccountResponse containing user's wallet accounts
     */
    public WalletAccountResponse getWalletAccounts(String sessionCookie) throws ExternalServiceException {
        return getWalletAccounts(sessionCookie, null);
    }
    
    /**
     * Gets wallet accounts via WaltID Wallet API.
     * 
     * This is a wrapper around the external WaltID Wallet service (/wallet-api/wallet/accounts/wallets).
     * Requires authenticated session (cookie-based authentication).
     * 
     * @param sessionCookie Session cookie from previous login (cookie-based) or bearer token
     * @param userId User identifier (email) for Kafka event tracking. If null, uses "unknown"
     * @return WalletAccountResponse containing user's wallet accounts
     */
    public WalletAccountResponse getWalletAccounts(String sessionCookie, String userId) throws ExternalServiceException {
        logger.info("Retrieving wallet accounts for user: {}", userId != null ? userId : "unknown");

        String effectiveUserId = userId != null ? userId : "unknown";
        
        try {
            ResponseEntity<WalletAccountResponse> response = executeOperation(
                () -> waltidWalletClient.getWalletAccounts(sessionCookie),
                "WaltID Wallet"
            );

            WalletAccountResponse accountResponse = response.getBody();
            
            // Publish wallet account access event to Kafka
            // Extract walletId from accounts if available, otherwise use "unknown"
            String walletId = "unknown";
            if (accountResponse != null && accountResponse.getAccounts() != null && !accountResponse.getAccounts().isEmpty()) {
                // Try to get walletId from first account if available
                var firstAccount = accountResponse.getAccounts().get(0);
                if (firstAccount != null && firstAccount.getWalletId() != null) {
                    walletId = firstAccount.getWalletId();
                }
            }
            walletEventProducer.publishWalletAccountAccess(effectiveUserId, walletId, "GET_ACCOUNTS", true);
            
            return accountResponse;
        } catch (Exception e) {
            // Publish failed access event
            walletEventProducer.publishWalletAccountAccess(effectiveUserId, null, "GET_ACCOUNTS", false);
            throw e;
        }
    }

    /**
     * Gets wallet keys
     */
    public WalletKeysResponse getWalletKeys(String walletId, String sessionCookie) throws ExternalServiceException {
        return getWalletKeys(walletId, sessionCookie, null);
    }
    
    /**
     * Gets wallet keys
     * 
     * @param walletId Wallet identifier
     * @param sessionCookie Session cookie from previous login
     * @param userId User identifier (email) for Kafka event tracking. If null, uses "unknown"
     */
    public WalletKeysResponse getWalletKeys(String walletId, String sessionCookie, String userId) throws ExternalServiceException {
        logger.info("Retrieving wallet keys for wallet: {} (user: {})", walletId, userId != null ? userId : "unknown");

        String effectiveUserId = userId != null ? userId : "unknown";

        try {
            ResponseEntity<List<WalletKeysResponse.WalletKey>> response = executeOperation(
                () -> waltidWalletClient.getWalletKeys(walletId, sessionCookie),
                "WaltID Wallet"
            );

            WalletKeysResponse keysResponse = WalletKeysResponse.builder()
                    .keys(response.getBody())
                    .build();

            // Publish wallet account access event to Kafka
            walletEventProducer.publishWalletAccountAccess(effectiveUserId, walletId, "GET_KEYS", true);
            
            return keysResponse;
        } catch (Exception e) {
            // Publish failed access event
            walletEventProducer.publishWalletAccountAccess(effectiveUserId, walletId, "GET_KEYS", false);
            throw e;
        }
    }

    /**
     * Gets wallet DIDs
     */
    public List<WalletDidResponse> getWalletDids(String walletId, String sessionCookie) throws ExternalServiceException {
        return getWalletDids(walletId, sessionCookie, null);
    }
    
    /**
     * Gets wallet DIDs
     * 
     * @param walletId Wallet identifier
     * @param sessionCookie Session cookie from previous login
     * @param userId User identifier (email) for Kafka event tracking. If null, uses "unknown"
     */
    public List<WalletDidResponse> getWalletDids(String walletId, String sessionCookie, String userId) throws ExternalServiceException {
        logger.info("Retrieving wallet DIDs for wallet: {} (user: {})", walletId, userId != null ? userId : "unknown");

        String effectiveUserId = userId != null ? userId : "unknown";

        try {
            ResponseEntity<List<WalletDidResponse>> response = executeOperation(
                () -> waltidWalletClient.getWalletDids(walletId, sessionCookie),
                "WaltID Wallet"
            );

            List<WalletDidResponse> dids = response.getBody();

            // Publish wallet account access event to Kafka
            walletEventProducer.publishWalletAccountAccess(effectiveUserId, walletId, "GET_DIDS", true);
            
            return dids;
        } catch (Exception e) {
            // Publish failed access event
            walletEventProducer.publishWalletAccountAccess(effectiveUserId, walletId, "GET_DIDS", false);
            throw e;
        }
    }

    /**
     * Gets credentials from wallet
     * 
     * @param walletId Wallet identifier
     * @param sessionCookie Session cookie for authentication
     * @param userId User identifier (email) for Kafka event tracking. If null, uses "unknown"
     * @return List of credentials stored in the wallet
     */
    public List<Map<String, Object>> getWalletCredentials(String walletId, String sessionCookie, String userId) 
            throws ExternalServiceException {
        logger.info("Retrieving wallet credentials for wallet: {} (user: {})", walletId, userId != null ? userId : "unknown");
        
        String effectiveUserId = userId != null ? userId : "unknown";
        
        try {
            ResponseEntity<List<Map<String, Object>>> response = executeOperation(
                () -> waltidWalletClient.getWalletCredentials(walletId, sessionCookie, false, false),
                "WaltID Wallet"
            );
            
            List<Map<String, Object>> credentials = response.getBody();
            logger.debug("Retrieved {} credential(s) from wallet: {} (user: {})", 
                    credentials != null ? credentials.size() : 0, walletId, effectiveUserId);
            
            return credentials != null ? credentials : new ArrayList<>();
        } catch (ExternalServiceException e) {
            // Check if it's a 404 (endpoint might not exist yet)
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                logger.debug("Wallet credentials endpoint not found (404) - endpoint may not be available. Will issue new credentials.");
            } else {
                logger.warn("Failed to retrieve wallet credentials for wallet: {} (user: {}): {}. Treating as empty list.", 
                        walletId, effectiveUserId, e.getMessage());
            }
            // Return empty list if query fails - we'll issue new credentials
            return new ArrayList<>();
        } catch (Exception e) {
            logger.warn("Unexpected error retrieving wallet credentials for wallet: {} (user: {}): {}. Treating as empty list.", 
                    walletId, effectiveUserId, e.getMessage());
            // Return empty list if query fails - we'll issue new credentials
            return new ArrayList<>();
        }
    }

    /**
     * Accepts a credential offer into the wallet
     * 
     * This method stores a W3C Verifiable Credential in the user's wallet by accepting
     * the credential offer URL from the issuer.
     * 
     * @param walletId Wallet identifier
     * @param subjectDid User's DID (Decentralized Identifier) - the subject of the credential
     * @param offerUrl Credential offer URL (OID4VCI format, e.g., "openid-credential-offer://...")
     * @param sessionCookie Session cookie for authentication
     * @param userId User identifier (email) for Kafka event tracking. If null, uses "unknown"
     * @return Response indicating success
     * @throws ExternalServiceException if the offer acceptance fails
     */
    public void acceptCredentialOffer(String walletId, String subjectDid, String offerUrl, 
                                      String sessionCookie, String userId) throws ExternalServiceException {
        logger.info("Accepting credential offer into wallet: {} for DID: {} (user: {})", 
                walletId, subjectDid, userId != null ? userId : "unknown");
        
        // Validate session cookie
        if (sessionCookie == null || sessionCookie.isEmpty()) {
            throw new ExternalServiceException(
                "Session cookie is required to accept credential offer. Please login first.", "WaltidService");
        }
        
        // Log cookie format for debugging (without exposing full value)
        logger.debug("Session cookie provided: {} (length: {})", 
                sessionCookie.length() > 20 ? sessionCookie.substring(0, 20) + "..." : sessionCookie,
                sessionCookie.length());

        String effectiveUserId = userId != null ? userId : "unknown";

        try {
            // Ensure cookie is in the correct format (should already be "login=...")
            String cookieHeader = sessionCookie.startsWith("login=") ? sessionCookie : "login=" + sessionCookie;
            logger.debug("Using cookie header: {} (length: {})", 
                    cookieHeader.length() > 20 ? cookieHeader.substring(0, 20) + "..." : cookieHeader,
                    cookieHeader.length());
            
            ResponseEntity<String> response = executeOperation(
                () -> waltidWalletClient.acceptCredentialOffer(walletId, subjectDid, offerUrl, cookieHeader),
                "WaltID Wallet"
            );

            logger.info("✅ Successfully accepted credential offer into wallet: {} (user: {})", 
                    walletId, effectiveUserId);
            
            // Publish wallet operation event to Kafka
            Map<String, Object> details = new HashMap<>();
            details.put("walletId", walletId);
            details.put("offerUrl", offerUrl);
            details.put("subjectDid", subjectDid);
            walletEventProducer.publishWalletOperation(effectiveUserId, "ACCEPT_CREDENTIAL_OFFER", details, true);
            
        } catch (Exception e) {
            logger.error("Failed to accept credential offer into wallet: {} (user: {}): {}", 
                    walletId, effectiveUserId, e.getMessage(), e);
            
            // Publish failed operation event to Kafka
            Map<String, Object> details = new HashMap<>();
            details.put("walletId", walletId);
            details.put("offerUrl", offerUrl);
            details.put("subjectDid", subjectDid);
            walletEventProducer.publishWalletOperation(effectiveUserId, "ACCEPT_CREDENTIAL_OFFER", details, false);
            
            throw new ExternalServiceException(
                "Failed to accept credential offer into wallet: " + e.getMessage(), "WaltidService");
        }
    }

    /**
     * Resolves a presentation request from a verification URL (OID4VP)
     * 
     * @param walletId Wallet identifier
     * @param presentationRequestUrl The openid4vp:// URL from the verifier
     * @param sessionCookie Session cookie for authentication
     * @return Presentation definition
     */
    /**
     * Resolves a presentation request from a verification URL (OID4VP)
     * 
     * The walt.id wallet API returns just the query string with presentation_definition added.
     * We need to extract and parse that parameter.
     * 
     * @param walletId Wallet identifier
     * @param presentationRequestUrl The openid4vp:// URL from the verifier
     * @param sessionCookie Session cookie for authentication
     * @return Map containing both the presentation definition and the full query string
     */
    public Map<String, Object> resolvePresentationRequest(String walletId, String presentationRequestUrl, String sessionCookie) throws ExternalServiceException {
        logger.info("Resolving presentation request for wallet: {} with URL: {}", walletId, presentationRequestUrl);
        
        try {
            // Call the endpoint - it returns the URL with presentation_definition added as a query parameter
            ResponseEntity<String> response = executeOperation(
                () -> waltidWalletClient.resolvePresentationRequest(walletId, presentationRequestUrl, sessionCookie),
                "WaltID Wallet"
            );
            
            String responseQueryString = response.getBody();
            if (responseQueryString == null || responseQueryString.isEmpty()) {
                throw new ExternalServiceException("Empty response from wallet API", "WaltidService");
            }
            
            logger.debug("Received response query string from wallet API: {}", responseQueryString);
            
            // The response is just the query string (not the full URL)
            // Parse query parameters to extract the presentation_definition
            try {
                String[] params = responseQueryString.split("&");
                String presentationDefinitionParam = null;
                for (String param : params) {
                    if (param.startsWith("presentation_definition=")) {
                        presentationDefinitionParam = param.substring("presentation_definition=".length());
                        break;
                    }
                }
                
                if (presentationDefinitionParam == null) {
                    throw new ExternalServiceException("presentation_definition parameter not found in response", "WaltidService");
                }
                
                // URL decode the parameter
                String decodedDefinition = java.net.URLDecoder.decode(presentationDefinitionParam, java.nio.charset.StandardCharsets.UTF_8);
                
                // Parse the JSON
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> presentationDefinition = mapper.readValue(decodedDefinition, Map.class);
                
                logger.info("Successfully extracted presentation definition from response");
                
                // Return both the presentation definition and the full query string
                Map<String, Object> result = new java.util.HashMap<>();
                result.put("presentationDefinition", presentationDefinition);
                result.put("queryString", responseQueryString);
                return result;
                
            } catch (IllegalArgumentException e) {
                throw new ExternalServiceException("Failed to decode URL parameter: " + e.getMessage(), "WaltidService");
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new ExternalServiceException("Failed to parse presentation definition JSON: " + e.getMessage(), "WaltidService");
            }
            
        } catch (Exception e) {
            logger.error("Error resolving presentation request: {}", e.getMessage(), e);
            throw new ExternalServiceException(
                "Failed to resolve presentation request: " + e.getMessage(), "WaltidService");
        }
    }

    /**
     * Matches credentials in wallet against a presentation definition
     * 
     * @param walletId Wallet identifier
     * @param presentationDefinition The presentation definition (from resolvePresentationRequest)
     * @param sessionCookie Session cookie for authentication
     * @return List of matched credentials
     */
    public List<Map<String, Object>> matchCredentialsForPresentationDefinition(String walletId, Map<String, Object> presentationDefinition, String sessionCookie) throws ExternalServiceException {
        logger.info("Matching credentials for presentation definition in wallet: {}", walletId);
        
        try {
            ResponseEntity<List<Map<String, Object>>> response = executeOperation(
                () -> waltidWalletClient.matchCredentialsForPresentationDefinition(walletId, presentationDefinition, sessionCookie),
                "WaltID Wallet"
            );
            
            List<Map<String, Object>> credentials = response.getBody();
            if (credentials == null) {
                return new java.util.ArrayList<>();
            }
            return credentials;
        } catch (Exception e) {
            throw new ExternalServiceException(
                "Failed to match credentials for presentation definition: " + e.getMessage(), "WaltidService");
        }
    }

    /**
     * Uses a presentation request to present credentials
     * 
     * @param walletId Wallet identifier
     * @param presentationRequestUrl The full openid4vp:// URL with presentation_definition included
     * @param selectedCredentialIds Array of credential IDs to present
     * @param sessionCookie Session cookie for authentication
     * @return Response indicating success
     */
    public Map<String, Object> usePresentationRequest(String walletId, String presentationRequestUrl, List<String> selectedCredentialIds, String sessionCookie) throws ExternalServiceException {
        logger.info("Using presentation request for wallet: {} with {} credential(s)", walletId, selectedCredentialIds.size());
        
        try {
            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("presentationRequest", presentationRequestUrl);
            requestBody.put("selectedCredentials", selectedCredentialIds);
            requestBody.put("disclosures", null); // Can be set for selective disclosure
            
            ResponseEntity<Map<String, Object>> response = executeOperation(
                () -> waltidWalletClient.usePresentationRequest(walletId, requestBody, sessionCookie),
                "WaltID Wallet"
            );
            
            Map<String, Object> result = response.getBody();
            if (result == null) {
                result = new java.util.HashMap<>();
                result.put("success", true);
                result.put("message", "Presentation successful");
            }
            return result;
        } catch (Exception e) {
            throw new ExternalServiceException(
                "Failed to use presentation request: " + e.getMessage(), "WaltidService");
        }
    }

    /**
     * Onboards a user with the issuer
     */
    public String onboardUser(Map<String, Object> requestBody) throws ExternalServiceException {
        logger.info("Onboarding user with issuer");

        ResponseEntity<String> response = executeOperation(
            () -> waltidIssuerClient.onboardUser(requestBody),
            "WaltID Issuer"
        );

        return response.getBody();
    }

    /**
     * Issues a credential
     */
    public String issueCredential(Map<String, Object> requestBody) throws ExternalServiceException {
        logger.info("Issuing credential");

        ResponseEntity<String> response = executeOperation(
            () -> waltidIssuerClient.issueCredential(requestBody),
            "WaltID Issuer"
        );

        return response.getBody();
    }

    /**
     * Gets issuer status
     */
    public String getIssuerStatus(String userId) throws ExternalServiceException {
        logger.info("Getting issuer status for user: {}", userId);

        ResponseEntity<String> response = executeOperation(
            () -> waltidIssuerClient.getIssuerStatus(userId),
            "WaltID Issuer"
        );

        return response.getBody();
    }

    /**
     * Extracts session cookie from WaltID Wallet API response.
     * 
     * WaltID Wallet API supports two authentication methods:
     * 1. Cookie-based (preferred): Returns session cookie in Set-Cookie header
     * 2. Bearer token (alternative): Returns token in JSON response body
     * 
     * This method handles both methods, prioritizing cookie-based authentication.
     * 
     * @param response HTTP response from WaltID Wallet API
     * @return Session cookie string in format "login=<value>" (for cookie-based) or token (for bearer token)
     */
    private String extractSessionCookie(ResponseEntity<String> response) {
        try {
            // Priority 1: Cookie-based authentication (preferred by WaltID Wallet API)
            // Cookie is returned in Set-Cookie header: "login=<session-token>; Path=/; Max-Age=..."
            // We need to extract just "login=<value>" from the header
            // Spring's HttpHeaders.get() is case-insensitive, so this should work
            List<String> setCookieHeaders = response.getHeaders().get("Set-Cookie");
            if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
                // Try lowercase variant (some HTTP libraries store headers in lowercase)
                setCookieHeaders = response.getHeaders().get("set-cookie");
            }
            if (setCookieHeaders != null && !setCookieHeaders.isEmpty()) {
                String setCookie = setCookieHeaders.get(0);
                logger.debug("Found Set-Cookie header (length: {}): {}...", 
                        setCookie.length(), 
                        setCookie.length() > 100 ? setCookie.substring(0, 100) : setCookie);
                if (setCookie != null && !setCookie.isEmpty()) {
                    // Extract just the "login=<value>" part from the Set-Cookie header
                    // Format: "login=value; Max-Age=...; Path=/; ..."
                    if (setCookie.startsWith("login=")) {
                        int endIndex = setCookie.indexOf(';');
                        String cookieValue = (endIndex > 0) ? setCookie.substring(0, endIndex) : setCookie;
                        logger.info("✅ Extracted session cookie from Set-Cookie header: {}...", 
                                cookieValue.length() > 50 ? cookieValue.substring(0, 50) + "..." : cookieValue);
                        return cookieValue;
                    }
                    // If it doesn't start with "login=", return the full header as fallback
                    logger.debug("Set-Cookie header doesn't start with 'login=', using full value");
                    return setCookie;
                }
            }
            
            // Debug: log available headers to help troubleshoot
            logger.warn("⚠️  Set-Cookie header not found. Available response headers: {}", response.getHeaders().headerNames());
            
            // Priority 2: Bearer token authentication (fallback)
            // Token is returned in JSON response: {"token": "..."}
            // NOTE: Bearer tokens won't work for cookie-based authentication!
            if (response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                if (jsonNode.has("sessionCookie")) {
                    String cookie = jsonNode.get("sessionCookie").asText();
                    logger.debug("Extracted session cookie from JSON response (sessionCookie field)");
                    return cookie;
                }
                if (jsonNode.has("token")) {
                    String token = jsonNode.get("token").asText();
                    logger.warn("⚠️  Using bearer token as fallback - cookie-based auth may fail. Token: {}...", 
                            token.length() > 20 ? token.substring(0, 20) : token);
                    return "login=" + token; // Format as cookie for consistency
                }
            }
            
            logger.warn("No session cookie or token found in response");
            return response.getBody();
        } catch (Exception e) {
            logger.error("Could not extract session cookie from response: {}", e.getMessage(), e);
            return response.getBody();
        }
    }

    /**
     * Onboard an issuer with walt.id - creates signing key and DID
     * 
     * @param keyType Algorithm for key generation (ed25519, secp256k1, secp256r1, RSA)
     * @param didMethod DID method to use (key, jwk, web, cheqd)
     * @return OnboardIssuerResponse containing the generated key and DID
     */
    public pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse onboardIssuer(
            String keyType, String didMethod) throws ExternalServiceException {
        
        logger.info("Onboarding issuer with keyType: {} and didMethod: {}", keyType, didMethod);
        
        pt.ulusofona.ulht.credential.dto.waltid.KeyConfig keyConfig = 
            pt.ulusofona.ulht.credential.dto.waltid.KeyConfig.builder()
                .backend("jwk")
                .keyType(keyType)
                .build();
        
        pt.ulusofona.ulht.credential.dto.waltid.DidConfig didConfig = 
            pt.ulusofona.ulht.credential.dto.waltid.DidConfig.builder()
                .method(didMethod)
                .build();
        
        pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerRequest request = 
            pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerRequest.builder()
                .key(keyConfig)
                .did(didConfig)
                .build();
        
        ResponseEntity<pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse> response = 
            executeOperation(
                () -> waltidIssuerClient.onboardIssuer(request),
                "WaltID Issuer Onboarding"
            );
        
        logger.info("Successfully onboarded issuer with DID: {}", 
            response.getBody() != null ? response.getBody().getIssuerDid() : "unknown");
        
        return response.getBody();
    }

    /**
     * Issues a JWT W3C Verifiable Credential using OID4VCI protocol
     * 
     * @param request The credential issuance request
     * @return OID4VCI credential offer URL
     */
    public String issueJwtCredential(
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request) 
            throws ExternalServiceException {
        
        return issueJwtCredential(request, null);
    }

    /**
     * Issues a JWT W3C Verifiable Credential with status callback
     * 
     * @param request The credential issuance request
     * @param statusCallbackUri Optional callback URI for issuance status updates
     * @return OID4VCI credential offer URL
     */
    public String issueJwtCredential(
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request,
            String statusCallbackUri) throws ExternalServiceException {
        
        logger.info("Issuing JWT credential with configuration: {}", 
            request.getCredentialConfigurationId());
        
        ResponseEntity<String> response = executeOperation(
            () -> waltidIssuerClient.issueJwtCredential(request, statusCallbackUri),
            "WaltID JWT Credential Issuance"
        );
        
        String offerUrl = response.getBody();
        logger.info("Successfully issued JWT credential, offer URL generated");
        
        return offerUrl;
    }

    /**
     * Issues an SD-JWT (Selective Disclosure) W3C Verifiable Credential using OID4VCI protocol
     * 
     * @param request The credential issuance request
     * @return OID4VCI credential offer URL
     */
    public String issueSdJwtCredential(
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request) 
            throws ExternalServiceException {
        
        return issueSdJwtCredential(request, null);
    }

    /**
     * Issues an SD-JWT W3C Verifiable Credential with status callback
     * 
     * @param request The credential issuance request
     * @param statusCallbackUri Optional callback URI for issuance status updates
     * @return OID4VCI credential offer URL
     */
    public String issueSdJwtCredential(
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request,
            String statusCallbackUri) throws ExternalServiceException {
        
        logger.info("Issuing SD-JWT credential with configuration: {}", 
            request.getCredentialConfigurationId());
        
        ResponseEntity<String> response = executeOperation(
            () -> waltidIssuerClient.issueSdJwtCredential(request, statusCallbackUri),
            "WaltID SD-JWT Credential Issuance"
        );
        
        String offerUrl = response.getBody();
        logger.info("Successfully issued SD-JWT credential, offer URL generated");
        
        return offerUrl;
    }

    /**
     * Issues a university degree credential (simplified interface)
     * 
     * @param degreeRequest The university degree credential request
     * @return OID4VCI credential offer URL
     */
    public String issueUniversityDegreeCredential(
            pt.ulusofona.ulht.credential.dto.UniversityDegreeCredentialRequest degreeRequest) 
            throws ExternalServiceException {
        
        logger.info("Issuing university degree credential for subject: {}", 
            degreeRequest.getSubjectDid());
        
        // Build credential data
        pt.ulusofona.ulht.credential.dto.waltid.UniversityDegreeData degreeData = 
            pt.ulusofona.ulht.credential.dto.waltid.UniversityDegreeData.builder()
                .type(degreeRequest.getDegreeType() != null ? 
                    degreeRequest.getDegreeType() : "BachelorDegree")
                .name(degreeRequest.getDegreeName() != null ? 
                    degreeRequest.getDegreeName() : "Bachelor of Science")
                .field(degreeRequest.getField())
                .institution(degreeRequest.getUniversityName())
                .awardDate(degreeRequest.getGraduationDate())
                .grade(degreeRequest.getGrade())
                .build();
        
        Map<String, Object> credentialData = 
            pt.ulusofona.ulht.credential.builder.CredentialDataBuilder
                .buildUniversityDegreeCredential(
                    null,  // Will be set by mapping
                    degreeRequest.getIssuerDid(),
                    degreeRequest.getSubjectDid(),
                    degreeData
                );
        
        // Build credential mapping
        pt.ulusofona.ulht.credential.dto.waltid.CredentialMapping mapping = 
            pt.ulusofona.ulht.credential.builder.CredentialDataBuilder.buildDefaultMapping();
        
        // Build issuance request
        pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request = 
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest.builder()
                .issuerKey(convertToIssuerKey(degreeRequest.getIssuerKey()))
                .issuerDid(degreeRequest.getIssuerDid())
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")
                .credentialData(credentialData)
                .mapping(mapping)
                .authenticationMethod("PRE_AUTHORIZED")
                .standardVersion("DRAFT13")
                .build();
        
        return issueJwtCredential(request);
    }

    /**
     * Get issuer metadata (OpenID4VCI)
     */
    public Map<String, Object> getIssuerMetadata() throws ExternalServiceException {
        logger.info("Retrieving issuer metadata");
        
        ResponseEntity<Map<String, Object>> response = executeOperation(
            () -> waltidIssuerClient.getIssuerMetadata(),
            "WaltID Issuer Metadata"
        );
        
        return response.getBody();
    }

    /**
     * Helper method to convert issuer key from request format
     */
    private pt.ulusofona.ulht.credential.dto.waltid.IssuerKey convertToIssuerKey(Object keyObject) {
        if (keyObject == null) {
            logger.warn("Issuer key object is null");
            return null;
        }
        
        try {
            // If it's already an IssuerKey, return it
            if (keyObject instanceof pt.ulusofona.ulht.credential.dto.waltid.IssuerKey) {
                return (pt.ulusofona.ulht.credential.dto.waltid.IssuerKey) keyObject;
            }
            
            // Try to convert from Map
            if (keyObject instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> keyMap = (Map<String, Object>) keyObject;
                
                // Get type - default to "jwk" if not specified
                String type = (String) keyMap.get("type");
                if (type == null || type.trim().isEmpty()) {
                    type = "jwk";  // Default to jwk
                    logger.debug("Issuer key type not specified, defaulting to 'jwk'");
                }
                
                pt.ulusofona.ulht.credential.dto.waltid.IssuerKey.IssuerKeyBuilder builder = 
                    pt.ulusofona.ulht.credential.dto.waltid.IssuerKey.builder().type(type);
                
                // Handle JWK type
                if ("jwk".equals(type)) {
                    Object jwkObj = keyMap.get("jwk");
                    if (jwkObj instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> jwkMap = (Map<String, Object>) jwkObj;
                        
                        pt.ulusofona.ulht.credential.dto.waltid.JwkKey jwk = 
                            pt.ulusofona.ulht.credential.dto.waltid.JwkKey.builder()
                                .kty((String) jwkMap.get("kty"))
                                .d((String) jwkMap.get("d"))
                                .crv((String) jwkMap.get("crv"))
                                .kid((String) jwkMap.get("kid"))
                                .x((String) jwkMap.get("x"))
                                .y((String) jwkMap.get("y"))
                                .n((String) jwkMap.get("n"))
                                .e((String) jwkMap.get("e"))
                                .build();
                        
                        builder.jwk(jwk);
                        logger.debug("Converted JWK key with kid: {}", jwk.getKid());
                    } else if (jwkObj == null) {
                        // Maybe the whole keyMap IS the JWK
                        logger.debug("No 'jwk' field found, treating entire object as JWK");
                        if (keyMap.containsKey("kty") && keyMap.containsKey("kid")) {
                            pt.ulusofona.ulht.credential.dto.waltid.JwkKey jwk = 
                                pt.ulusofona.ulht.credential.dto.waltid.JwkKey.builder()
                                    .kty((String) keyMap.get("kty"))
                                    .d((String) keyMap.get("d"))
                                    .crv((String) keyMap.get("crv"))
                                    .kid((String) keyMap.get("kid"))
                                    .x((String) keyMap.get("x"))
                                    .y((String) keyMap.get("y"))
                                    .n((String) keyMap.get("n"))
                                    .e((String) keyMap.get("e"))
                                    .build();
                            
                            builder.type("jwk").jwk(jwk);
                            logger.debug("Treated entire object as JWK with kid: {}", jwk.getKid());
                        }
                    }
                } else {
                    // KMS reference
                    builder.kmsRef(keyMap.get("kmsRef"));
                }
                
                pt.ulusofona.ulht.credential.dto.waltid.IssuerKey result = builder.build();
                logger.debug("Converted issuer key with type: {}", result.getType());
                return result;
            }
            
            logger.warn("Unable to convert issuer key from type: {}", keyObject.getClass());
            return null;
            
        } catch (Exception e) {
            logger.error("Error converting issuer key: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Builds selective disclosure configuration for Educational ID credential
     * Allows students to selectively disclose specific fields when presenting their credential
     */
    private Map<String, Object> buildEducationalIdSelectiveDisclosure() {
        Map<String, Object> sdConfig = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        
        // IssuanceDate - can be selectively disclosed
        Map<String, Object> issuanceDate = new HashMap<>();
        issuanceDate.put("sd", true);
        fields.put("issuanceDate", issuanceDate);
        
        // CredentialSubject - has nested fields
        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("sd", false);  // Don't hide the whole subject
        
        Map<String, Object> children = new HashMap<>();
        Map<String, Object> subjectFields = new HashMap<>();
        
        // Make date of birth selectively disclosable (sensitive info)
        Map<String, Object> dateOfBirth = new HashMap<>();
        dateOfBirth.put("sd", true);
        subjectFields.put("dateOfBirth", dateOfBirth);
        
        // Make email selectively disclosable (privacy)
        Map<String, Object> mail = new HashMap<>();
        mail.put("sd", true);
        subjectFields.put("mail", mail);
        
        // Make identifiers selectively disclosable (privacy)
        Map<String, Object> identifier = new HashMap<>();
        identifier.put("sd", true);
        subjectFields.put("identifier", identifier);
        
        Map<String, Object> schacPersonalUniqueCode = new HashMap<>();
        schacPersonalUniqueCode.put("sd", true);
        subjectFields.put("schacPersonalUniqueCode", schacPersonalUniqueCode);
        
        Map<String, Object> schacPersonalUniqueID = new HashMap<>();
        schacPersonalUniqueID.put("sd", true);
        subjectFields.put("schacPersonalUniqueID", schacPersonalUniqueID);
        
        // Name fields - can be selectively disclosed
        Map<String, Object> familyName = new HashMap<>();
        familyName.put("sd", true);
        subjectFields.put("familyName", familyName);
        
        Map<String, Object> firstName = new HashMap<>();
        firstName.put("sd", true);
        subjectFields.put("firstName", firstName);
        
        Map<String, Object> displayName = new HashMap<>();
        displayName.put("sd", true);
        subjectFields.put("displayName", displayName);
        
        Map<String, Object> commonName = new HashMap<>();
        commonName.put("sd", true);
        subjectFields.put("commonName", commonName);
        
        // eduPerson fields - can be selectively disclosed
        Map<String, Object> eduPersonPrincipalName = new HashMap<>();
        eduPersonPrincipalName.put("sd", true);
        subjectFields.put("eduPersonPrincipalName", eduPersonPrincipalName);
        
        Map<String, Object> eduPersonAffiliation = new HashMap<>();
        eduPersonAffiliation.put("sd", true);
        subjectFields.put("eduPersonAffiliation", eduPersonAffiliation);
        
        Map<String, Object> eduPersonScopedAffiliation = new HashMap<>();
        eduPersonScopedAffiliation.put("sd", true);
        subjectFields.put("eduPersonScopedAffiliation", eduPersonScopedAffiliation);
        
        Map<String, Object> eduPersonAssurance = new HashMap<>();
        eduPersonAssurance.put("sd", true);
        subjectFields.put("eduPersonAssurance", eduPersonAssurance);
        
        children.put("fields", subjectFields);
        credentialSubject.put("children", children);
        fields.put("credentialSubject", credentialSubject);
        
        sdConfig.put("fields", fields);
        return sdConfig;
    }
    
    /**
     * Builds selective disclosure configuration for European Student Card
     * Allows students to selectively disclose specific fields when presenting their credential
     */
    private Map<String, Object> buildEuropeanStudentCardSelectiveDisclosure() {
        Map<String, Object> sdConfig = new HashMap<>();
        Map<String, Object> fields = new HashMap<>();
        
        // IssuanceDate - can be selectively disclosed
        Map<String, Object> issuanceDate = new HashMap<>();
        issuanceDate.put("sd", true);
        fields.put("issuanceDate", issuanceDate);
        
        // CredentialSubject - has nested fields
        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("sd", false);  // Don't hide the whole subject
        
        Map<String, Object> children = new HashMap<>();
        Map<String, Object> subjectFields = new HashMap<>();
        
        // Student card object has nested fields
        Map<String, Object> studentCard = new HashMap<>();
        studentCard.put("sd", false);
        
        Map<String, Object> cardChildren = new HashMap<>();
        Map<String, Object> cardFields = new HashMap<>();
        
        // Make sensitive information selectively disclosable
        Map<String, Object> givenName = new HashMap<>();
        givenName.put("sd", true);
        cardFields.put("givenName", givenName);
        
        Map<String, Object> familyName = new HashMap<>();
        familyName.put("sd", true);
        cardFields.put("familyName", familyName);
        
        Map<String, Object> email = new HashMap<>();
        email.put("sd", true);
        cardFields.put("email", email);
        
        Map<String, Object> esi = new HashMap<>();
        esi.put("sd", true);
        cardFields.put("esi", esi);
        
        Map<String, Object> escn = new HashMap<>();
        escn.put("sd", true);
        cardFields.put("escn", escn);
        
        Map<String, Object> validFrom = new HashMap<>();
        validFrom.put("sd", true);
        cardFields.put("validFrom", validFrom);
        
        Map<String, Object> validUntil = new HashMap<>();
        validUntil.put("sd", true);
        cardFields.put("validUntil", validUntil);
        
        cardChildren.put("fields", cardFields);
        studentCard.put("children", cardChildren);
        subjectFields.put("studentCard", studentCard);
        
        children.put("fields", subjectFields);
        credentialSubject.put("children", children);
        fields.put("credentialSubject", credentialSubject);
        
        sdConfig.put("fields", fields);
        return sdConfig;
    }

    /**
     * Issues an Educational ID credential (SCHAC-based)
     * 
     * @param eduIdRequest The educational ID credential request
     * @return OID4VCI credential offer URL
     */
    public String issueEducationalIdCredential(
            pt.ulusofona.ulht.credential.dto.EducationalIdCredentialRequest eduIdRequest) 
            throws ExternalServiceException {
        
        logger.info("Issuing Educational ID credential for: {}", 
            eduIdRequest.getIdentifier());
        
        // Build SCHAC-compliant unique codes
        String orgDomain = eduIdRequest.getSchacHomeOrganization();
        String identifier = eduIdRequest.getIdentifier();
        
        java.util.List<String> schacCodes = java.util.Arrays.asList(
            String.format("urn:schac:personalUniqueCode:int:esi:urn:esid:%s:%s", 
                orgDomain, identifier)
        );
        
        String schacPersonalUniqueID = String.format(
            "urn:schac:personalUniqueID:PT:%s:%s", orgDomain, identifier);
        
        String displayName = eduIdRequest.getFirstName() + " " + eduIdRequest.getFamilyName();
        String eduPersonPrincipalName = eduIdRequest.getMail();
        
        java.util.List<String> scopedAffiliations = java.util.Arrays.asList(
            eduIdRequest.getEduPersonPrimaryAffiliation() + "@" + orgDomain
        );
        
        // Build credential data
        pt.ulusofona.ulht.credential.dto.waltid.EducationalIdData eduIdData = 
            pt.ulusofona.ulht.credential.dto.waltid.EducationalIdData.builder()
                .id(identifier)
                .identifier(identifier)
                .schacPersonalUniqueCode(schacCodes)
                .schacPersonalUniqueID(schacPersonalUniqueID)
                .schacHomeOrganization(orgDomain)
                .familyName(eduIdRequest.getFamilyName())
                .firstName(eduIdRequest.getFirstName())
                .displayName(displayName)
                .dateOfBirth(eduIdRequest.getDateOfBirth())
                .commonName(displayName)
                .mail(eduIdRequest.getMail())
                .eduPersonPrincipalName(eduPersonPrincipalName)
                .eduPersonPrimaryAffiliation(eduIdRequest.getEduPersonPrimaryAffiliation())
                .eduPersonAffiliation(eduIdRequest.getEduPersonAffiliation() != null ? 
                    eduIdRequest.getEduPersonAffiliation() : 
                    java.util.Arrays.asList(eduIdRequest.getEduPersonPrimaryAffiliation()))
                .eduPersonScopedAffiliation(scopedAffiliations)
                .eduPersonAssurance(eduIdRequest.getEduPersonAssurance() != null ?
                    eduIdRequest.getEduPersonAssurance() :
                    java.util.Arrays.asList("https://refeds.org/assurance/IAP/low"))
                .build();
        
        Map<String, Object> credentialData = 
            pt.ulusofona.ulht.credential.builder.CredentialDataBuilder
                .buildEducationalIdCredential(
                    null,  // Will be set by mapping
                    eduIdRequest.getIssuerDid(),
                    eduIdRequest.getSubjectDid(),
                    eduIdData
                );
        
        // Build credential mapping
        pt.ulusofona.ulht.credential.dto.waltid.CredentialMapping mapping = 
            pt.ulusofona.ulht.credential.builder.CredentialDataBuilder.buildDefaultMapping();
        
        // Build issuance request with selective disclosure
        // Note: Using UniversityDegree config as walt.id doesn't have EducationalID by default
        // The credential type is determined by credentialData.type, not the configuration ID
        pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request = 
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest.builder()
                .issuerKey(convertToIssuerKey(eduIdRequest.getIssuerKey()))
                .issuerDid(eduIdRequest.getIssuerDid())
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")  // Use supported config
                .credentialData(credentialData)
                .mapping(mapping)
                .authenticationMethod("PRE_AUTHORIZED")
                .standardVersion("DRAFT13")
                .selectiveDisclosure(buildEducationalIdSelectiveDisclosure())
                .build();
        
        // Use SD-JWT for selective disclosure support
        return issueSdJwtCredential(request);
    }

    /**
     * Issues a European Student Card credential
     * 
     * @param escRequest The European Student Card credential request
     * @return OID4VCI credential offer URL
     */
    public String issueEuropeanStudentCardCredential(
            pt.ulusofona.ulht.credential.dto.EuropeanStudentCardRequest escRequest) 
            throws ExternalServiceException {
        
        logger.info("Issuing European Student Card for ESI: {}", 
            escRequest.getEsi());
        
        // Build credential data
        pt.ulusofona.ulht.credential.dto.waltid.EuropeanStudentCardData escData = 
            pt.ulusofona.ulht.credential.dto.waltid.EuropeanStudentCardData.builder()
                .id(escRequest.getEsi())
                .givenName(escRequest.getGivenName())
                .familyName(escRequest.getFamilyName())
                .email(escRequest.getEmail())
                .esi(escRequest.getEsi())
                .escn(escRequest.getEscn())
                .institutionPIC(escRequest.getInstitutionPIC())
                .institutionName(escRequest.getInstitutionName())
                .academicLevel(escRequest.getAcademicLevel())
                .cardType(escRequest.getCardType() != null ? 
                    escRequest.getCardType() : "Digital")
                .validFrom(escRequest.getValidFrom())
                .validUntil(escRequest.getValidUntil())
                .build();
        
        Map<String, Object> credentialData = 
            pt.ulusofona.ulht.credential.builder.CredentialDataBuilder
                .buildEuropeanStudentCardCredential(
                    null,  // Will be set by mapping
                    escRequest.getIssuerDid(),
                    escRequest.getSubjectDid(),
                    escData
                );
        
        // Build credential mapping with custom expiration based on validUntil
        pt.ulusofona.ulht.credential.dto.waltid.CredentialMapping mapping = 
            pt.ulusofona.ulht.credential.builder.CredentialDataBuilder.buildDefaultMapping();
        
        // Build issuance request with selective disclosure
        // Note: Using UniversityDegree config as walt.id doesn't have EuropeanStudentCard by default
        // The credential type is determined by credentialData.type, not the configuration ID
        pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request = 
            pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest.builder()
                .issuerKey(convertToIssuerKey(escRequest.getIssuerKey()))
                .issuerDid(escRequest.getIssuerDid())
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")  // Use supported config
                .credentialData(credentialData)
                .mapping(mapping)
                .authenticationMethod("PRE_AUTHORIZED")
                .standardVersion("DRAFT13")
                .selectiveDisclosure(buildEuropeanStudentCardSelectiveDisclosure())
                .build();
        
        // Use SD-JWT for selective disclosure support
        return issueSdJwtCredential(request);
    }
}
