package com.example.dcs.credential.dto.waltid;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for walt.id DTO serialization and validation
 */
class DtoValidationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testOnboardIssuerRequest_Serialization() throws Exception {
        // Given
        KeyConfig keyConfig = KeyConfig.builder()
                .backend("jwk")
                .keyType("secp256r1")
                .build();
        
        DidConfig didConfig = DidConfig.builder()
                .method("jwk")
                .build();
        
        OnboardIssuerRequest request = OnboardIssuerRequest.builder()
                .key(keyConfig)
                .did(didConfig)
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);
        OnboardIssuerRequest deserialized = objectMapper.readValue(json, OnboardIssuerRequest.class);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"backend\":\"jwk\""));
        assertTrue(json.contains("\"keyType\":\"secp256r1\""));
        assertTrue(json.contains("\"method\":\"jwk\""));
        
        assertEquals("jwk", deserialized.getKey().getBackend());
        assertEquals("secp256r1", deserialized.getKey().getKeyType());  // KeyConfig has keyType
        assertEquals("jwk", deserialized.getDid().getMethod());
    }

    @Test
    void testJwkKey_Serialization() throws Exception {
        // Given
        JwkKey jwkKey = JwkKey.builder()
                .kty("OKP")
                .crv("Ed25519")
                .kid("test-key-id")
                .x("public-x-data")
                .d("private-d-data")
                .build();

        // When
        String json = objectMapper.writeValueAsString(jwkKey);
        JwkKey deserialized = objectMapper.readValue(json, JwkKey.class);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"kty\":\"OKP\""));
        assertTrue(json.contains("\"crv\":\"Ed25519\""));
        assertTrue(json.contains("\"kid\":\"test-key-id\""));
        
        assertEquals("OKP", deserialized.getKty());
        assertEquals("Ed25519", deserialized.getCrv());
        assertEquals("test-key-id", deserialized.getKid());
    }

    @Test
    void testIssueCredentialRequest_Serialization() throws Exception {
        // Given
        JwkKey jwkKey = JwkKey.builder()
                .kty("EC")
                .crv("P-256")
                .kid("key-123")
                .build();
        
        IssuerKey issuerKey = IssuerKey.builder()
                .type("jwk")
                .jwk(jwkKey)
                .build();
        
        Map<String, Object> credentialData = new HashMap<>();
        credentialData.put("@context", Arrays.asList(
                "https://www.w3.org/2018/credentials/v1"
        ));
        credentialData.put("type", Arrays.asList("VerifiableCredential"));
        
        Map<String, Object> issuerMapping = new HashMap<>();
        issuerMapping.put("id", "<issuerDid>");
        
        Map<String, Object> subjectMapping = new HashMap<>();
        subjectMapping.put("id", "<subjectDid>");
        
        CredentialMapping mapping = CredentialMapping.builder()
                .id("<uuid>")
                .issuer(issuerMapping)
                .credentialSubject(subjectMapping)
                .issuanceDate("<timestamp>")
                .expirationDate("<timestamp-in:365d>")
                .build();
        
        IssueCredentialRequest request = IssueCredentialRequest.builder()
                .issuerKey(issuerKey)
                .issuerDid("did:jwk:test")
                .credentialConfigurationId("UniversityDegree_jwt_vc_json")
                .credentialData(credentialData)
                .mapping(mapping)
                .authenticationMethod("PRE_AUTHORIZED")
                .standardVersion("DRAFT13")
                .build();

        // When
        String json = objectMapper.writeValueAsString(request);
        IssueCredentialRequest deserialized = objectMapper.readValue(json, IssueCredentialRequest.class);

        // Then
        assertNotNull(json);
        assertTrue(json.contains("\"issuerDid\":\"did:jwk:test\""));
        assertTrue(json.contains("\"authenticationMethod\":\"PRE_AUTHORIZED\""));
        assertTrue(json.contains("\"standardVersion\":\"DRAFT13\""));
        
        assertEquals("did:jwk:test", deserialized.getIssuerDid());
        assertEquals("PRE_AUTHORIZED", deserialized.getAuthenticationMethod());
        assertEquals("DRAFT13", deserialized.getStandardVersion());
        assertEquals("UniversityDegree_jwt_vc_json", deserialized.getCredentialConfigurationId());
    }

    @Test
    void testUniversityDegreeData_AllFields() {
        // Given & When
        UniversityDegreeData degreeData = UniversityDegreeData.builder()
                .type("BachelorDegree")
                .name("Bachelor of Science")
                .field("Computer Science")
                .institution("DCS")
                .awardDate("2024-06-15")
                .grade("18.5")
                .build();

        // Then
        assertNotNull(degreeData);
        assertEquals("BachelorDegree", degreeData.getType());
        assertEquals("Bachelor of Science", degreeData.getName());
        assertEquals("Computer Science", degreeData.getField());
        assertEquals("DCS", degreeData.getInstitution());
        assertEquals("2024-06-15", degreeData.getAwardDate());
        assertEquals("18.5", degreeData.getGrade());
    }

    @Test
    void testCredentialMapping_Builder() {
        // Given
        Map<String, Object> issuerMap = new HashMap<>();
        issuerMap.put("id", "<issuerDid>");
        
        Map<String, Object> subjectMap = new HashMap<>();
        subjectMap.put("id", "<subjectDid>");

        // When
        CredentialMapping mapping = CredentialMapping.builder()
                .id("<uuid>")
                .issuer(issuerMap)
                .credentialSubject(subjectMap)
                .issuanceDate("<timestamp>")
                .expirationDate("<timestamp-in:180d>")
                .build();

        // Then
        assertNotNull(mapping);
        assertEquals("<uuid>", mapping.getId());
        assertEquals("<timestamp>", mapping.getIssuanceDate());
        assertEquals("<timestamp-in:180d>", mapping.getExpirationDate());
        assertEquals("<issuerDid>", mapping.getIssuer().get("id"));
        assertEquals("<subjectDid>", mapping.getCredentialSubject().get("id"));
    }

    @Test
    void testOnboardIssuerResponse_Deserialization() throws Exception {
        // Given
        String json = """
        {
          "issuerKey": {
            "type": "jwk",
            "jwk": {
              "kty": "OKP",
              "crv": "Ed25519",
              "kid": "test-key-id",
              "x": "test-x-value"
            }
          },
          "issuerDid": "did:jwk:test123"
        }
        """;

        // When
        OnboardIssuerResponse response = objectMapper.readValue(json, OnboardIssuerResponse.class);

        // Then
        assertNotNull(response);
        assertEquals("did:jwk:test123", response.getIssuerDid());
        assertNotNull(response.getIssuerKey());
        assertEquals("jwk", response.getIssuerKey().getType());
        assertEquals("OKP", response.getIssuerKey().getJwk().getKty());
        assertEquals("Ed25519", response.getIssuerKey().getJwk().getCrv());
    }

    @Test
    void testIssuerKey_WithKmsRef() throws Exception {
        // Given
        Map<String, Object> kmsRef = new HashMap<>();
        kmsRef.put("provider", "vault");
        kmsRef.put("keyId", "vault-key-123");
        
        IssuerKey issuerKey = IssuerKey.builder()
                .type("vault")
                .kmsRef(kmsRef)
                .build();

        // When
        String json = objectMapper.writeValueAsString(issuerKey);
        IssuerKey deserialized = objectMapper.readValue(json, IssuerKey.class);

        // Then
        assertEquals("vault", deserialized.getType());
        assertNull(deserialized.getJwk());
        assertNotNull(deserialized.getKmsRef());
    }
}

