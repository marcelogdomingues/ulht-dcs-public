package pt.ulusofona.ulht.credential.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import pt.ulusofona.ulht.credential.domain.wallet.WalletAccountResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletKeysResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletDidResponse;
import pt.ulusofona.ulht.credential.config.WaltidClientConfiguration;

import java.util.Map;
import java.util.List;

/**
 * WaltID Wallet Client - Feign client for external WaltID Wallet API.
 * 
 * This client connects to the external WaltID Wallet service (not a custom wallet).
 * Similar to how we use WaltID Issuer and WaltID Verifier, we use WaltID Wallet as an external service.
 * 
 * Authentication Methods Supported:
 * - Cookie-based (default/preferred): Session cookie from /auth/login in Set-Cookie header
 * - Bearer token (alternative): Token from /auth/login in JSON response body
 * 
 * @see <a href="https://docs.walt.id/v/stable/wallet/wallet-api">WaltID Wallet API Documentation</a>
 */
@FeignClient(
    name = "waltidWalletClient",
    url = "${waltid.wallet.url}",
    configuration = WaltidClientConfiguration.class
)
public interface WaltidWalletClient {

    @PostMapping(
        value = "/wallet-api/auth/register",
        consumes = "application/json", 
        produces = "application/json",
        headers = "Content-Type=application/json"
    )
    ResponseEntity<String> registerWallet(@RequestBody Map<String, Object> requestBody);

    @PostMapping(
        value = "/wallet-api/auth/login",
        consumes = "application/json", 
        produces = "application/json",
        headers = "Content-Type=application/json"
    )
    ResponseEntity<String> loginWallet(@RequestBody Map<String, Object> requestBody);

    @GetMapping(value = "/wallet-api/wallet/accounts/wallets")
    ResponseEntity<WalletAccountResponse> getWalletAccounts(
        @RequestHeader(value = "Cookie", required = false) String cookie
    );

    @GetMapping(value = "/wallet-api/wallet/{walletId}/keys")
    ResponseEntity<List<WalletKeysResponse.WalletKey>> getWalletKeys(
        @PathVariable("walletId") String walletId, 
        @RequestHeader(value = "Cookie", required = false) String cookie
    );

    @GetMapping(value = "/wallet-api/wallet/{walletId}/dids")
    ResponseEntity<List<WalletDidResponse>> getWalletDids(
        @PathVariable("walletId") String walletId,
        @RequestHeader(value = "Cookie", required = false) String cookie
    );

    /**
     * Lists all credentials stored in the wallet
     * 
     * @param walletId Wallet identifier
     * @param cookie Session cookie for authentication
     * @param showDeleted Whether to include deleted credentials (default: false)
     * @param showPending Whether to include pending credentials (default: false)
     * @return List of credentials stored in the wallet
     */
    @GetMapping(value = "/wallet-api/wallet/{walletId}/credentials")
    ResponseEntity<List<Map<String, Object>>> getWalletCredentials(
        @PathVariable("walletId") String walletId,
        @RequestHeader(value = "Cookie", required = false) String cookie,
        @RequestParam(value = "showDeleted", required = false, defaultValue = "false") Boolean showDeleted,
        @RequestParam(value = "showPending", required = false, defaultValue = "false") Boolean showPending
    );

    /**
     * Accepts a credential offer into the wallet
     * 
     * Note: Cookie header must be explicitly set when using @RequestBody.
     * Feign may not send headers marked as required=false when @RequestBody is present.
     * 
     * @param walletId Wallet identifier
     * @param did User's DID (Decentralized Identifier)
     * @param offerUrl Credential offer URL (OID4VCI format)
     * @param cookie Session cookie for authentication (required, must not be null)
     * @return Response indicating success
     */
    @PostMapping(
        value = "/wallet-api/wallet/{walletId}/exchange/useOfferRequest",
        consumes = "text/plain",
        produces = "application/json"
    )
    ResponseEntity<String> acceptCredentialOffer(
        @PathVariable("walletId") String walletId,
        @RequestParam("did") String did,
        @RequestBody String offerUrl,
        @RequestHeader("Cookie") String cookie
    );

    /**
     * Resolves a presentation request from a verification URL (OID4VP)
     * 
     * @param walletId Wallet identifier
     * @param presentationRequestUrl The openid4vp:// URL from the verifier (in request body as plain text)
     * @param cookie Session cookie for authentication
     * @return Response containing the URL with presentation_definition parameter added (as plain text, not JSON)
     */
    @PostMapping(
        value = "/wallet-api/wallet/{walletId}/exchange/resolvePresentationRequest",
        consumes = "text/plain",
        produces = "text/plain"
    )
    ResponseEntity<String> resolvePresentationRequest(
        @PathVariable("walletId") String walletId,
        @RequestBody String presentationRequestUrl,
        @RequestHeader("Cookie") String cookie
    );

    /**
     * Matches credentials in wallet against a presentation definition
     * 
     * @param walletId Wallet identifier
     * @param presentationDefinition The presentation definition (from resolvePresentationRequest)
     * @param cookie Session cookie for authentication
     * @return Response with matched credentials (array of credentials)
     */
    @PostMapping(
        value = "/wallet-api/wallet/{walletId}/exchange/matchCredentialsForPresentationDefinition",
        consumes = "application/json",
        produces = "application/json"
    )
    ResponseEntity<List<Map<String, Object>>> matchCredentialsForPresentationDefinition(
        @PathVariable("walletId") String walletId,
        @RequestBody Map<String, Object> presentationDefinition,
        @RequestHeader("Cookie") String cookie
    );

    /**
     * Uses a presentation request to present credentials
     * 
     * @param walletId Wallet identifier
     * @param requestBody Request body containing presentationRequest (URL), selectedCredentials (array), and disclosures (optional)
     * @param cookie Session cookie for authentication
     * @return Response indicating success
     */
    @PostMapping(
        value = "/wallet-api/wallet/{walletId}/exchange/usePresentationRequest",
        consumes = "application/json",
        produces = "application/json"
    )
    ResponseEntity<Map<String, Object>> usePresentationRequest(
        @PathVariable("walletId") String walletId,
        @RequestBody Map<String, Object> requestBody,
        @RequestHeader("Cookie") String cookie
    );
}
