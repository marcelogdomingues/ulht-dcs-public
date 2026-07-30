package pt.ulusofona.digital.wallet.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.ulusofona.digital.wallet.domain.avro.UniversityRegistrationRequest;
import pt.ulusofona.digital.wallet.domain.avro.UniversityServiceCommonRequest;
import pt.ulusofona.digital.wallet.domain.avro.LanguageCode;
import pt.ulusofona.digital.wallet.domain.avro.PlatformType;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UniversityRegistrationRequestToUniversityServiceCommonRequestConverter Tests")
class UniversityRegistrationRequestToUniversityServiceCommonRequestConverterTest {

    private UniversityRegistrationRequestToUniversityServiceCommonRequestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UniversityRegistrationRequestToUniversityServiceCommonRequestConverter();
    }

    @Test
    @DisplayName("Should convert valid UniversityRegistrationRequest")
    void testConvertValidRequest() {
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
                .setCorrelationId("corr_123")
                .setInstallKey("install_123")
                .build();

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
        assertEquals(source.getRequestTimestamp(), result.getRequestTimestamp());
        assertEquals("corr_123", result.getCorrelationId());
    }

    @Test
    @DisplayName("Should convert request with null values")
    void testConvertRequestWithNullValues() {
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
                .setCorrelationId(null)
                .setInstallKey(null)
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals("a12345678_AUTO_KEY", result.getInstallKey()); // Should generate auto key
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
        assertNull(result.getCorrelationId());
    }

    @Test
    @DisplayName("Should convert request with empty userName and generate default install key")
    void testConvertRequestWithEmptyUserName() {
        // Given
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setPassword("securePassword123")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .setInstallKey(null)
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("", result.getUserName());
        assertEquals("_AUTO_KEY", result.getInstallKey()); // Should use default key
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
    }

    @Test
    @DisplayName("Should convert request with all enum values")
    void testConvertRequestWithAllEnumValues() {
        // Test PT language
        testLanguageConversion(LanguageCode.PT);
        testLanguageConversion(LanguageCode.EN);
        testLanguageConversion(LanguageCode.ES);
        testLanguageConversion(LanguageCode.FR);

        // Test all platform types
        testPlatformConversion(PlatformType.ios);
        testPlatformConversion(PlatformType.android);
        testPlatformConversion(PlatformType.web);
        testPlatformConversion(PlatformType.desktop);
    }

    private void testLanguageConversion(LanguageCode language) {
        // Given
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setLanguage(language)
                .setPlatform(PlatformType.ios)
                .setPassword("securePassword123")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(language, result.getLanguage());
    }

    private void testPlatformConversion(PlatformType platform) {
        // Given
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setLanguage(LanguageCode.PT)
                .setPlatform(platform)
                .setPassword("securePassword123")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(platform, result.getPlatform());
    }

    @Test
    @DisplayName("Should convert request with minimal required fields")
    void testConvertRequestWithMinimalFields() {
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

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals("a12345678_AUTO_KEY", result.getInstallKey());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("org.cofac.mobile.ulht", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
        assertNull(result.getCorrelationId());
        assertNull(result.getClientInfo());
        assertNull(result.getRequestMetadata());
    }

    @Test
    @DisplayName("Should preserve request timestamp")
    void testPreserveRequestTimestamp() {
        // Given
        Instant timestamp = Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS); // 1 hour ago, truncated to milliseconds
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setPassword("securePassword123")
                .setApplication("org.cofac.mobile.ulht")
                .setVersionCode("1601206")
                .setRequestTimestamp(timestamp)
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(timestamp, result.getRequestTimestamp());
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testHandleEmptyStrings() {
        // Given
        UniversityRegistrationRequest source = UniversityRegistrationRequest.newBuilder()
                .setRequestId("")
                .setUserName("")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setPassword("")
                .setApplication("")
                .setVersionCode("")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityServiceCommonRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("", result.getRequestId());
        assertEquals("", result.getUserName());
        assertEquals("_AUTO_KEY", result.getInstallKey());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("", result.getApplication());
        assertEquals("", result.getVersionCode());
    }
}