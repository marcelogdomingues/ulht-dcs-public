package com.example.dcs.credential.builder;

import org.junit.jupiter.api.Test;
import com.example.dcs.credential.dto.waltid.CredentialMapping;
import com.example.dcs.credential.dto.waltid.UniversityDegreeData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CredentialDataBuilder
 */
class CredentialDataBuilderTest {

    @Test
    void testBuildUniversityDegreeCredential() {
        // Given
        String credentialId = "http://example.edu/credentials/123";
        String issuerDid = "did:jwk:testissuer123";
        String subjectDid = "did:jwk:teststudent456";
        
        UniversityDegreeData degreeData = UniversityDegreeData.builder()
                .type("BachelorDegree")
                .name("Bachelor of Science and Arts")
                .field("Computer Science")
                .institution("DCS")
                .awardDate("2024-06-15")
                .grade("18.5")
                .build();

        // When
        Map<String, Object> credential = CredentialDataBuilder.buildUniversityDegreeCredential(
                credentialId, issuerDid, subjectDid, degreeData);

        // Then
        assertNotNull(credential);
        assertEquals(credentialId, credential.get("id"));
        
        // Check context
        @SuppressWarnings("unchecked")
        List<String> context = (List<String>) credential.get("@context");
        assertTrue(context.contains("https://www.w3.org/2018/credentials/v1"));
        assertTrue(context.contains("https://www.w3.org/2018/credentials/examples/v1"));
        
        // Check type
        @SuppressWarnings("unchecked")
        List<String> types = (List<String>) credential.get("type");
        assertTrue(types.contains("VerifiableCredential"));
        assertTrue(types.contains("UniversityDegree"));
        
        // Check issuer
        @SuppressWarnings("unchecked")
        Map<String, Object> issuer = (Map<String, Object>) credential.get("issuer");
        assertEquals(issuerDid, issuer.get("id"));
        
        // Check credential subject
        @SuppressWarnings("unchecked")
        Map<String, Object> subject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals(subjectDid, subject.get("id"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> degree = (Map<String, Object>) subject.get("degree");
        assertEquals("BachelorDegree", degree.get("type"));
        assertEquals("Bachelor of Science and Arts", degree.get("name"));
        assertEquals("Computer Science", degree.get("field"));
        assertEquals("DCS", degree.get("institution"));
        assertEquals("2024-06-15", degree.get("awardDate"));
        assertEquals("18.5", degree.get("grade"));
    }

    @Test
    void testBuildUniversityDegreeCredentialWithoutOptionalFields() {
        // Given
        UniversityDegreeData degreeData = UniversityDegreeData.builder()
                .type("MasterDegree")
                .name("Master of Science")
                .build();

        // When
        Map<String, Object> credential = CredentialDataBuilder.buildUniversityDegreeCredential(
                null, "did:jwk:issuer", "did:jwk:subject", degreeData);

        // Then
        assertNotNull(credential);
        assertNull(credential.get("id")); // No ID provided
        
        @SuppressWarnings("unchecked")
        Map<String, Object> subject = (Map<String, Object>) credential.get("credentialSubject");
        @SuppressWarnings("unchecked")
        Map<String, Object> degree = (Map<String, Object>) subject.get("degree");
        assertEquals("MasterDegree", degree.get("type"));
        assertEquals("Master of Science", degree.get("name"));
        assertFalse(degree.containsKey("field"));
        assertFalse(degree.containsKey("institution"));
    }

    @Test
    void testBuildDefaultMapping() {
        // When
        CredentialMapping mapping = CredentialDataBuilder.buildDefaultMapping();

        // Then
        assertNotNull(mapping);
        assertEquals("<uuid>", mapping.getId());
        assertEquals("<timestamp>", mapping.getIssuanceDate());
        assertEquals("<timestamp-in:365d>", mapping.getExpirationDate());
        
        assertNotNull(mapping.getIssuer());
        assertEquals("<issuerDid>", mapping.getIssuer().get("id"));
        
        assertNotNull(mapping.getCredentialSubject());
        assertEquals("<subjectDid>", mapping.getCredentialSubject().get("id"));
    }

    @Test
    void testBuildCustomMapping() {
        // When
        CredentialMapping mapping = CredentialDataBuilder.buildMapping(
                "<uuid>", "<timestamp>", "<timestamp-in:180d>");

        // Then
        assertNotNull(mapping);
        assertEquals("<uuid>", mapping.getId());
        assertEquals("<timestamp>", mapping.getIssuanceDate());
        assertEquals("<timestamp-in:180d>", mapping.getExpirationDate());
    }

    @Test
    void testBuildGenericCredential() {
        // Given
        List<String> contexts = Arrays.asList(
                "https://www.w3.org/2018/credentials/v1",
                "https://example.com/custom/v1"
        );
        List<String> types = Arrays.asList("CustomCredential");
        Map<String, Object> claims = new HashMap<>();
        claims.put("customClaim", "customValue");
        claims.put("anotherClaim", 12345);

        // When
        Map<String, Object> credential = CredentialDataBuilder.buildGenericCredential(
                contexts, "http://example.com/cred/1", types,
                "did:jwk:issuer", "did:jwk:subject", claims);

        // Then
        assertNotNull(credential);
        assertEquals("http://example.com/cred/1", credential.get("id"));
        
        @SuppressWarnings("unchecked")
        List<String> credContexts = (List<String>) credential.get("@context");
        assertEquals(2, credContexts.size());
        assertTrue(credContexts.contains("https://example.com/custom/v1"));
        
        @SuppressWarnings("unchecked")
        List<String> credTypes = (List<String>) credential.get("type");
        assertTrue(credTypes.contains("VerifiableCredential"));
        assertTrue(credTypes.contains("CustomCredential"));
        
        @SuppressWarnings("unchecked")
        Map<String, Object> subject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("did:jwk:subject", subject.get("id"));
        assertEquals("customValue", subject.get("customClaim"));
        assertEquals(12345, subject.get("anotherClaim"));
    }

    @Test
    void testBuildGenericCredentialWithDefaults() {
        // When
        Map<String, Object> credential = CredentialDataBuilder.buildGenericCredential(
                null, null, null, "did:jwk:issuer", "did:jwk:subject", null);

        // Then
        assertNotNull(credential);
        assertNull(credential.get("id"));
        
        @SuppressWarnings("unchecked")
        List<String> contexts = (List<String>) credential.get("@context");
        assertEquals(1, contexts.size());
        assertEquals("https://www.w3.org/2018/credentials/v1", contexts.get(0));
        
        @SuppressWarnings("unchecked")
        List<String> types = (List<String>) credential.get("type");
        assertEquals(1, types.size());
        assertEquals("VerifiableCredential", types.get(0));
    }
}

