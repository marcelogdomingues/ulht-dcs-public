package pt.ulusofona.digital.wallet.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ulusofona.digital.wallet.domain.avro.UniversityRegistrationRequest;
import pt.ulusofona.digital.wallet.domain.avro.UniversityServiceCommonRequest;
import pt.ulusofona.digital.wallet.domain.avro.LanguageCode;
import pt.ulusofona.digital.wallet.domain.avro.PlatformType;
import java.time.Instant;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ObjectToUniversityRegistrationRequestConverter Tests")
class ObjectToUniversityRegistrationRequestConverterTest {

    @Mock
    private UniversityServiceCommonRequestToUniversityRegistrationRequestConverter commonToRegConverter;

    private ObjectToUniversityRegistrationRequestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ObjectToUniversityRegistrationRequestConverter(commonToRegConverter);
    }

    @Test
    @DisplayName("Should convert UniversityRegistrationRequest directly")
    void testConvertUniversityRegistrationRequest() {
        // Given
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setUserName("a12345678")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setPassword("securePassword123")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestId("req_123")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("securePassword123", result.getPassword());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert UniversityServiceCommonRequest using delegate converter")
    void testConvertUniversityServiceCommonRequest() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestId("req_123")
                .setRequestTimestamp(Instant.now())
                .build();

        UniversityRegistrationRequest expectedResult = UniversityRegistrationRequest.newBuilder()
                .setUserName("a12345678")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setPassword("defaultPassword")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestId("req_123")
                .setRequestTimestamp(Instant.now())
                .build();

        when(commonToRegConverter.doConvert(any(UniversityServiceCommonRequest.class)))
                .thenReturn(expectedResult);

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("Should convert valid Map to UniversityRegistrationRequest")
    void testConvertValidMap() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("password", "securePassword123");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("securePassword123", result.getPassword());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert Map with null values")
    void testConvertMapWithNullValues() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", null);
        source.put("userName", null);
        source.put("language", null);
        source.put("platform", null);
        source.put("password", null);
        source.put("application", null);
        source.put("versionCode", null);

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertNull(result.getRequestId());
        assertNull(result.getUserName());
        assertNull(result.getLanguage());
        assertNull(result.getPlatform());
        assertNull(result.getPassword());
        assertNull(result.getApplication());
        assertNull(result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert Map with missing fields")
    void testConvertMapWithMissingFields() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("userName", "a12345678");
        source.put("language", "PT");
        // Missing other fields

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertNull(result.getPlatform());
        assertNull(result.getPassword());
        assertNull(result.getApplication());
        assertNull(result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert Map with non-string versionCode")
    void testConvertMapWithNonStringVersionCode() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("password", "securePassword123");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", 1601206); // Integer instead of String

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("securePassword123", result.getPassword());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode()); // Should be converted to String
    }

    @Test
    @DisplayName("Should handle Map with ClassCastException")
    void testConvertMapWithClassCastException() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", new Object()); // Non-string value
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("password", "securePassword123");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNull(result); // Should return null due to ClassCastException
    }

    @Test
    @DisplayName("Should handle invalid enum values")
    void testConvertMapWithInvalidEnumValues() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("language", "INVALID_LANGUAGE");
        source.put("platform", "INVALID_PLATFORM");
        source.put("password", "securePassword123");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNull(result); // Should return null due to IllegalArgumentException
    }

    @Test
    @DisplayName("Should handle unsupported object type")
    void testConvertUnsupportedObjectType() {
        // Given
        Object unsupportedSource = new Object();

        // When
        UniversityRegistrationRequest result = converter.convert(unsupportedSource);

        // Then
        assertNull(result); // Should return null for unsupported types
    }

    @Test
    @DisplayName("Should handle empty Map")
    void testConvertEmptyMap() {
        // Given
        Map<String, Object> source = new HashMap<>();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertNull(result.getRequestId());
        assertNull(result.getUserName());
        assertNull(result.getLanguage());
        assertNull(result.getPlatform());
        assertNull(result.getPassword());
        assertNull(result.getApplication());
        assertNull(result.getVersionCode());
    }

    @Test
    @DisplayName("Should handle Map with extra fields")
    void testConvertMapWithExtraFields() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("password", "securePassword123");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");
        source.put("extraField", "extraValue");
        source.put("anotherField", "anotherValue");

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("securePassword123", result.getPassword());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
    }
}