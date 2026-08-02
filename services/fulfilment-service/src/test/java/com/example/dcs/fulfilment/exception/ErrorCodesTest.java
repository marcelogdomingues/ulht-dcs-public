package com.example.dcs.fulfilment.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCodes Tests")
class ErrorCodesTest {

    @Test
    @DisplayName("Should get error code by code string")
    void testFromCode() {
        ErrorCodes error = ErrorCodes.fromCode("FULF-001");
        assertEquals(ErrorCodes.WORKFLOW_NOT_FOUND, error);
        assertEquals("FULF-001", error.getCode());
    }

    @Test
    @DisplayName("Should return UNKNOWN_ERROR for invalid code")
    void testFromCodeInvalid() {
        ErrorCodes error = ErrorCodes.fromCode("INVALID");
        assertEquals(ErrorCodes.UNKNOWN_ERROR, error);
    }

    @Test
    @DisplayName("Should get error code by fun name")
    void testFromFunName() {
        ErrorCodes error = ErrorCodes.fromFunName("Lost Package");
        assertEquals(ErrorCodes.WORKFLOW_NOT_FOUND, error);
    }

    @Test
    @DisplayName("Should return null for invalid fun name")
    void testFromFunNameInvalid() {
        ErrorCodes error = ErrorCodes.fromFunName("Invalid Fun Name");
        assertNull(error);
    }

    @Test
    @DisplayName("Should identify workflow errors")
    void testIsWorkflowError() {
        assertTrue(ErrorCodes.WORKFLOW_NOT_FOUND.isWorkflowError());
        assertTrue(ErrorCodes.WORKFLOW_CREATION_FAILED.isWorkflowError());
        assertFalse(ErrorCodes.SSE_CONNECTION_FAILED.isWorkflowError());
    }

    @Test
    @DisplayName("Should identify SSE errors")
    void testIsSSEError() {
        assertTrue(ErrorCodes.SSE_CONNECTION_FAILED.isSSEError());
        assertTrue(ErrorCodes.SSE_SEND_FAILED.isSSEError());
        assertFalse(ErrorCodes.WORKFLOW_NOT_FOUND.isSSEError());
    }

    @Test
    @DisplayName("Should identify Kafka errors")
    void testIsKafkaError() {
        assertTrue(ErrorCodes.KAFKA_CONSUME_ERROR.isKafkaError());
        assertTrue(ErrorCodes.KAFKA_PRODUCE_ERROR.isKafkaError());
        assertFalse(ErrorCodes.WORKFLOW_NOT_FOUND.isKafkaError());
    }

    @Test
    @DisplayName("Should have unique error codes")
    void testUniqueErrorCodes() {
        ErrorCodes[] errors = ErrorCodes.values();
        for (int i = 0; i < errors.length; i++) {
            for (int j = i + 1; j < errors.length; j++) {
                assertNotEquals(errors[i].getCode(), errors[j].getCode(),
                        "Duplicate error code found: " + errors[i].getCode());
            }
        }
    }

    @Test
    @DisplayName("Should have fun name for all errors")
    void testAllHaveFunNames() {
        for (ErrorCodes error : ErrorCodes.values()) {
            assertNotNull(error.getFunName());
            assertFalse(error.getFunName().isEmpty());
        }
    }

    @Test
    @DisplayName("Should have description for all errors")
    void testAllHaveDescriptions() {
        for (ErrorCodes error : ErrorCodes.values()) {
            assertNotNull(error.getDescription());
            assertFalse(error.getDescription().isEmpty());
        }
    }

    @Test
    @DisplayName("Should verify code format")
    void testCodeFormat() {
        for (ErrorCodes error : ErrorCodes.values()) {
            assertTrue(error.getCode().startsWith("FULF-"),
                    "Error code should start with FULF-: " + error.getCode());
        }
    }

    @Test
    @DisplayName("Should have delivery theme fun names")
    void testDeliveryTheme() {
        // Verify some delivery-themed fun names exist
        ErrorCodes[] errors = ErrorCodes.values();
        assertTrue(errors.length > 0);
        // Just verify they have fun names (delivery theme)
        assertNotNull(ErrorCodes.WORKFLOW_NOT_FOUND.getFunName());
    }
}

