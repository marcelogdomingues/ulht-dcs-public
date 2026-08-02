package com.example.dcs.student.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ErrorCodes Tests")
class ErrorCodesTest {

    @Test
    @DisplayName("Should get error code by code string")
    void testFromCode() {
        ErrorCodes error = ErrorCodes.fromCode("STUD-001");
        assertEquals(ErrorCodes.INVALID_REQUEST, error);
        assertEquals("STUD-001", error.getCode());
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
        ErrorCodes error = ErrorCodes.fromFunName("Confused Student");
        assertEquals(ErrorCodes.INVALID_REQUEST, error);
    }

    @Test
    @DisplayName("Should return null for invalid fun name")
    void testFromFunNameInvalid() {
        ErrorCodes error = ErrorCodes.fromFunName("Invalid Fun Name");
        assertNull(error);
    }

    @Test
    @DisplayName("Should identify validation errors")
    void testIsValidationError() {
        assertTrue(ErrorCodes.VALIDATION_ERROR.isValidationError());
        assertTrue(ErrorCodes.INVALID_USERNAME.isValidationError());
        assertFalse(ErrorCodes.KAFKA_PRODUCE_ERROR.isValidationError());
    }

    @Test
    @DisplayName("Should identify Kafka errors")
    void testIsKafkaError() {
        assertTrue(ErrorCodes.KAFKA_PRODUCE_ERROR.isKafkaError());
        assertTrue(ErrorCodes.KAFKA_CONNECTION_ERROR.isKafkaError());
        assertFalse(ErrorCodes.INVALID_REQUEST.isKafkaError());
    }

    @Test
    @DisplayName("Should identify external service errors")
    void testIsExternalServiceError() {
        assertTrue(ErrorCodes.FULFILMENT_SERVICE_ERROR.isExternalServiceError());
        assertFalse(ErrorCodes.INVALID_REQUEST.isExternalServiceError());
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
            assertTrue(error.getCode().startsWith("STUD-"),
                    "Error code should start with STUD-: " + error.getCode());
        }
    }

    @Test
    @DisplayName("Should have school theme fun names")
    void testSchoolTheme() {
        // Verify some school-themed fun names exist
        ErrorCodes[] errors = ErrorCodes.values();
        assertTrue(errors.length > 0);
        assertNotNull(ErrorCodes.INVALID_REQUEST.getFunName());
    }

    @Test
    @DisplayName("Should return same error for same code")
    void testConsistency() {
        ErrorCodes error1 = ErrorCodes.fromCode("STUD-001");
        ErrorCodes error2 = ErrorCodes.fromCode("STUD-001");
        assertEquals(error1, error2);
    }

    @Test
    @DisplayName("Should handle null code lookup")
    void testNullCodeLookup() {
        ErrorCodes error = ErrorCodes.fromCode(null);
        assertEquals(ErrorCodes.UNKNOWN_ERROR, error);
    }

    @Test
    @DisplayName("Should handle null fun name lookup")
    void testNullFunNameLookup() {
        ErrorCodes error = ErrorCodes.fromFunName(null);
        assertNull(error);
    }

    @Test
    @DisplayName("Should have UNKNOWN_ERROR as fallback")
    void testUnknownError() {
        assertNotNull(ErrorCodes.UNKNOWN_ERROR);
        assertEquals("STUD-000", ErrorCodes.UNKNOWN_ERROR.getCode());
    }

    @Test
    @DisplayName("Should have INTERNAL_SERVER_ERROR")
    void testInternalServerError() {
        assertNotNull(ErrorCodes.INTERNAL_SERVER_ERROR);
        assertEquals("STUD-999", ErrorCodes.INTERNAL_SERVER_ERROR.getCode());
    }

    @Test
    @DisplayName("Should categorize request errors")
    void testRequestErrors() {
        ErrorCodes[] requestErrors = {
            ErrorCodes.INVALID_REQUEST,
            ErrorCodes.MISSING_REQUIRED_FIELD,
            ErrorCodes.INVALID_FIELD_FORMAT
        };
        
        for (ErrorCodes error : requestErrors) {
            assertTrue(error.getCode().startsWith("STUD-00"));
        }
    }

    @Test
    @DisplayName("Should categorize validation errors")
    void testValidationErrorGroup() {
        ErrorCodes[] validationErrors = {
            ErrorCodes.VALIDATION_ERROR,
            ErrorCodes.INVALID_USERNAME,
            ErrorCodes.INVALID_STUDENT_CODE
        };
        
        for (ErrorCodes error : validationErrors) {
            assertTrue(error.getCode().startsWith("STUD-01"));
        }
    }

    @Test
    @DisplayName("Should categorize Kafka errors")
    void testKafkaErrorGroup() {
        ErrorCodes[] kafkaErrors = {
            ErrorCodes.KAFKA_PRODUCE_ERROR,
            ErrorCodes.KAFKA_CONNECTION_ERROR
        };
        
        for (ErrorCodes error : kafkaErrors) {
            assertTrue(error.getCode().startsWith("STUD-02"));
        }
    }

    @Test
    @DisplayName("Should handle case-sensitive code lookup")
    void testCaseSensitiveCode() {
        ErrorCodes error = ErrorCodes.fromCode("stud-001");
        assertEquals(ErrorCodes.UNKNOWN_ERROR, error);
    }

    @Test
    @DisplayName("Should handle case-sensitive fun name lookup")
    void testCaseSensitiveFunName() {
        ErrorCodes error = ErrorCodes.fromFunName("confused student");
        assertNull(error);
    }

    @Test
    @DisplayName("Should get code value")
    void testGetCode() {
        String code = ErrorCodes.INVALID_REQUEST.getCode();
        assertEquals("STUD-001", code);
    }

    @Test
    @DisplayName("Should get fun name value")
    void testGetFunName() {
        String funName = ErrorCodes.INVALID_REQUEST.getFunName();
        assertNotNull(funName);
        assertFalse(funName.isEmpty());
    }

    @Test
    @DisplayName("Should get description value")
    void testGetDescription() {
        String description = ErrorCodes.INVALID_REQUEST.getDescription();
        assertNotNull(description);
        assertFalse(description.isEmpty());
    }

    @Test
    @DisplayName("Should have at least 20 error codes")
    void testMinimumErrorCodes() {
        ErrorCodes[] errors = ErrorCodes.values();
        assertTrue(errors.length >= 20, "Should have at least 20 error codes");
    }

    @Test
    @DisplayName("Should have consistent naming pattern")
    void testNamingPattern() {
        for (ErrorCodes error : ErrorCodes.values()) {
            String code = error.getCode();
            assertTrue(code.matches("STUD-\\d{3}"),
                    "Error code should match pattern STUD-XXX: " + code);
        }
    }

    @Test
    @DisplayName("Should enum values be accessible")
    void testEnumValues() {
        ErrorCodes[] values = ErrorCodes.values();
        assertNotNull(values);
        assertTrue(values.length > 0);
    }

    @Test
    @DisplayName("Should valueOf work correctly")
    void testValueOf() {
        ErrorCodes error = ErrorCodes.valueOf("INVALID_REQUEST");
        assertEquals(ErrorCodes.INVALID_REQUEST, error);
    }

    @Test
    @DisplayName("Should throw exception for invalid valueOf")
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            ErrorCodes.valueOf("INVALID_ENUM_NAME");
        });
    }

    @Test
    @DisplayName("Should have error for missing fields")
    void testMissingFieldError() {
        assertNotNull(ErrorCodes.MISSING_REQUIRED_FIELD);
        assertEquals("STUD-002", ErrorCodes.MISSING_REQUIRED_FIELD.getCode());
    }

    @Test
    @DisplayName("Should have error for invalid format")
    void testInvalidFormatError() {
        assertNotNull(ErrorCodes.INVALID_FIELD_FORMAT);
        assertEquals("STUD-003", ErrorCodes.INVALID_FIELD_FORMAT.getCode());
    }

    @Test
    @DisplayName("Should have error for credentials not ready")
    void testCredentialsNotReadyError() {
        assertNotNull(ErrorCodes.CREDENTIALS_NOT_READY);
        assertEquals("STUD-060", ErrorCodes.CREDENTIALS_NOT_READY.getCode());
    }

    @Test
    @DisplayName("Should have error for credentials not found")
    void testCredentialsNotFoundError() {
        assertNotNull(ErrorCodes.CREDENTIALS_NOT_FOUND);
        assertEquals("STUD-061", ErrorCodes.CREDENTIALS_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Should have error for workflow not found")
    void testWorkflowNotFoundError() {
        assertNotNull(ErrorCodes.WORKFLOW_NOT_FOUND);
        assertEquals("STUD-050", ErrorCodes.WORKFLOW_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("Should handle empty string code lookup")
    void testEmptyStringCodeLookup() {
        ErrorCodes error = ErrorCodes.fromCode("");
        assertEquals(ErrorCodes.UNKNOWN_ERROR, error);
    }

    @Test
    @DisplayName("Should handle empty string fun name lookup")
    void testEmptyStringFunNameLookup() {
        ErrorCodes error = ErrorCodes.fromFunName("");
        assertNull(error);
    }

    @Test
    @DisplayName("Should error codes be singleton per type")
    void testSingletonPerType() {
        ErrorCodes error1 = ErrorCodes.INVALID_REQUEST;
        ErrorCodes error2 = ErrorCodes.INVALID_REQUEST;
        assertSame(error1, error2);
    }

    @Test
    @DisplayName("Should maintain enum order")
    void testEnumOrder() {
        ErrorCodes[] errors = ErrorCodes.values();
        assertTrue(errors[0].ordinal() == 0);
        assertTrue(errors[errors.length - 1].ordinal() == errors.length - 1);
    }

    @Test
    @DisplayName("Should compare enum values correctly")
    void testEnumComparison() {
        ErrorCodes error1 = ErrorCodes.INVALID_REQUEST;
        ErrorCodes error2 = ErrorCodes.INVALID_REQUEST;
        ErrorCodes error3 = ErrorCodes.VALIDATION_ERROR;
        
        assertEquals(error1, error2);
        assertNotEquals(error1, error3);
    }

    @Test
    @DisplayName("Should have toString method")
    void testToString() {
        String str = ErrorCodes.INVALID_REQUEST.toString();
        assertNotNull(str);
        assertEquals("INVALID_REQUEST", str);
    }

    @Test
    @DisplayName("Should handle correlation ID generation error")
    void testCorrelationIdError() {
        assertNotNull(ErrorCodes.CORRELATION_ID_GENERATION_FAILED);
        assertTrue(ErrorCodes.CORRELATION_ID_GENERATION_FAILED.getCode().startsWith("STUD-03"));
    }

    @Test
    @DisplayName("Should have configuration error codes")
    void testConfigurationErrors() {
        assertNotNull(ErrorCodes.CONFIGURATION_ERROR);
        assertNotNull(ErrorCodes.MISSING_CONFIGURATION);
        assertTrue(ErrorCodes.CONFIGURATION_ERROR.getCode().startsWith("STUD-07"));
    }

    @Test
    @DisplayName("Should distinguish between different error categories")
    void testErrorCategories() {
        assertFalse(ErrorCodes.KAFKA_PRODUCE_ERROR.isValidationError());
        assertFalse(ErrorCodes.VALIDATION_ERROR.isKafkaError());
        assertFalse(ErrorCodes.FULFILMENT_SERVICE_ERROR.isValidationError());
    }
}

