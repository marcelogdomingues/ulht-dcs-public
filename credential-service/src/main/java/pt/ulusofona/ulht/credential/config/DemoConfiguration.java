package pt.ulusofona.ulht.credential.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import pt.ulusofona.ulht.credential.client.WaltidIssuerClient;
import pt.ulusofona.ulht.credential.client.WaltidVerifierClient;
import pt.ulusofona.ulht.credential.client.WaltidWalletClient;
import pt.ulusofona.ulht.credential.domain.wallet.WalletAccountResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletDidResponse;
import pt.ulusofona.ulht.credential.domain.wallet.WalletKeysResponse;
import pt.ulusofona.ulht.credential.dto.waltid.IssuerKey;
import pt.ulusofona.ulht.credential.dto.waltid.JwkKey;
import pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerRequest;
import pt.ulusofona.ulht.credential.dto.waltid.OnboardIssuerResponse;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.PresentedCredentialsResponse;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.VerificationRequest;
import pt.ulusofona.ulht.credential.dto.waltid.verifier.VerificationStatusResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DEMO profile configuration for the credential-service.
 *
 * <p>When the {@code demo} Spring profile is active, this configuration provides
 * {@link Primary @Primary} in-memory implementations of the three walt.id Feign client
 * interfaces ({@link WaltidIssuerClient}, {@link WaltidWalletClient},
 * {@link WaltidVerifierClient}). This lets the full issue -> verify pipeline run end-to-end
 * with NO real walt.id services.</p>
 *
 * <p>The real Feign-generated client beans are left untouched; because these beans are
 * {@code @Primary} they win for injection whenever the {@code demo} profile is active. The
 * default and {@code docker} profiles are unaffected.</p>
 *
 * <p>The canned responses mirror exactly what the orchestration reads:</p>
 * <ul>
 *   <li><b>Issuer onboarding</b> returns a valid-looking {@code did:jwk:...} DID + JWK key.</li>
 *   <li><b>Credential issuance</b> returns an OID4VCI credential-offer URL as a plain-text body
 *       (the service uses the raw body string as the offer URL).</li>
 *   <li><b>Wallet login</b> returns a {@code Set-Cookie: login=...} header (the service parses
 *       the {@code login=} cookie value as the session cookie).</li>
 *   <li><b>Wallet accounts / DIDs</b> return one wallet and one subject DID.</li>
 *   <li><b>Verification</b> returns a Map with {@code url}/{@code state}/{@code presentationId}
 *       and a status whose {@code verificationResult == true}.</li>
 * </ul>
 *
 * <p>All values here are illustrative placeholders. Credentials produced in demo mode are NOT
 * real, verifiable, or cryptographically meaningful.</p>
 */
@Configuration
@Profile("demo")
public class DemoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DemoConfiguration.class);

    // Obviously-fake demo identifiers.
    private static final String DEMO_ISSUER_DID =
            "did:jwk:eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6ImRlbW8taXNzdWVyLXgiLCJ5IjoiZGVtby1pc3N1ZXIteSJ9";
    private static final String DEMO_SUBJECT_DID =
            "did:jwk:eyJrdHkiOiJFQyIsImNydiI6IlAtMjU2IiwieCI6ImRlbW8tc3ViamVjdC14IiwieSI6ImRlbW8tc3ViamVjdC15In0";
    private static final String DEMO_WALLET_ID = "00000000-0000-4000-8000-000000000001";
    private static final String DEMO_SESSION_COOKIE_VALUE = "demo-session-token";

    @Bean
    @Primary
    public WaltidIssuerClient demoWaltidIssuerClient() {
        log.warn("⚠️  DEMO profile active - using in-memory mock WaltidIssuerClient (no real walt.id issuer). "
                + "Issued credentials are illustrative and NOT verifiable.");
        return new DemoWaltidIssuerClient();
    }

    @Bean
    @Primary
    public WaltidWalletClient demoWaltidWalletClient() {
        log.warn("⚠️  DEMO profile active - using in-memory mock WaltidWalletClient (no real walt.id wallet).");
        return new DemoWaltidWalletClient();
    }

    @Bean
    @Primary
    public WaltidVerifierClient demoWaltidVerifierClient() {
        log.warn("⚠️  DEMO profile active - using in-memory mock WaltidVerifierClient (no real walt.id verifier).");
        return new DemoWaltidVerifierClient();
    }

    /** Builds an OID4VCI credential-offer URL that looks realistic but is entirely fake. */
    private static String buildDemoOfferUrl() {
        String offer = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"credential_issuer\":\"https://issuer.example.edu\",\"grants\":{\"urn:demo\":\""
                        + UUID.randomUUID() + "\"}}").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "openid-credential-offer://issuer.example.edu/?credential_offer=" + offer;
    }

    // -------------------------------------------------------------------------
    // Issuer
    // -------------------------------------------------------------------------
    static final class DemoWaltidIssuerClient implements WaltidIssuerClient {

        @Override
        public ResponseEntity<OnboardIssuerResponse> onboardIssuer(OnboardIssuerRequest request) {
            JwkKey jwk = JwkKey.builder()
                    .kty("EC")
                    .crv("P-256")
                    .kid("demo-issuer-key")
                    .x("demo-issuer-x")
                    .y("demo-issuer-y")
                    .d("demo-issuer-d")
                    .build();
            IssuerKey issuerKey = IssuerKey.builder().type("jwk").jwk(jwk).build();
            OnboardIssuerResponse response = OnboardIssuerResponse.builder()
                    .issuerKey(issuerKey)
                    .issuerDid(DEMO_ISSUER_DID)
                    .build();
            return ResponseEntity.ok(response);
        }

        @Override
        public ResponseEntity<String> issueJwtCredential(
                pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request,
                String statusCallbackUri) {
            return ResponseEntity.ok(buildDemoOfferUrl());
        }

        @Override
        public ResponseEntity<String> issueSdJwtCredential(
                pt.ulusofona.ulht.credential.dto.waltid.IssueCredentialRequest request,
                String statusCallbackUri) {
            return ResponseEntity.ok(buildDemoOfferUrl());
        }

        @Override
        public ResponseEntity<Map<String, Object>> getIssuerMetadata() {
            return ResponseEntity.ok(Map.of(
                    "credential_issuer", "https://issuer.example.edu",
                    "credential_endpoint", "https://issuer.example.edu/credential"));
        }

        @Override
        @SuppressWarnings("deprecation")
        public ResponseEntity<String> onboardUser(Map<String, Object> requestBody) {
            return ResponseEntity.ok("{\"issuerDid\":\"" + DEMO_ISSUER_DID + "\"}");
        }

        @Override
        @SuppressWarnings("deprecation")
        public ResponseEntity<String> issueCredential(Map<String, Object> requestBody) {
            return ResponseEntity.ok(buildDemoOfferUrl());
        }

        @Override
        @SuppressWarnings("deprecation")
        public ResponseEntity<String> getIssuerStatus(String userId) {
            return ResponseEntity.ok("{\"status\":\"OK\"}");
        }
    }

    // -------------------------------------------------------------------------
    // Wallet
    // -------------------------------------------------------------------------
    static final class DemoWaltidWalletClient implements WaltidWalletClient {

        private ResponseEntity<String> loginWithCookie() {
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.SET_COOKIE, "login=" + DEMO_SESSION_COOKIE_VALUE + "; Path=/; HttpOnly");
            return ResponseEntity.ok().headers(headers).body("{\"status\":\"SUCCESS\"}");
        }

        @Override
        public ResponseEntity<String> registerWallet(Map<String, Object> requestBody) {
            return ResponseEntity.ok("{\"status\":\"SUCCESS\"}");
        }

        @Override
        public ResponseEntity<String> loginWallet(Map<String, Object> requestBody) {
            return loginWithCookie();
        }

        @Override
        public ResponseEntity<WalletAccountResponse> getWalletAccounts(String cookie) {
            WalletAccountResponse.WalletAccount wallet = WalletAccountResponse.WalletAccount.builder()
                    .walletId(DEMO_WALLET_ID)
                    .name("Demo Wallet")
                    .createdOn("2025-01-01T00:00:00Z")
                    .permission("ADMIN")
                    .build();
            WalletAccountResponse response = WalletAccountResponse.builder()
                    .account("a12345@students.example.edu")
                    .wallets(List.of(wallet))
                    .build();
            return ResponseEntity.ok(response);
        }

        @Override
        public ResponseEntity<List<WalletKeysResponse.WalletKey>> getWalletKeys(String walletId, String cookie) {
            WalletKeysResponse.WalletKey key = WalletKeysResponse.WalletKey.builder()
                    .keyId("demo-subject-key")
                    .keyType("EC")
                    .algorithm("ES256")
                    .status("ACTIVE")
                    .build();
            return ResponseEntity.ok(List.of(key));
        }

        @Override
        public ResponseEntity<List<WalletDidResponse>> getWalletDids(String walletId, String cookie) {
            WalletDidResponse did = WalletDidResponse.builder()
                    .did(DEMO_SUBJECT_DID)
                    .method("jwk")
                    .alias("demo-subject")
                    .createdAt("2025-01-01T00:00:00Z")
                    .build();
            return ResponseEntity.ok(List.of(did));
        }

        @Override
        public ResponseEntity<List<Map<String, Object>>> getWalletCredentials(
                String walletId, String cookie, Boolean showDeleted, Boolean showPending) {
            // Empty list => credential-service will issue fresh credentials.
            return ResponseEntity.ok(List.of());
        }

        @Override
        public ResponseEntity<String> acceptCredentialOffer(
                String walletId, String did, String offerUrl, String cookie) {
            // Body is ignored by the service; only success matters.
            return ResponseEntity.ok("{\"status\":\"ACCEPTED\"}");
        }

        @Override
        public ResponseEntity<String> resolvePresentationRequest(
                String walletId, String presentationRequestUrl, String cookie) {
            return ResponseEntity.ok("{\"presentationDefinition\":{\"id\":\"demo\"},"
                    + "\"queryString\":\"" + presentationRequestUrl + "\"}");
        }

        @Override
        public ResponseEntity<List<Map<String, Object>>> matchCredentialsForPresentationDefinition(
                String walletId, Map<String, Object> presentationDefinition, String cookie) {
            return ResponseEntity.ok(List.of(Map.of("id", "demo-credential-1")));
        }

        @Override
        public ResponseEntity<Map<String, Object>> usePresentationRequest(
                String walletId, Map<String, Object> requestBody, String cookie) {
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "redirectUri", "https://verifier.example.edu/success"));
        }
    }

    // -------------------------------------------------------------------------
    // Verifier
    // -------------------------------------------------------------------------
    static final class DemoWaltidVerifierClient implements WaltidVerifierClient {

        @Override
        public ResponseEntity<Map<String, Object>> initiateVerification(
                String authorizeBaseUrl, String responseMode, String successRedirectUri,
                String errorRedirectUri, String statusCallbackUri, String statusCallbackApiKey,
                String stateId, String openId4VPProfile, VerificationRequest request) {
            String state = (stateId != null && !stateId.isEmpty()) ? stateId : "demo-state-" + UUID.randomUUID();
            String url = "openid4vp://authorize?response_type=vp_token&client_id=verifier.example.edu&state=" + state;
            // Map form => VerifierService reads url/presentationId/state directly.
            return ResponseEntity.ok(Map.of(
                    "url", url,
                    "presentationId", "demo-presentation-" + state,
                    "state", state));
        }

        @Override
        public ResponseEntity<VerificationStatusResponse> getVerificationStatus(String state) {
            // verificationResult == true => isVerificationSuccessful() returns true.
            VerificationStatusResponse status = VerificationStatusResponse.builder()
                    .id(state)
                    .verificationResult(Boolean.TRUE)
                    .build();
            return ResponseEntity.ok(status);
        }

        @Override
        public ResponseEntity<PresentedCredentialsResponse> getPresentedCredentials(String id, String viewMode) {
            PresentedCredentialsResponse response = PresentedCredentialsResponse.builder()
                    .viewMode(viewMode)
                    .credentialsByFormat(Map.of("jwt_vc_json", List.of(Map.of(
                            "type", List.of("VerifiableCredential", "DemoStudentCredential"),
                            "issuer", DEMO_ISSUER_DID))))
                    .build();
            return ResponseEntity.ok(response);
        }
    }
}
