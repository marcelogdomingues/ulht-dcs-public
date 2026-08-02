package com.example.dcs.sis.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCodes Tests")
class ErrorCodesTest {

    @Test
    @DisplayName("Should get error code by code string")
    void testFromCode() {
        ErrorCodes error = ErrorCodes.fromCode("TE-001");
        assertEquals(ErrorCodes.NOT_FOUND, error);
        assertEquals("TE-001", error.getCode());
    }

    @Test
    @DisplayName("Should return null for invalid code")
    void testFromCodeInvalid() {
        ErrorCodes error = ErrorCodes.fromCode("INVALID");
        assertNull(error);
    }

    @Test
    @DisplayName("Should get error code by fun name")
    void testFromFunName() {
        ErrorCodes error = ErrorCodes.fromFunName("Wired Nutella");
        assertEquals(ErrorCodes.NOT_FOUND, error);
    }

    @Test
    @DisplayName("Should return null for invalid fun name")
    void testFromFunNameInvalid() {
        ErrorCodes error = ErrorCodes.fromFunName("Invalid Fun Name");
        assertNull(error);
    }

    @Test
    @DisplayName("Should verify all error codes have unique codes")
    void testUniqueErrorCodes() {
        ErrorCodes[] errors = ErrorCodes.values();
        for (int i = 0; i < errors.length; i++) {
            for (int j = i + 1; j < errors.length; j++) {
                assertNotEquals(errors[i].getCode(), errors[j].getCode(),
                        "Duplicate error code found: " + errors[i].getCode());
            }
        }
    }
}

