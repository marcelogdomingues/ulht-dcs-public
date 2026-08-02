package com.example.dcs.credential.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.dcs.credential.config.CredentialTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GenericCredentialBuilder Tests")
class GenericCredentialBuilderTest {

    private GenericCredentialBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new GenericCredentialBuilder();
    }

    @Test
    @DisplayName("Should build basic credential with static fields")
    void testBuildBasicCredential() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        assertNotNull(credential);
        assertEquals("VerifiableCredential", ((List<?>) credential.get("type")).get(0));
        assertEquals("TestCredential", ((List<?>) credential.get("type")).get(1));
    }

    @Test
    @DisplayName("Should map fields with fallback values")
    void testFieldMappingWithFallback() {
        // Given
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        
        Map<String, List<String>> fieldMappings = new HashMap<>();
        fieldMappings.put("name", Arrays.asList("fullName", "firstName", "name"));
        template.setFieldMappings(fieldMappings);

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("firstName", "John");

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("John", credentialSubject.get("name"));
    }

    @Test
    @DisplayName("Should handle missing fields gracefully")
    void testMissingFields() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> emptyData = new HashMap<>();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, emptyData, "did:jwk:issuer", "did:jwk:subject"
        );

        // Then
        assertNotNull(credential);
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertNotNull(credentialSubject);
    }

    @Test
    @DisplayName("Should include static fields in credential")
    void testStaticFields() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> staticFields = new HashMap<>();
        staticFields.put("institutionName", "DCS");
        staticFields.put("country", "PT");
        template.setStaticFields(staticFields);

        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("DCS", credentialSubject.get("institutionName"));
        assertEquals("PT", credentialSubject.get("country"));
    }

    @Test
    @DisplayName("Should handle nested field mapping")
    void testNestedFieldMapping() {
        // Given
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        
        Map<String, List<String>> fieldMappings = new HashMap<>();
        fieldMappings.put("city", Arrays.asList("address.city", "location.city"));
        template.setFieldMappings(fieldMappings);

        Map<String, Object> studentData = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("city", "Lisbon");
        studentData.put("address", address);

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("Lisbon", credentialSubject.get("city"));
    }

    @Test
    @DisplayName("Should set issuer DID correctly")
    void testIssuerDID() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> studentData = createBasicStudentData();
        String issuerDid = "did:jwk:test-issuer-123";

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", issuerDid
        );

        // Then — issuer is a W3C VC object { "id": "<did>" }
        @SuppressWarnings("unchecked")
        Map<String, Object> issuer = (Map<String, Object>) credential.get("issuer");
        assertEquals(issuerDid, issuer.get("id"));
    }

    @Test
    @DisplayName("Should set subject DID correctly")
    void testSubjectDID() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> studentData = createBasicStudentData();
        String subjectDid = "did:jwk:test-subject-456";

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, subjectDid, "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals(subjectDid, credentialSubject.get("id"));
    }

    @Test
    @DisplayName("Should include correct credential types")
    void testCredentialTypes() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        template.setType("UniversityDegree");
        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        List<String> types = (List<String>) credential.get("type");
        assertTrue(types.contains("VerifiableCredential"));
        assertTrue(types.contains("UniversityDegree"));
    }

    @Test
    @DisplayName("Should handle additional contexts")
    void testAdditionalContexts() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        List<String> contexts = Arrays.asList(
                "https://www.w3.org/2018/credentials/v1",
                "https://europa.eu/2018/credentials/esi/v1"
        );
        template.setContexts(contexts);
        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        List<String> resultContexts = (List<String>) credential.get("@context");
        assertEquals(2, resultContexts.size());
        assertTrue(resultContexts.contains("https://www.w3.org/2018/credentials/v1"));
        assertTrue(resultContexts.contains("https://europa.eu/2018/credentials/esi/v1"));
    }

    @Test
    @DisplayName("Should handle empty student data")
    void testNullStudentData() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> emptyData = new HashMap<>();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, emptyData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        assertNotNull(credential);
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertNotNull(credentialSubject);
    }

    @Test
    @DisplayName("Should handle empty field mappings")
    void testEmptyFieldMappings() {
        // Given
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        template.setFieldMappings(new HashMap<>());
        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        assertNotNull(credential);
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertNotNull(credentialSubject);
        assertEquals("did:jwk:subject", credentialSubject.get("id"));
    }

    @Test
    @DisplayName("Should handle multiple field mapping options")
    void testMultipleFieldMappingOptions() {
        // Given
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        
        Map<String, List<String>> fieldMappings = new HashMap<>();
        fieldMappings.put("email", Arrays.asList("email", "studentEmail", "contactEmail"));
        template.setFieldMappings(fieldMappings);

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("studentEmail", "john@example.com");

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("john@example.com", credentialSubject.get("email"));
    }

    @Test
    @DisplayName("Should prioritize first available field in mapping")
    void testFieldMappingPriority() {
        // Given
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        
        Map<String, List<String>> fieldMappings = new HashMap<>();
        fieldMappings.put("studentId", Arrays.asList("studentId", "studentCode", "id"));
        template.setFieldMappings(fieldMappings);

        Map<String, Object> studentData = new HashMap<>();
        studentData.put("studentId", "primary-id");
        studentData.put("studentCode", "backup-code");

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("primary-id", credentialSubject.get("studentId"));
    }

    @Test
    @DisplayName("Should handle complex nested structures")
    void testComplexNestedStructures() {
        // Given
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        
        Map<String, List<String>> fieldMappings = new HashMap<>();
        fieldMappings.put("street", Arrays.asList("address.street"));
        fieldMappings.put("postalCode", Arrays.asList("address.postalCode"));
        template.setFieldMappings(fieldMappings);

        Map<String, Object> studentData = new HashMap<>();
        Map<String, Object> address = new HashMap<>();
        address.put("street", "Main Street 123");
        address.put("postalCode", "1000-001");
        studentData.put("address", address);

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertEquals("Main Street 123", credentialSubject.get("street"));
        assertEquals("1000-001", credentialSubject.get("postalCode"));
    }

    @Test
    @DisplayName("Should include issuer in credential")
    void testIssuerIncluded() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        assertNotNull(credential);
        assertNotNull(credential.get("issuer"));
        // issuer is a W3C VC object { "id": "<did>" }
        assertEquals("did:jwk:issuer", ((Map<?, ?>) credential.get("issuer")).get("id"));
    }

    @Test
    @DisplayName("Should include credentialSubject with ID")
    void testCredentialSubjectWithId() {
        // Given
        CredentialTemplate template = createBasicTemplate();
        Map<String, Object> studentData = createBasicStudentData();

        // When
        Map<String, Object> credential = builder.buildCredential(
                template, studentData, "did:jwk:subject", "did:jwk:issuer"
        );

        // Then
        assertNotNull(credential);
        Map<String, Object> credentialSubject = (Map<String, Object>) credential.get("credentialSubject");
        assertNotNull(credentialSubject);
        assertEquals("did:jwk:subject", credentialSubject.get("id"));
    }

    // Helper methods
    private CredentialTemplate createBasicTemplate() {
        CredentialTemplate template = new CredentialTemplate();
        template.setId("test-credential");
        template.setType("TestCredential");
        template.setWaltidConfigId("test_jwt_vc_json");
        
        Map<String, List<String>> fieldMappings = new HashMap<>();
        fieldMappings.put("studentId", Arrays.asList("studentId", "id"));
        fieldMappings.put("name", Arrays.asList("fullName", "name"));
        template.setFieldMappings(fieldMappings);
        
        return template;
    }

    private Map<String, Object> createBasicStudentData() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentId", "12345");
        data.put("fullName", "John Doe");
        data.put("email", "john@example.com");
        return data;
    }
}

