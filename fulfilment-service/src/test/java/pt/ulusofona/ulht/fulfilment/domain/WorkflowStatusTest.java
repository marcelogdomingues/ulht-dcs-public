package pt.ulusofona.ulht.fulfilment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WorkflowStatus Tests")
class WorkflowStatusTest {

    @Test
    @DisplayName("Should create workflow status with all fields")
    void testCreateWorkflowStatus() {
        WorkflowStatus status = WorkflowStatus.builder()
                .correlationId("test-123")
                .status("PROCESSING")
                .progress(50)
                .message("Processing...")
                .timestamp(System.currentTimeMillis())
                .lastUpdated(System.currentTimeMillis())
                .build();

        assertEquals("test-123", status.getCorrelationId());
        assertEquals("PROCESSING", status.getStatus());
        assertEquals(50, status.getProgress());
        assertEquals("Processing...", status.getMessage());
        assertNotNull(status.getTimestamp());
        assertNotNull(status.getLastUpdated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "PROCESSING", "COMPLETED", "FAILED"})
    @DisplayName("Should handle all workflow statuses")
    void testWorkflowStatuses(String statusValue) {
        WorkflowStatus status = new WorkflowStatus();
        status.setStatus(statusValue);
        assertEquals(statusValue, status.getStatus());
    }

    @Test
    @DisplayName("Should set correlation ID")
    void testCorrelationId() {
        WorkflowStatus status = new WorkflowStatus();
        status.setCorrelationId("test-correlation-id");
        assertEquals("test-correlation-id", status.getCorrelationId());
    }

    @Test
    @DisplayName("Should set progress")
    void testProgress() {
        WorkflowStatus status = new WorkflowStatus();
        status.setProgress(75);
        assertEquals(75, status.getProgress());
    }

    @Test
    @DisplayName("Should set message")
    void testMessage() {
        WorkflowStatus status = new WorkflowStatus();
        status.setMessage("Test message");
        assertEquals("Test message", status.getMessage());
    }

    @Test
    @DisplayName("Should set result when completed")
    void testResult() {
        WorkflowStatus status = new WorkflowStatus();
        status.setResult("Test result");
        assertEquals("Test result", status.getResult());
    }

    @Test
    @DisplayName("Should set error fields when failed")
    void testErrorFields() {
        WorkflowStatus status = new WorkflowStatus();
        status.setErrorCode("FULF-001");
        status.setErrorName("Lost Package");
        status.setErrorMessage("Workflow not found");

        assertEquals("FULF-001", status.getErrorCode());
        assertEquals("Lost Package", status.getErrorName());
        assertEquals("Workflow not found", status.getErrorMessage());
    }
}

