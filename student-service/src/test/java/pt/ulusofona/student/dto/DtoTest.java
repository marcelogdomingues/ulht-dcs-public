package pt.ulusofona.student.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests")
class DtoTest {

    @Test
    @DisplayName("Should create LoginRequest with all fields")
    void testLoginRequestAllFields() {
        LoginRequest request = new LoginRequest(
                "a12345678",
                "00000_0000000000000",
                "ios",
                "PT",
                "org.cofac.mobile.ulht",
                "1601206"
        );

        assertEquals("a12345678", request.userName);
        assertEquals("00000_0000000000000", request.installKey);
        assertEquals("ios", request.platform);
        assertEquals("PT", request.language);
        assertEquals("org.cofac.mobile.ulht", request.application);
        assertEquals("1601206", request.versionCode);
    }

    @Test
    @DisplayName("Should create LoginRequest with defaults")
    void testLoginRequestDefaults() {
        LoginRequest request = new LoginRequest();
        request.userName = "test";
        request.installKey = "test-key";

        assertEquals("test", request.userName);
        assertEquals("test-key", request.installKey);
        assertEquals("ios", request.platform); // Default value
        assertEquals("PT", request.language); // Default value
    }

    @Test
    @DisplayName("Should create empty LoginRequest")
    void testLoginRequestEmpty() {
        LoginRequest request = new LoginRequest();
        assertNull(request.userName);
        assertNull(request.installKey);
        assertNotNull(request.platform); // Has default
        assertNotNull(request.language); // Has default
    }

    @Test
    @DisplayName("Should set LoginRequest fields individually")
    void testLoginRequestIndividualFields() {
        LoginRequest request = new LoginRequest();
        request.userName = "test-user";
        request.installKey = "test-key";
        request.platform = "android";
        request.language = "EN";

        assertEquals("test-user", request.userName);
        assertEquals("test-key", request.installKey);
        assertEquals("android", request.platform);
        assertEquals("EN", request.language);
    }

    @Test
    @DisplayName("Should create LoginRequest with partial constructor")
    void testLoginRequestPartialConstructor() {
        LoginRequest request = new LoginRequest(
                "user1",
                "key1",
                null,
                null,
                null,
                null
        );

        assertEquals("user1", request.userName);
        assertEquals("key1", request.installKey);
    }
}

