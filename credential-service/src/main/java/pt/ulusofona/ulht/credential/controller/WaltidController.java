package pt.ulusofona.ulht.credential.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import pt.ulusofona.ulht.credential.domain.authentication.LoginRequest;
import pt.ulusofona.ulht.credential.domain.authentication.LoginResponse;
import pt.ulusofona.ulht.credential.domain.authentication.RegisterRequest;
import pt.ulusofona.ulht.credential.domain.authentication.RegisterResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletAccountResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletKeysResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletDidResponse;
import pt.ulusofona.ulht.credential.exception.ExternalServiceException;
import pt.ulusofona.ulht.credential.exception.ValidationException;
import pt.ulusofona.ulht.credential.exception.TimeoutException;
import pt.ulusofona.ulht.credential.exception.ConflictException;
import pt.ulusofona.ulht.credential.exception.ForbiddenException;
import pt.ulusofona.ulht.credential.exception.UnauthorizedException;
import pt.ulusofona.ulht.credential.exception.BadRequestException;
import pt.ulusofona.ulht.credential.exception.NotFoundException;
import pt.ulusofona.ulht.credential.service.WaltidService;
import pt.ulusofona.ulht.credential.service.StudentWalletService;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * WaltID Wallet Controller - Wrapper around external WaltID Wallet API.
 * 
 * This controller provides a REST API interface that proxies requests to the external WaltID Wallet service.
 * We do NOT build a custom wallet - we use WaltID Wallet as an external service, similar to how we use
 * WaltID Issuer and WaltID Verifier.
 * 
 * Authentication: Supports cookie-based (preferred) and bearer token authentication methods.
 * 
 * @see <a href="https://docs.walt.id/v/stable/wallet/wallet-api">WaltID Wallet API Documentation</a>
 */
@Slf4j
@RestController
@RequestMapping("/wallet")
@Import(FeignClientsConfiguration.class)
@Tag(
    name = "Wallet Authentication", 
    description = "Endpoints for wallet authentication and user management. " +
                  "These endpoints are a wrapper/proxy around the external WaltID Wallet API. " +
                  "We use WaltID Wallet as an external service (not a custom-built wallet)."
)
@SecurityRequirement(name = "sessionAuth")
public class WaltidController {

    private final WaltidService waltidService;
    private final StudentWalletService studentWalletService;

    public WaltidController(WaltidService waltidService, StudentWalletService studentWalletService) {
        this.waltidService = waltidService;
        this.studentWalletService = studentWalletService;
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Wallet login authentication",
        description = "Authenticates user credentials for wallet access and returns JWT token with session cookie. " +
                     "This endpoint validates user credentials against the WaltID wallet system and establishes " +
                     "an authenticated session for subsequent wallet operations.",
        operationId = "walletLogin"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Login request containing email and password",
        required = true,
        content = @Content(
            schema = @Schema(implementation = LoginRequest.class),
            examples = {
                @ExampleObject(
                    name = "Standard Login",
                    summary = "User login with email and password",
                    value = """
                    {
                      "email": "student@ulusofona.pt",
                      "password": "securePassword123",
                      "type": "email"
                    }
                    """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully authenticated user",
            content = @Content(
                schema = @Schema(implementation = LoginResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "User authenticated successfully",
                        value = """
                        {
                          "userId": "12345",
                          "username": "student@ulusofona.pt",
                          "jwtToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                          "sessionCookie": "login=abc123def456; Path=/; HttpOnly"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - invalid credentials",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Authentication Error",
                        summary = "Invalid credentials",
                        value = """
                        {
                          "error": "Invalid email or password",
                          "errorCode": "INVALID_CREDENTIALS",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad request - invalid input data",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Validation Error",
                        summary = "Missing required field",
                        value = """
                        {
                          "error": "email is required",
                          "errorCode": "VALIDATION_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error or external service unavailable",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Server Error",
                        summary = "Internal server error",
                        value = """
                        {
                          "error": "Internal server error",
                          "errorCode": "INTERNAL_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        )
    })
    public LoginResponse loginWallet(@RequestBody LoginRequest request) throws ValidationException, ExternalServiceException, TimeoutException, ConflictException, ForbiddenException, UnauthorizedException, BadRequestException, NotFoundException {
        log.info("Received wallet login request for user: {}", request.getEmail());
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("type is required");
        }
        
        LoginResponse response = waltidService.walletLogin(request);
        log.info("Wallet login successful for user: {}", request.getEmail());
        return response;
    }

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Wallet user registration",
        description = "Registers a new user for wallet access. This endpoint creates a new user account " +
                     "in the WaltID wallet system with the provided credentials and user information.",
        operationId = "walletRegister"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Registration request containing user details",
        required = true,
        content = @Content(
            schema = @Schema(implementation = RegisterRequest.class),
            examples = {
                @ExampleObject(
                    name = "Student Registration",
                    summary = "New student registration",
                    value = """
                    {
                      "email": "newstudent@ulusofona.pt",
                      "password": "securePassword123",
                      "name": "João Silva",
                      "type": "email"
                    }
                    """
                ),
                @ExampleObject(
                    name = "Faculty Registration",
                    summary = "New faculty registration",
                    value = """
                    {
                      "email": "professor@ulusofona.pt",
                      "password": "securePassword123",
                      "name": "Dr. Maria Santos",
                      "type": "email"
                    }
                    """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully registered user",
            content = @Content(
                schema = @Schema(implementation = RegisterResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "User registered successfully",
                        value = """
                        {
                          "userId": "12345",
                          "username": "newstudent@ulusofona.pt",
                          "status": "REGISTERED",
                          "message": "User registered successfully"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Conflict - user already exists",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Conflict Error",
                        summary = "User already exists",
                        value = """
                        {
                          "error": "User already exists",
                          "errorCode": "USER_ALREADY_EXISTS",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad request - invalid input data",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Validation Error",
                        summary = "Missing required field",
                        value = """
                        {
                          "error": "email is required",
                          "errorCode": "VALIDATION_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Server Error",
                        summary = "Internal server error",
                        value = """
                        {
                          "error": "Internal server error",
                          "errorCode": "INTERNAL_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        )
    })
    public RegisterResponse registerWallet(@RequestBody RegisterRequest request) throws ExternalServiceException, TimeoutException, ValidationException, ConflictException, ForbiddenException, UnauthorizedException, BadRequestException, NotFoundException {
        log.info("Received wallet registration request for user: {}", request.getEmail());
        
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("email is required");
        }
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("password is required");
        }
        if (request.getType() == null || request.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("type is required");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        
        RegisterResponse response = waltidService.walletRegister(request);
        log.info("Wallet registration successful for user: {}", request.getEmail());
        return response;
    }

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get wallet accounts",
        description = "Retrieves account information and associated wallets for the authenticated user. " +
                     "This endpoint requires a valid session cookie from a previous login operation.",
        operationId = "getWalletAccounts"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully retrieved wallet accounts",
            content = @Content(
                schema = @Schema(implementation = WalletAccountResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "Wallet accounts retrieved successfully",
                        value = """
                        {
                          "userId": "12345",
                          "accounts": [
                            {
                              "accountId": "acc_123",
                              "name": "Main Wallet",
                              "type": "DID",
                              "did": "did:example:123456789",
                              "status": "ACTIVE"
                            }
                          ],
                          "totalAccounts": 1
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - invalid or missing session cookie",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Authentication Error",
                        summary = "Invalid session",
                        value = """
                        {
                          "error": "Invalid or missing session cookie",
                          "errorCode": "UNAUTHORIZED",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "No wallet accounts found",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Not Found",
                        summary = "No accounts found",
                        value = """
                        {
                          "error": "No wallet accounts found",
                          "errorCode": "NOT_FOUND",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Server Error",
                        summary = "Internal server error",
                        value = """
                        {
                          "error": "Internal server error",
                          "errorCode": "INTERNAL_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        )
    })
    public WalletAccountResponse getWalletAccounts(@CookieValue(value = "login", required = false) String loginCookie) throws ValidationException, ExternalServiceException, TimeoutException, ConflictException, ForbiddenException, UnauthorizedException, BadRequestException, NotFoundException {
        log.info("Received request to retrieve wallet accounts");
        log.info("loginCookie received: {}", loginCookie);
        try {
            WalletAccountResponse response = waltidService.getWalletAccounts(loginCookie);
            log.info("Successfully retrieved wallet accounts");
            return response;
        } catch (Exception e) {
            log.error("Failed to retrieve wallet accounts: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping(value = "/{walletId}/keys", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get wallet keys",
        description = "Retrieves keys information for a specific wallet. " +
                     "This endpoint requires a valid session cookie from a previous login operation.",
        operationId = "getWalletKeys"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully retrieved wallet keys",
            content = @Content(
                schema = @Schema(implementation = WalletKeysResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "Wallet keys retrieved successfully",
                        value = """
                        {
                          "keys": [
                            {
                              "algorithm": "Ed25519",
                              "cryptoProvider": "[walt.id crypto private Ed25519 key]",
                              "keyId": {
                                "id": "rFVOrHQbq0pTBV0G0L6CEEiYFOQ-vw9b95CKDTHja88"
                              },
                              "keyPair": {},
                              "keysetHandle": null
                            }
                          ]
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - invalid or missing session cookie",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Authentication Error",
                        summary = "Invalid session",
                        value = """
                        {
                          "error": "Invalid or missing session cookie",
                          "errorCode": "UNAUTHORIZED",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404", 
            description = "Wallet not found",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Not Found",
                        summary = "Wallet not found",
                        value = """
                        {
                          "error": "Wallet not found",
                          "errorCode": "NOT_FOUND",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Server Error",
                        summary = "Internal server error",
                        value = """
                        {
                          "error": "Internal server error",
                          "errorCode": "INTERNAL_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        )
    })
    public WalletKeysResponse getWalletKeys(@PathVariable("walletId") String walletId, @CookieValue(value = "login", required = false) String loginCookie) throws ValidationException, ExternalServiceException, TimeoutException, ConflictException, ForbiddenException, UnauthorizedException, BadRequestException, NotFoundException {
        log.info("Received request to retrieve keys for wallet: {}", walletId);
        log.info("loginCookie received: {}", loginCookie);
        try {
            WalletKeysResponse response = waltidService.getWalletKeys(walletId, loginCookie);
            log.info("Successfully retrieved keys for wallet: {}", walletId);
            return response;
        } catch (Exception e) {
            log.error("Failed to retrieve keys for wallet {}: {}", walletId, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping(value = "/{walletId}/dids", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get wallet DIDs",
        description = "Retrieves DIDs for a specific wallet. Requires a valid session cookie.",
        operationId = "getWalletDids"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved wallet DIDs",
            content = @Content(
                schema = @Schema(implementation = WalletDidResponse.class)
            )
        )
    })
    public List<WalletDidResponse> getWalletDids(
        @PathVariable("walletId") String walletId,
        @CookieValue(value = "login", required = false) String loginCookie
    ) throws ExternalServiceException {
        return waltidService.getWalletDids(walletId, loginCookie);
    }

    @PostMapping(value = "/issue-jwt-credential", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Issue JWT credential via OpenID4VC",
        description = "Issues a W3C Verifiable Credential using JWT format and starts an OIDC credential exchange flow. " +
                     "This endpoint creates a credential issuance URL that can be used to complete the credential exchange process.",
        operationId = "issueJwtCredential"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Credential issuance request containing issuer key, credential configuration, and credential data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = Object.class),
            examples = {
                @ExampleObject(
                    name = "University Degree Credential",
                    summary = "Issue a university degree credential",
                    value = """
                    {
                      "issuerKey": {
                        "type": "jwk",
                        "jwk": {
                          "kty": "OKP",
                          "d": "mDhpwaH6JYSrD2Bq7Cs-pzmsjlLj4EOhxyI-9DM1mFI",
                          "crv": "Ed25519",
                          "kid": "Vzx7l5fh56F3Pf9aR3DECU5BwfrY6ZJe05aiWYWzan8",
                          "x": "T3T4-u1Xz3vAV2JwPNxWfs4pik_JLiArz_WTCvrCFUM"
                        }
                      },
                      "credentialConfigurationId": "UniversityDegree_jwt_vc_json",
                      "credentialData": {
                        "@context": ["https://www.w3.org/2018/credentials/v1", "https://www.w3.org/2018/credentials/examples/v1"],
                        "id": "http://example.gov/credentials/3732",
                        "type": ["VerifiableCredential", "UniversityDegreeCredential"],
                        "issuer": {
                          "id": "did:web:vc.transmute.world"
                        },
                        "issuanceDate": "2020-03-10T04:24:12.164Z",
                        "credentialSubject": {
                          "id": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                          "degree": {
                            "type": "BachelorDegree",
                            "name": "Bachelor of Science and Arts"
                          }
                        }
                      },
                      "mapping": {
                        "id": "<uuid>",
                        "issuer": {
                          "id": "<issuerDid>"
                        },
                        "credentialSubject": {
                          "id": "<subjectDid>"
                        },
                        "issuanceDate": "<timestamp>",
                        "expirationDate": "<timestamp-in:365d>"
                      },
                      "issuerDid": "did:key:z6MkjoRhq1jSNJdLiruSXrFFxagqrztZaXHqHGUTKJbcNywp"
                    }
                    """
                ),
                @ExampleObject(
                    name = "Open Badge Credential",
                    summary = "Issue an open badge credential",
                    value = """
                    {
                      "issuerKey": {
                        "type": "jwk",
                        "jwk": {
                          "kty": "OKP",
                          "d": "mDhpwaH6JYSrD2Bq7Cs-pzmsjlLj4EOhxyI-9DM1mFI",
                          "crv": "Ed25519",
                          "kid": "Vzx7l5fh56F3Pf9aR3DECU5BwfrY6ZJe05aiWYWzan8",
                          "x": "T3T4-u1Xz3vAV2JwPNxWfs4pik_JLiArz_WTCvrCFUM"
                        }
                      },
                      "credentialConfigurationId": "OpenBadgeCredential_jwt_vc_json",
                      "credentialData": {
                        "@context": ["https://www.w3.org/2018/credentials/v1", "https://purl.imsglobal.org/spec/ob/v3p0/context.json"],
                        "id": "urn:uuid:THIS WILL BE REPLACED WITH DYNAMIC DATA FUNCTION",
                        "type": ["VerifiableCredential", "OpenBadgeCredential"],
                        "name": "JFF x vc-edu PlugFest 3 Interoperability",
                        "issuer": {
                          "type": ["Profile"],
                          "name": "Jobs for the Future (JFF)",
                          "url": "https://www.jff.org/",
                          "image": "https://w3c-ccg.github.io/vc-ed/plugfest-1-2022/images/JFF_LogoLockup.png"
                        },
                        "credentialSubject": {
                          "type": ["AchievementSubject"],
                          "achievement": {
                            "id": "urn:uuid:ac254bd5-8fad-4bb1-9d29-efd938536926",
                            "type": ["Achievement"],
                            "name": "JFF x vc-edu PlugFest 3 Interoperability",
                            "description": "This wallet supports the use of W3C Verifiable Credentials and has demonstrated interoperability during the presentation request workflow during JFF x VC-EDU PlugFest 3."
                          }
                        }
                      },
                      "mapping": {
                        "id": "<uuid>",
                        "issuer": {
                          "id": "<issuerDid>"
                        },
                        "credentialSubject": {
                          "id": "<subjectDid>"
                        },
                        "issuanceDate": "<timestamp>",
                        "expirationDate": "<timestamp-in:365d>"
                      },
                      "issuerDid": "did:key:z6MkjoRhq1jSNJdLiruSXrFFxagqrztZaXHqHGUTKJbcNywp"
                    }
                    """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully issued JWT credential",
            content = @Content(
                schema = @Schema(implementation = String.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "Credential issuance URL generated",
                        value = """
                        "openid-credential-offer://localhost/?credential_offer=%7B%22credential_issuer%22%3A%22http%3A%2F%2Flocalhost%3A8000%22%2C%22credentials%22%3A%5B%22VerifiableId%22%5D%2C%22grants%22%3A%7B%22authorization_code%22%3A%7B%22issuer_state%22%3A%22501414a4-c461-43f0-84b2-c628730c7c02%22%7D%7D%7D"
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad request - invalid credential data",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Validation Error",
                        summary = "Missing required field",
                        value = """
                        {
                          "error": "Missing issuerKey in the request body",
                          "errorCode": "VALIDATION_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error or external service unavailable",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Server Error",
                        summary = "Internal server error",
                        value = """
                        {
                          "error": "Failed to issue JWT credential",
                          "errorCode": "INTERNAL_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        )
    })
    public String issueJwtCredential(@RequestBody Object requestBody) throws ValidationException, ExternalServiceException, TimeoutException, ConflictException, ForbiddenException, UnauthorizedException, BadRequestException, NotFoundException {
        log.info("Received request to issue JWT credential (legacy endpoint, consider using /issue-w3c-jwt-credential)");
        
        if (requestBody == null) {
            throw new IllegalArgumentException("Request body is required");
        }
        
        try {
            // Try to convert the request to IssueCredentialRequest
            if (requestBody instanceof pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest) {
                return waltidService.issueJwtCredential(
                    (pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest) requestBody);
            }
            
            // Invalid request format
            log.error("Invalid JWT credential request format. Use /issue-w3c-jwt-credential endpoint instead.");
            throw new IllegalArgumentException(
                "Invalid request format. This is a legacy endpoint. " +
                "Please use /issue-w3c-jwt-credential with proper IssueCredentialRequest format."
            );
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to issue JWT credential: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/issue-university-degree", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Issue University Degree Credential",
        description = "Issues a University Degree Credential using JWT format and starts an OIDC credential exchange flow. " +
                     "This endpoint is specifically designed for university degree credentials with a simplified request format.",
        operationId = "issueUniversityDegreeCredential"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "University Degree Credential request containing issuer key, subject DID, and degree information",
        required = true,
        content = @Content(
            schema = @Schema(implementation = Object.class),
            examples = {
                @ExampleObject(
                    name = "Bachelor Degree",
                    summary = "Issue a Bachelor degree credential",
                    value = """
                    {
                      "issuerKey": {
                        "type": "jwk",
                        "jwk": {
                          "kty": "OKP",
                          "d": "mDhpwaH6JYSrD2Bq7Cs-pzmsjlLj4EOhxyI-9DM1mFI",
                          "crv": "Ed25519",
                          "kid": "Vzx7l5fh56F3Pf9aR3DECU5BwfrY6ZJe05aiWYWzan8",
                          "x": "T3T4-u1Xz3vAV2JwPNxWfs4pik_JLiArz_WTCvrCFUM"
                        }
                      },
                      "issuerDid": "did:key:z6MkjoRhq1jSNJdLiruSXrFFxagqrztZaXHqHGUTKJbcNywp",
                      "subjectDid": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                      "degree": {
                        "type": "BachelorDegree",
                        "name": "Bachelor of Science and Arts"
                      },
                      "credentialId": "http://example.gov/credentials/3732"
                    }
                    """
                ),
                @ExampleObject(
                    name = "Master Degree",
                    summary = "Issue a Master degree credential",
                    value = """
                    {
                      "issuerKey": {
                        "type": "jwk",
                        "jwk": {
                          "kty": "OKP",
                          "d": "mDhpwaH6JYSrD2Bq7Cs-pzmsjlLj4EOhxyI-9DM1mFI",
                          "crv": "Ed25519",
                          "kid": "Vzx7l5fh56F3Pf9aR3DECU5BwfrY6ZJe05aiWYWzan8",
                          "x": "T3T4-u1Xz3vAV2JwPNxWfs4pik_JLiArz_WTCvrCFUM"
                        }
                      },
                      "issuerDid": "did:key:z6MkjoRhq1jSNJdLiruSXrFFxagqrztZaXHqHGUTKJbcNywp",
                      "subjectDid": "did:example:ebfeb1f712ebc6f1c276e12ec21",
                      "degree": {
                        "type": "MasterDegree",
                        "name": "Master of Computer Science"
                      }
                    }
                    """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Successfully issued University Degree Credential",
            content = @Content(
                schema = @Schema(implementation = String.class),
                examples = {
                    @ExampleObject(
                        name = "Success Response",
                        summary = "Credential issuance URL generated",
                        value = """
                        "openid-credential-offer://localhost/?credential_offer=%7B%22credential_issuer%22%3A%22http%3A%2F%2Flocalhost%3A8000%22%2C%22credentials%22%3A%5B%22UniversityDegree%22%5D%2C%22grants%22%3A%7B%22authorization_code%22%3A%7B%22issuer_state%22%3A%22501414a4-c461-43f0-84b2-c628730c7c02%22%7D%7D%7D"
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Bad request - invalid credential data",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Validation Error",
                        summary = "Missing required field",
                        value = """
                        {
                          "error": "Subject DID is required",
                          "errorCode": "VALIDATION_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error or external service unavailable",
            content = @Content(
                schema = @Schema(implementation = Map.class),
                examples = {
                    @ExampleObject(
                        name = "Server Error",
                        summary = "Internal server error",
                        value = """
                        {
                          "error": "Failed to issue University Degree Credential",
                          "errorCode": "INTERNAL_ERROR",
                          "timestamp": "2024-01-15T10:30:00Z"
                        }
                        """
                    )
                }
            )
        )
    })
    public String issueUniversityDegreeCredential(@RequestBody pt.ulusofona.ulht.credential.dto.UniversityDegreeCredentialRequest request) throws ValidationException, ExternalServiceException, TimeoutException, ConflictException, ForbiddenException, UnauthorizedException, BadRequestException, NotFoundException {
        log.info("Received request to issue University Degree Credential for subject: {}", request.getSubjectDid());
        
        // Validate required fields
        if (request.getIssuerKey() == null) {
            throw new IllegalArgumentException("Issuer key is required");
        }
        if (request.getIssuerDid() == null || request.getIssuerDid().trim().isEmpty()) {
            throw new IllegalArgumentException("Issuer DID is required");
        }
        if (request.getSubjectDid() == null || request.getSubjectDid().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject DID is required");
        }
        // Note: degree field is deprecated - using degreeType and degreeName instead
        
        try {
            String issuanceUrl = waltidService.issueUniversityDegreeCredential(request);
            log.info("Successfully issued University Degree Credential, issuance URL generated for subject: {}", request.getSubjectDid());
            return issuanceUrl;
        } catch (Exception e) {
            log.error("Failed to issue University Degree Credential for subject {}: {}", request.getSubjectDid(), e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/onboard-issuer",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Onboard an issuer for credential issuance",
        description = "Creates a signing key and DID for credential issuance. This is the first step before issuing credentials. " +
                     "The generated key and DID will be used to sign verifiable credentials. " +
                     "IMPORTANT: Save the returned key securely - it contains the private key component.",
        operationId = "onboardIssuer"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Onboarding configuration specifying key algorithm and DID method",
        required = true,
        content = @Content(
            schema = @Schema(implementation = Map.class),
            examples = {
                @ExampleObject(
                    name = "Ed25519 with did:jwk",
                    summary = "Onboard with Ed25519 key and did:jwk method",
                    value = """
                    {
                      "keyType": "ed25519",
                      "didMethod": "jwk"
                    }
                    """
                ),
                @ExampleObject(
                    name = "secp256r1 with did:key",
                    summary = "Onboard with secp256r1 key and did:key method",
                    value = """
                    {
                      "keyType": "secp256r1",
                      "didMethod": "key"
                    }
                    """
                )
            }
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully created issuer key and DID",
            content = @Content(
                schema = @Schema(implementation = pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during onboarding",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse onboardIssuer(
            @RequestBody Map<String, String> request)
            throws ValidationException, ExternalServiceException {
        
        log.info("Received request to onboard issuer");
        
        String keyType = request.getOrDefault("keyType", "secp256r1");
        String didMethod = request.getOrDefault("didMethod", "jwk");
        
        try {
            pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse response = 
                waltidService.onboardIssuer(keyType, didMethod);
            log.info("Successfully onboarded issuer with DID: {}", response.getIssuerDid());
            return response;
        } catch (Exception e) {
            log.error("Failed to onboard issuer: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/issue-w3c-jwt-credential",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Issue W3C Verifiable Credential (JWT format)",
        description = "Issues a W3C Verifiable Credential using JWT signature format and OID4VCI protocol. " +
                     "Returns a credential offer URL that can be scanned as QR code or pasted into a wallet.",
        operationId = "issueW3cJwtCredential"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Credential issuance request with issuer key, credential configuration, and data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest.class)
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully issued credential, returns OID4VCI offer URL",
            content = @Content(
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    value = "openid-credential-offer://issuer.portal.walt.id/?credential_offer=%7B%22credential_issuer%22..."
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during credential issuance",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public String issueW3cJwtCredential(
            @RequestBody pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request)
            throws ValidationException, ExternalServiceException {
        
        log.info("Received request to issue W3C JWT credential");
        
        try {
            String offerUrl = waltidService.issueJwtCredential(request);
            log.info("Successfully issued W3C JWT credential");
            return offerUrl;
        } catch (Exception e) {
            log.error("Failed to issue W3C JWT credential: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/issue-w3c-sdjwt-credential",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Issue W3C Verifiable Credential (SD-JWT format)",
        description = "Issues a W3C Verifiable Credential using SD-JWT (Selective Disclosure JWT) signature format. " +
                     "SD-JWT allows holders to selectively disclose only specific claims when presenting credentials, " +
                     "enhancing privacy. Returns a credential offer URL compatible with OID4VCI protocol.",
        operationId = "issueW3cSdJwtCredential"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Credential issuance request with issuer key, credential configuration, and data",
        required = true,
        content = @Content(
            schema = @Schema(implementation = pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest.class)
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully issued SD-JWT credential, returns OID4VCI offer URL",
            content = @Content(
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    value = "openid-credential-offer://issuer.portal.walt.id/?credential_offer=%7B%22credential_issuer%22..."
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during credential issuance",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public String issueW3cSdJwtCredential(
            @RequestBody pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request)
            throws ValidationException, ExternalServiceException {
        
        log.info("Received request to issue W3C SD-JWT credential");
        
        try {
            String offerUrl = waltidService.issueSdJwtCredential(request);
            log.info("Successfully issued W3C SD-JWT credential");
            return offerUrl;
        } catch (Exception e) {
            log.error("Failed to issue W3C SD-JWT credential: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping(value = "/issuer-metadata",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get issuer metadata (OpenID4VCI)",
        description = "Retrieves the OpenID4VCI issuer metadata including supported credential configurations, " +
                     "authorization endpoints, and other issuer capabilities.",
        operationId = "getIssuerMetadata"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved issuer metadata",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error retrieving metadata",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public Map<String, Object> getIssuerMetadata() throws ExternalServiceException {
        log.info("Received request to get issuer metadata");
        
        try {
            Map<String, Object> metadata = waltidService.getIssuerMetadata();
            log.info("Successfully retrieved issuer metadata");
            return metadata;
        } catch (Exception e) {
            log.error("Failed to get issuer metadata: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/issue-educational-id",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Issue Educational ID Credential (SCHAC)",
        description = "Issues an Educational Identity credential based on SCHAC (Schema for Academia) standards. " +
                     "This credential contains educational identity information including schacPersonalUniqueCode, " +
                     "eduPerson attributes, and institutional affiliation information. " +
                     "Returns a credential offer URL compatible with OID4VCI protocol.",
        operationId = "issueEducationalId"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "Educational ID credential request with SCHAC-compliant attributes",
        required = true,
        content = @Content(
            schema = @Schema(implementation = pt.ulusofona.ulht.credential.dto.EducationalIdCredentialRequest.class),
            examples = @ExampleObject(
                name = "Educational ID Example",
                value = """
                {
                  "identifier": "511114706",
                  "familyName": "Costa",
                  "firstName": "Luísa",
                  "mail": "luisa.costa@students.example.edu",
                  "dateOfBirth": "1970-01-01",
                  "schacHomeOrganization": "ulusofona.pt",
                  "eduPersonPrimaryAffiliation": "student",
                  "eduPersonAffiliation": ["student"],
                  "eduPersonAssurance": ["https://refeds.org/assurance/IAP/low"],
                  "subjectDid": "did:jwk:student123",
                  "issuerDid": "did:jwk:issuer456",
                  "issuerKey": {
                    "type": "jwk",
                    "jwk": {
                      "kty": "EC",
                      "d": "private-key",
                      "crv": "P-256",
                      "kid": "key-id",
                      "x": "x-coordinate",
                      "y": "y-coordinate"
                    }
                  }
                }
                """
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully issued Educational ID credential",
            content = @Content(
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    value = "openid-credential-offer://issuer.portal.walt.id/?credential_offer=..."
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during credential issuance",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public String issueEducationalIdCredential(
            @RequestBody pt.ulusofona.ulht.credential.dto.EducationalIdCredentialRequest request)
            throws ValidationException, ExternalServiceException {
        
        log.info("Received request to issue Educational ID credential for: {}", request.getIdentifier());
        
        // Validate required fields
        if (request.getIssuerKey() == null) {
            throw new IllegalArgumentException("Issuer key is required");
        }
        if (request.getIssuerDid() == null || request.getIssuerDid().trim().isEmpty()) {
            throw new IllegalArgumentException("Issuer DID is required");
        }
        if (request.getSubjectDid() == null || request.getSubjectDid().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject DID is required");
        }
        if (request.getIdentifier() == null || request.getIdentifier().trim().isEmpty()) {
            throw new IllegalArgumentException("Student identifier is required");
        }
        if (request.getSchacHomeOrganization() == null || request.getSchacHomeOrganization().trim().isEmpty()) {
            throw new IllegalArgumentException("Home organization is required");
        }
        if (request.getEduPersonPrimaryAffiliation() == null) {
            throw new IllegalArgumentException("Primary affiliation is required");
        }
        
        try {
            String offerUrl = waltidService.issueEducationalIdCredential(request);
            log.info("Successfully issued Educational ID credential for: {}", request.getIdentifier());
            return offerUrl;
        } catch (Exception e) {
            log.error("Failed to issue Educational ID credential: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping(value = "/issue-european-student-card",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(
        summary = "Issue European Student Card",
        description = "Issues a European Student Card (ESC) credential following the European Student Card Initiative standards. " +
                     "The credential includes the European Student Identifier (ESI), European Student Card Number (ESCN), " +
                     "institution PIC, academic level, and validity period. " +
                     "Returns a credential offer URL compatible with OID4VCI protocol.",
        operationId = "issueEuropeanStudentCard"
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "European Student Card credential request with ESI and institution information",
        required = true,
        content = @Content(
            schema = @Schema(implementation = pt.ulusofona.ulht.credential.dto.EuropeanStudentCardRequest.class),
            examples = @ExampleObject(
                name = "European Student Card Example",
                value = """
                {
                  "givenName": "Luísa",
                  "familyName": "Costa",
                  "email": "luisa.costa@aluno.ulusofona.pt",
                  "esi": "ESI-3618254416",
                  "escn": "511114706",
                  "institutionPIC": "997605425",
                  "institutionName": "ULHT - Universidade Lusófona",
                  "academicLevel": "bachelor",
                  "cardType": "Digital",
                  "validFrom": "2024-09-01",
                  "validUntil": "2025-08-31",
                  "subjectDid": "did:jwk:student123",
                  "issuerDid": "did:jwk:issuer456",
                  "issuerKey": {
                    "type": "jwk",
                    "jwk": {
                      "kty": "EC",
                      "d": "private-key",
                      "crv": "P-256",
                      "kid": "key-id",
                      "x": "x-coordinate",
                      "y": "y-coordinate"
                    }
                  }
                }
                """
            )
        )
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully issued European Student Card",
            content = @Content(
                schema = @Schema(type = "string"),
                examples = @ExampleObject(
                    value = "openid-credential-offer://issuer.portal.walt.id/?credential_offer=..."
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request parameters",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Server error during credential issuance",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public String issueEuropeanStudentCard(
            @RequestBody pt.ulusofona.ulht.credential.dto.EuropeanStudentCardRequest request)
            throws ValidationException, ExternalServiceException {
        
        log.info("Received request to issue European Student Card for ESI: {}", request.getEsi());
        
        // Validate required fields
        if (request.getIssuerKey() == null) {
            throw new IllegalArgumentException("Issuer key is required");
        }
        if (request.getIssuerDid() == null || request.getIssuerDid().trim().isEmpty()) {
            throw new IllegalArgumentException("Issuer DID is required");
        }
        if (request.getSubjectDid() == null || request.getSubjectDid().trim().isEmpty()) {
            throw new IllegalArgumentException("Subject DID is required");
        }
        if (request.getEsi() == null || request.getEsi().trim().isEmpty()) {
            throw new IllegalArgumentException("European Student Identifier (ESI) is required");
        }
        if (request.getEscn() == null || request.getEscn().trim().isEmpty()) {
            throw new IllegalArgumentException("European Student Card Number (ESCN) is required");
        }
        if (request.getInstitutionPIC() == null || request.getInstitutionPIC().trim().isEmpty()) {
            throw new IllegalArgumentException("Institution PIC is required");
        }
        if (request.getAcademicLevel() == null || request.getAcademicLevel().trim().isEmpty()) {
            throw new IllegalArgumentException("Academic level is required");
        }
        
        try {
            String offerUrl = waltidService.issueEuropeanStudentCardCredential(request);
            log.info("Successfully issued European Student Card for ESI: {}", request.getEsi());
            return offerUrl;
        } catch (Exception e) {
            log.error("Failed to issue European Student Card: {}", e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping(value = "/credentials", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Get wallet credentials for student",
        description = "Retrieves all credentials stored in the student's wallet. Requires student email or userName.",
        operationId = "getStudentWalletCredentials"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Successfully retrieved wallet credentials",
            content = @Content(
                schema = @Schema(implementation = List.class),
                examples = {
                    @ExampleObject(
                        name = "Credentials List",
                        summary = "List of credentials in wallet",
                        value = """
                        [
                          {
                            "id": "credential-123",
                            "type": ["VerifiableCredential", "EducationalID"],
                            "issuer": "did:example:issuer",
                            "issuanceDate": "2024-01-15T10:30:00Z"
                          }
                        ]
                        """
                    )
                }
            )
        )
    })
    public List<Map<String, Object>> getStudentWalletCredentials(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String studentId,
        @RequestParam(required = false) String userName
    ) throws ExternalServiceException {
        log.info("Received request to get wallet credentials - email: {}, studentId: {}, userName: {}", 
                 email, studentId, userName);
        
        // Build student data map from parameters
        Map<String, Object> studentData = new HashMap<>();
        if (email != null && !email.isEmpty()) {
            studentData.put("email", email);
        } else if (userName != null && !userName.isEmpty()) {
            // Generate email from username if not provided
            email = userName + "@students.example.edu";

            studentData.put("email", email);
        } else {
            throw new IllegalArgumentException("Either email or userName must be provided");
        }
        
        if (studentId != null && !studentId.isEmpty()) {
            studentData.put("studentId", studentId);
        } else if (userName != null && !userName.isEmpty()) {
            studentData.put("studentId", userName);
        }
        
        // Get credentials from wallet
        return studentWalletService.getWalletCredentials(studentData);
    }

    @PostMapping(value = "/present", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Handle presentation request",
        description = "Handles a verification request (openid4vp:// URL) by resolving it and matching credentials from the student's wallet.",
        operationId = "handlePresentationRequest"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Presentation request handled successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing required parameters"
        )
    })
    public Map<String, Object> handlePresentationRequest(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String studentId,
        @RequestParam(required = false) String userName,
        @RequestBody String presentationRequestUrl
    ) throws ExternalServiceException {
        log.info("Received presentation request - email: {}, studentId: {}, userName: {}", 
                 email, studentId, userName);
        
        // Build student data map from parameters
        Map<String, Object> studentData = new HashMap<>();
        if (email != null && !email.isEmpty()) {
            studentData.put("email", email);
        } else if (userName != null && !userName.isEmpty()) {
            email = userName + "@students.example.edu";
            studentData.put("email", email);
        } else {
            throw new IllegalArgumentException("Either email or userName must be provided");
        }
        
        if (studentId != null && !studentId.isEmpty()) {
            studentData.put("studentId", studentId);
        } else if (userName != null && !userName.isEmpty()) {
            studentData.put("studentId", userName);
        }
        
        // Handle presentation request
        return studentWalletService.handlePresentationRequest(presentationRequestUrl, studentData);
    }

    @PostMapping(value = "/match-presentation", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Match credentials for presentation request",
        description = "Matches credentials in the student's wallet against a presentation request without presenting them. Returns matched credentials with disclosure information for selective disclosure UI.",
        operationId = "matchCredentialsForPresentation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Credentials matched successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request - missing required parameters"
        )
    })
    public Map<String, Object> matchCredentialsForPresentation(
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String studentId,
        @RequestParam(required = false) String userName,
        @RequestBody String presentationRequestUrl
    ) throws ExternalServiceException {
        log.info("Received match credentials request - email: {}, studentId: {}, userName: {}", 
                 email, studentId, userName);
        
        // Build student data map from parameters
        Map<String, Object> studentData = new HashMap<>();
        if (email != null && !email.isEmpty()) {
            studentData.put("email", email);
        } else if (userName != null && !userName.isEmpty()) {
            email = userName + "@students.example.edu";
            studentData.put("email", email);
        } else {
            throw new IllegalArgumentException("Either email or userName must be provided");
        }
        
        if (studentId != null && !studentId.isEmpty()) {
            studentData.put("studentId", studentId);
        } else if (userName != null && !userName.isEmpty()) {
            studentData.put("studentId", userName);
        }
        
        // Match credentials for presentation
        return studentWalletService.matchCredentialsForPresentation(presentationRequestUrl, studentData);
    }
}
