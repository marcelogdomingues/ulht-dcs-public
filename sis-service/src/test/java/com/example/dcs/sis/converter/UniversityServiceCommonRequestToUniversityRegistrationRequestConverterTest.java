package com.example.dcs.sis.converter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.dcs.sis.domain.avro.UniversityRegistrationRequest;
import com.example.dcs.sis.domain.avro.UniversityServiceCommonRequest;
import com.example.dcs.sis.domain.avro.LanguageCode;
import com.example.dcs.sis.domain.avro.PlatformType;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UniversityServiceCommonRequestToUniversityRegistrationRequestConverter Tests")
class UniversityServiceCommonRequestToUniversityRegistrationRequestConverterTest {

    private UniversityServiceCommonRequestToUniversityRegistrationRequestConverter converter;

    @BeforeEach
    void setUp() {
        converter = new UniversityServiceCommonRequestToUniversityRegistrationRequestConverter();
    }

    @Test
    @DisplayName("Should convert valid UniversityServiceCommonRequest")
    void testConvertValidRequest() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .setCorrelationId("corr_123")
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("DEFAULT_REG_PASSWORD", result.getPassword());
        assertEquals("com.example.dcs.mobile", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
        assertEquals(source.getRequestTimestamp(), result.getRequestTimestamp());
        assertEquals("corr_123", result.getCorrelationId());
    }

    @Test
    @DisplayName("Should convert request with null values")
    void testConvertRequestWithNullValues() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .setCorrelationId(null)
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("DEFAULT_REG_PASSWORD", result.getPassword());
        assertEquals("com.example.dcs.mobile", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
        assertNull(result.getCorrelationId());
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
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(language)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(language, result.getLanguage());
    }

    private void testPlatformConversion(PlatformType platform) {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(platform)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(platform, result.getPlatform());
    }

    @Test
    @DisplayName("Should convert request with minimal required fields")
    void testConvertRequestWithMinimalFields() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("req_123", result.getRequestId());
        assertEquals("a12345678", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("DEFAULT_REG_PASSWORD", result.getPassword());
        assertEquals("com.example.dcs.mobile", result.getApplication());
        assertEquals("1601206", result.getVersionCode());
        assertNull(result.getCorrelationId());
        assertNull(result.getInstallKey());
        assertNull(result.getDeviceInfo());
        assertNull(result.getRegistrationMetadata());
    }

    @Test
    @DisplayName("Should preserve request timestamp")
    void testPreserveRequestTimestamp() {
        // Given
        Instant timestamp = Instant.now().minusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MILLIS); // 1 hour ago, truncated to milliseconds
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(timestamp)
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals(timestamp, result.getRequestTimestamp());
    }

    @Test
    @DisplayName("Should handle empty strings")
    void testHandleEmptyStrings() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("")
                .setUserName("")
                .setInstallKey("")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("")
                .setVersionCode("")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("", result.getRequestId());
        assertEquals("", result.getUserName());
        assertEquals(LanguageCode.PT, result.getLanguage());
        assertEquals(PlatformType.ios, result.getPlatform());
        assertEquals("DEFAULT_REG_PASSWORD", result.getPassword());
        assertEquals("", result.getApplication());
        assertEquals("", result.getVersionCode());
    }

    @Test
    @DisplayName("Should set default password correctly")
    void testDefaultPassword() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("install_123")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        assertEquals("DEFAULT_REG_PASSWORD", result.getPassword());
    }

    @Test
    @DisplayName("Should handle empty installKey field")
    void testHandleEmptyInstallKey() {
        // Given
        UniversityServiceCommonRequest source = UniversityServiceCommonRequest.newBuilder()
                .setRequestId("req_123")
                .setUserName("a12345678")
                .setInstallKey("")
                .setLanguage(LanguageCode.PT)
                .setPlatform(PlatformType.ios)
                .setApplication("com.example.dcs.mobile")
                .setVersionCode("1601206")
                .setRequestTimestamp(Instant.now())
                .build();

        // When
        UniversityRegistrationRequest result = converter.convert(source);

        // Then
        assertNotNull(result);
        // installKey is not mapped back to registration request, so it remains null
        assertNull(result.getInstallKey());
    }
}