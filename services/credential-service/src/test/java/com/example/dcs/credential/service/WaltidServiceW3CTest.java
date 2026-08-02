package com.example.dcs.credential.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.example.dcs.credential.client.WaltidIssuerClient;
import com.example.dcs.credential.client.WaltidWalletClient;
import com.example.dcs.credential.dto.UniversityDegreeCredentialRequest;
import com.example.dcs.credential.dto.waltid.*;
import com.example.dcs.credential.exception.ExternalServiceException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WaltidService W3C Verifiable Credentials functionality
 */
@ExtendWith(MockitoExtension.class)
class WaltidServiceW3CTest {

    @Mock
    private WaltidIssuerClient waltidIssuerClient;

    @Mock
    private WaltidWalletClient waltidWalletClient;

    @InjectMocks
    private WaltidService waltidService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testOnboardIssuer_Success() throws ExternalServiceException {
        // Given
        String keyType = "secp256r1";
        String didMethod = "jwk";
        
        JwkKey jwkKey = JwkKey.builder()
                .kty("EC")
                .crv("P-256")
                .kid("test-key-id")
                .x("test-x")
                .build();
        
        IssuerKey issuerKey = IssuerKey.builder()
                .type("jwk")
                .jwk(jwkKey)
                .build();
        
        OnboardIssuerResponse expectedResponse = OnboardIssuerResponse.builder()
                .issuerKey(issuerKey)
                .issuerDid("did:jwk:test123")
                .build();
        
        when(waltidIssuerClient.onboardIssuer(any(OnboardIssuerRequest.class)))
                .thenReturn(ResponseEntity.ok(expectedResponse));

        // When
        OnboardIssuerResponse response = waltidService.onboardIssuer(keyType, didMethod);

        // Then
        assertNotNull(response);
        assertEquals("did:jwk:test123", response.getIssuerDid());
        assertNotNull(response.getIssuerKey());
        assertEquals("jwk", response.getIssuerKey().getType());
        
        verify(waltidIssuerClient, times(1)).onboardIssuer(any(OnboardIssuerRequest.class));
    }

    @Test
    void testOnboardIssuer_Failure() {
        // Given
        when(waltidIssuerClient.onboardIssuer(any(OnboardIssuerRequest.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        // When & Then
        assertThrows(ExternalServiceException.class, () -> {
            waltidService.onboardIssuer("ed25519", "key");
        });
    }

    @Test
    void testIssueJwtCredential_Success() throws ExternalServiceException {
        // Given
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("type", "UniversityDegree");
        
        IssueCredentialRequest request = IssueCredentialRequest.builder()
                .issuerDid("did:jwk:issuer123")
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")
                .credentialData(credentialData)
                .build();
        
        String expectedOfferUrl = "openid-credential-offer://issuer.portal.walt.id/?credential_offer=...";
        
        when(waltidIssuerClient.issueJwtCredential(any(IssueCredentialRequest.class), eq(null)))
                .thenReturn(ResponseEntity.ok(expectedOfferUrl));

        // When
        String offerUrl = waltidService.issueJwtCredential(request);

        // Then
        assertNotNull(offerUrl);
        assertEquals(expectedOfferUrl, offerUrl);
        assertTrue(offerUrl.startsWith("openid-credential-offer://"));
        
        verify(waltidIssuerClient, times(1))
                .issueJwtCredential(any(IssueCredentialRequest.class), eq(null));
    }

    @Test
    void testIssueJwtCredential_WithCallback() throws ExternalServiceException {
        // Given
        IssueCredentialRequest request = IssueCredentialRequest.builder()
                .issuerDid("did:jwk:issuer123")
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")
                .credentialData(new HashMap<>())
                .build();
        
        String callbackUri = "https://example.com/callback/123";
        String expectedOfferUrl = "openid-credential-offer://...";
        
        when(waltidIssuerClient.issueJwtCredential(any(IssueCredentialRequest.class), eq(callbackUri)))
                .thenReturn(ResponseEntity.ok(expectedOfferUrl));

        // When
        String offerUrl = waltidService.issueJwtCredential(request, callbackUri);

        // Then
        assertNotNull(offerUrl);
        verify(waltidIssuerClient, times(1))
                .issueJwtCredential(any(IssueCredentialRequest.class), eq(callbackUri));
    }

    @Test
    void testIssueSdJwtCredential_Success() throws ExternalServiceException {
        // Given
        IssueCredentialRequest request = IssueCredentialRequest.builder()
                .issuerDid("did:jwk:issuer123")
                .credentialConfigurationId("UniversityDegree_vc+sd-jwt")
                .credentialData(new HashMap<>())
                .build();
        
        String expectedOfferUrl = "openid-credential-offer://...sdjwt...";
        
        when(waltidIssuerClient.issueSdJwtCredential(any(IssueCredentialRequest.class), eq(null)))
                .thenReturn(ResponseEntity.ok(expectedOfferUrl));

        // When
        String offerUrl = waltidService.issueSdJwtCredential(request);

        // Then
        assertNotNull(offerUrl);
        assertEquals(expectedOfferUrl, offerUrl);
        verify(waltidIssuerClient, times(1))
                .issueSdJwtCredential(any(IssueCredentialRequest.class), eq(null));
    }

    @Test
    void testIssueUniversityDegreeCredential_Success() throws ExternalServiceException {
        // Given
        Map<String, Object> issuerKeyMap = new HashMap<>();
        issuerKeyMap.put("type", "jwk");
        Map<String, Object> jwkMap = new HashMap<>();
        jwkMap.put("kty", "EC");
        jwkMap.put("crv", "P-256");
        jwkMap.put("x", "test-x");
        issuerKeyMap.put("jwk", jwkMap);
        
        UniversityDegreeCredentialRequest request = UniversityDegreeCredentialRequest.builder()
                .subjectDid("did:jwk:student123")
                .issuerDid("did:jwk:issuer456")
                .issuerKey(issuerKeyMap)
                .degreeName("Bachelor of Science and Arts")
                .degreeType("BachelorDegree")
                .field("Computer Science")
                .universityName("DCS")
                .graduationDate("2024-06-15")
                .grade("18.5")
                .build();
        
        String expectedOfferUrl = "openid-credential-offer://...degree...";
        
        when(waltidIssuerClient.issueJwtCredential(any(IssueCredentialRequest.class), eq(null)))
                .thenReturn(ResponseEntity.ok(expectedOfferUrl));

        // When
        String offerUrl = waltidService.issueUniversityDegreeCredential(request);

        // Then
        assertNotNull(offerUrl);
        assertEquals(expectedOfferUrl, offerUrl);
        verify(waltidIssuerClient, times(1))
                .issueJwtCredential(any(IssueCredentialRequest.class), eq(null));
    }

    @Test
    void testIssueUniversityDegreeCredential_WithMinimalData() throws ExternalServiceException {
        // Given
        Map<String, Object> issuerKeyMap = new HashMap<>();
        issuerKeyMap.put("type", "jwk");
        
        UniversityDegreeCredentialRequest request = UniversityDegreeCredentialRequest.builder()
                .subjectDid("did:jwk:student123")
                .issuerDid("did:jwk:issuer456")
                .issuerKey(issuerKeyMap)
                .build();
        
        String expectedOfferUrl = "openid-credential-offer://...";
        
        when(waltidIssuerClient.issueJwtCredential(any(IssueCredentialRequest.class), eq(null)))
                .thenReturn(ResponseEntity.ok(expectedOfferUrl));

        // When
        String offerUrl = waltidService.issueUniversityDegreeCredential(request);

        // Then
        assertNotNull(offerUrl);
        verify(waltidIssuerClient, times(1))
                .issueJwtCredential(any(IssueCredentialRequest.class), eq(null));
    }

    @Test
    void testGetIssuerMetadata_Success() throws ExternalServiceException {
        // Given
        Map<String, Object> expectedMetadata = new HashMap<>();
        expectedMetadata.put("credential_issuer", "https://issuer.portal.walt.id");
        expectedMetadata.put("credential_endpoint", "https://issuer.portal.walt.id/credential");
        
        when(waltidIssuerClient.getIssuerMetadata())
                .thenReturn(ResponseEntity.ok(expectedMetadata));

        // When
        Map<String, Object> metadata = waltidService.getIssuerMetadata();

        // Then
        assertNotNull(metadata);
        assertEquals("https://issuer.portal.walt.id", metadata.get("credential_issuer"));
        assertTrue(metadata.containsKey("credential_endpoint"));
        verify(waltidIssuerClient, times(1)).getIssuerMetadata();
    }

    @Test
    void testIssueCredential_ExternalServiceFailure() {
        // Given
        IssueCredentialRequest request = IssueCredentialRequest.builder()
                .issuerDid("did:jwk:issuer123")
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")
                .credentialData(new HashMap<>())
                .build();
        
        when(waltidIssuerClient.issueJwtCredential(any(IssueCredentialRequest.class), eq(null)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        assertThrows(ExternalServiceException.class, () -> {
            waltidService.issueJwtCredential(request);
        });
    }

    @Test
    void testConvertIssuerKey_FromMap() throws ExternalServiceException {
        // This tests the private method indirectly through issueUniversityDegreeCredential
        
        // Given
        Map<String, Object> issuerKeyMap = new HashMap<>();
        issuerKeyMap.put("type", "jwk");
        Map<String, Object> jwkMap = new HashMap<>();
        jwkMap.put("kty", "OKP");
        jwkMap.put("crv", "Ed25519");
        jwkMap.put("d", "private-key-data");
        jwkMap.put("x", "public-key-data");
        jwkMap.put("kid", "key-id-123");
        issuerKeyMap.put("jwk", jwkMap);
        
        UniversityDegreeCredentialRequest request = UniversityDegreeCredentialRequest.builder()
                .subjectDid("did:jwk:student")
                .issuerDid("did:jwk:issuer")
                .issuerKey(issuerKeyMap)
                .build();
        
        when(waltidIssuerClient.issueJwtCredential(any(IssueCredentialRequest.class), eq(null)))
                .thenReturn(ResponseEntity.ok("openid-credential-offer://..."));

        // When
        String result = waltidService.issueUniversityDegreeCredential(request);

        // Then
        assertNotNull(result);
        verify(waltidIssuerClient, times(1))
                .issueJwtCredential(any(IssueCredentialRequest.class), eq(null));
    }
}

