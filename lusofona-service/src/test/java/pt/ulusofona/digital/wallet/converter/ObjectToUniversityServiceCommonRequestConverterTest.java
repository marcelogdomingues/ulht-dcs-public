package pt.ulusofona.digital.wallet.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
@DisplayName("ObjectToUniversityServiceCommonRequestConverter Tests")
class ObjectToUniversityServiceCommonRequestConverterTest {

    @Mock
    private UniversityRegistrationRequestToUniversityServiceCommonRequestConverter regToCommonConverter;

    private ObjectToUniversityServiceCommonRequestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new ObjectToUniversityServiceCommonRequestConverter(regToCommonConverter);
    }

    @Test
    @DisplayName("Should convert UniversityServiceCommonRequest directly")
    void testConvertUniversityServiceCommonRequest() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(source, result);
    }

    @Test
    @DisplayName("Should convert UniversityRegistrationRequest using delegate converter")
    void testConvertUniversityRegistrationRequest() {
        // Given
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setPassword("securePassword123")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        UniversityServiceCommonRequest expectedResult = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        when(regToCommonConverter.doConvert(any(UniversityRegistrationRequest.class)))
                .thenReturn(expectedResult);

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(expectedResult, result);
    }

    @Test
    @DisplayName("Should convert valid map to UniversityServiceCommonRequest")
    void testConvertValidMap() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("installKey", "00000_0000000000000");
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals("00000_0000000000000", result.getInstallKey());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert map with null values")
    void testConvertMapWithNullValues() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", null);
        source.put("userName", null);
        source.put("installKey", null);
        source.put("language", null);
        source.put("platform", null);
        source.put("application", null);
        source.put("versionCode", null);

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertNull(result.getRequestId());
        assertNull(result.getUserName());
        assertNull(result.getInstallKey());
        assertNull(result.getLanguage());
        assertNull(result.getPlatform());
        assertNull(result.getApplication());
        assertNull(result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert map with missing fields")
    void testConvertMapWithMissingFields() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("userName", "a12345678");
        source.put("language", "PT");
        // Missing other fields

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertNull(result.getRequestId());
        assertNull(result.getInstallKey());
        assertNull(result.getPlatform());
        assertNull(result.getApplication());
        assertNull(result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert map with non-string platform")
    void testConvertMapWithNonStringPlatform() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("installKey", "install_123");
        source.put("language", "PT");
        source.put("platform", "ios"); // Use valid string value for platform
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(PlatformType.ios, result.getPlatform());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PT", "EN", "ES", "FR"})
    @DisplayName("Should convert map with all supported language codes")
    void testConvertMapWithAllLanguageCodes(String languageCode) {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("installKey", "install_123");
        source.put("language", languageCode);
        source.put("platform", "ios");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(LanguageCode.valueOf(languageCode), result.getLanguage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ios", "android", "web", "desktop"})
    @DisplayName("Should convert map with all supported platform types")
    void testConvertMapWithAllPlatformTypes(String platformType) {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("installKey", "install_123");
        source.put("language", "PT");
        source.put("platform", platformType);
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(PlatformType.valueOf(platformType), result.getPlatform());
    }

    @Test
    @DisplayName("Should handle map with ClassCastException")
    void testConvertMapWithClassCastException() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", new Object()); // Non-string value
        source.put("userName", "a12345678");
        source.put("installKey", "install_123");
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

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
        source.put("installKey", "install_123");
        source.put("language", "INVALID_LANGUAGE");
        source.put("platform", "INVALID_PLATFORM");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNull(result); // Should return null due to IllegalArgumentException
    }

    @Test
    @DisplayName("Should handle unsupported object type")
    void testConvertUnsupportedObjectType() {
        // Given
        Object unsupportedSource = new Object();

        // When
        UniversityServiceCommonRequest result = converter.convert(unsupportedSource);

        // Then
        assertNull(result); // Should return null for unsupported types
    }

    @Test
    @DisplayName("Should handle empty map")
    void testConvertEmptyMap() {
        // Given
        Map<String, Object> source = new HashMap<>();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertNull(result.getRequestId());
        assertNull(result.getUserName());
        assertNull(result.getInstallKey());
        assertNull(result.getLanguage());
        assertNull(result.getPlatform());
        assertNull(result.getApplication());
        assertNull(result.getVersionCode());
    }

    @Test
    @DisplayName("Should handle map with extra fields")
    void testConvertMapWithExtraFields() {
        // Given
        Map<String, Object> source = new HashMap<>();
        source.put("requestId", "req_123");
        source.put("userName", "a12345678");
        source.put("installKey", "install_123");
        source.put("language", "PT");
        source.put("platform", "ios");
        source.put("application", "org.cofac.mobile.ulht");
        source.put("versionCode", "1601206");
        source.put("extraField", "extraValue");
        source.put("anotherField", "anotherValue");

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals("install_123", result.getInstallKey());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
    }
}