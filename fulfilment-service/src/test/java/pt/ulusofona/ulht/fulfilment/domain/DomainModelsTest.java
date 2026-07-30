package pt.ulusofona.ulht.fulfilment.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Domain Models Tests")
class DomainModelsTest {

    // WorkflowProgressEvent Tests
    @Test
    @DisplayName("Should create WorkflowProgressEvent with all fields")
    void testWorkflowProgressEvent() {
        WorkflowProgressEvent event = WorkflowProgressEvent.builder()
                .correlationId("test-123")
                .status("PROCESSING")
                .progress(50)
                .message("Processing...")
                .timestamp(System.currentTimeMillis())
                .build();

        assertEquals("test-123", event.getCorrelationId());
        assertEquals("PROCESSING", event.getStatus());
        assertEquals(50, event.getProgress());
        assertEquals("Processing...", event.getMessage());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Should set progress event fields")
    void testSetProgressEventFields() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setCorrelationId("test");
        event.setStatus("PENDING");
        event.setProgress(0);
        event.setMessage("Starting...");

        assertEquals("test", event.getCorrelationId());
        assertEquals("PENDING", event.getStatus());
        assertEquals(0, event.getProgress());
    }

    @Test
    @DisplayName("Should handle null progress message")
    void testNullProgressMessage() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setMessage(null);
        assertNull(event.getMessage());
    }

    // WorkflowCompletionEvent Tests
    @Test
    @DisplayName("Should create WorkflowCompletionEvent with result")
    void testWorkflowCompletionEvent() {
        Map<String, Object> result = new HashMap<>();
        result.put("credentialsIssued", 3);
        
        WorkflowCompletionEvent event = WorkflowCompletionEvent.builder()
                .correlationId("test-123")
                .status("COMPLETED")
                .progress(100)
                .message("Completed")
                .result(result)
                .timestamp(System.currentTimeMillis())
                .build();

        assertEquals("test-123", event.getCorrelationId());
        assertEquals("COMPLETED", event.getStatus());
        assertEquals(100, event.getProgress());
        assertNotNull(event.getResult());
    }

    @Test
    @DisplayName("Should set completion event fields")
    void testSetCompletionEventFields() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        event.setCorrelationId("test");
        event.setStatus("COMPLETED");
        event.setProgress(100);

        assertEquals("test", event.getCorrelationId());
        assertEquals("COMPLETED", event.getStatus());
        assertEquals(100, event.getProgress());
    }

    @Test
    @DisplayName("Should handle failed completion")
    void testFailedCompletion() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        event.setStatus("FAILED");
        event.setProgress(-1);

        assertEquals("FAILED", event.getStatus());
        assertEquals(-1, event.getProgress());
    }

    @Test
    @DisplayName("Should handle null completion result")
    void testNullCompletionResult() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        event.setResult(null);
        assertNull(event.getResult());
    }

    // WorkflowErrorEvent Tests
    @Test
    @DisplayName("Should create WorkflowErrorEvent with error details")
    void testWorkflowErrorEvent() {
        WorkflowErrorEvent event = WorkflowErrorEvent.builder()
                .correlationId("test-123")
                .status("FAILED")
                .errorCode("FULF-001")
                .errorMessage("Workflow not found")
                .timestamp(System.currentTimeMillis())
                .build();

        assertEquals("test-123", event.getCorrelationId());
        assertEquals("FAILED", event.getStatus());
        assertEquals("FULF-001", event.getErrorCode());
        assertEquals("Workflow not found", event.getErrorMessage());
        assertNotNull(event.getTimestamp());
    }

    @Test
    @DisplayName("Should set error event fields")
    void testSetErrorEventFields() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        event.setCorrelationId("test");
        event.setStatus("FAILED");
        event.setErrorCode("ERROR-001");
        event.setErrorMessage("Test error");

        assertEquals("test", event.getCorrelationId());
        assertEquals("FAILED", event.getStatus());
        assertEquals("ERROR-001", event.getErrorCode());
        assertEquals("Test error", event.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle null error code")
    void testNullErrorCode() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        event.setErrorCode(null);
        assertNull(event.getErrorCode());
    }

    @Test
    @DisplayName("Should handle empty error message")
    void testEmptyErrorMessage() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        event.setErrorMessage("");
        assertEquals("", event.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle long error message")
    void testLongErrorMessage() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        String longMessage = "Error: " + "A".repeat(500);
        event.setErrorMessage(longMessage);
        assertTrue(event.getErrorMessage().length() > 500);
    }

    // General Tests
    @Test
    @DisplayName("Should create default progress event")
    void testDefaultProgressEvent() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        assertNull(event.getCorrelationId());
        assertNull(event.getStatus());
        assertNull(event.getProgress());
    }

    @Test
    @DisplayName("Should create default completion event")
    void testDefaultCompletionEvent() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        assertNull(event.getCorrelationId());
        assertNull(event.getStatus());
    }

    @Test
    @DisplayName("Should create default error event")
    void testDefaultErrorEvent() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        assertNull(event.getCorrelationId());
        assertNull(event.getStatus());
        assertNull(event.getErrorCode());
        assertNull(event.getErrorMessage());
    }

    @Test
    @DisplayName("Should update progress event timestamp")
    void testUpdateProgressTimestamp() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        long timestamp = System.currentTimeMillis();
        event.setTimestamp(timestamp);
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    @DisplayName("Should update completion event timestamp")
    void testUpdateCompletionTimestamp() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        long timestamp = System.currentTimeMillis();
        event.setTimestamp(timestamp);
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    @DisplayName("Should update error event timestamp")
    void testUpdateErrorTimestamp() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        long timestamp = System.currentTimeMillis();
        event.setTimestamp(timestamp);
        assertEquals(timestamp, event.getTimestamp());
    }

    @Test
    @DisplayName("Should handle progress boundaries")
    void testProgressBoundaries() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        
        event.setProgress(0);
        assertEquals(0, event.getProgress());
        
        event.setProgress(100);
        assertEquals(100, event.getProgress());
        
        event.setProgress(-1);
        assertEquals(-1, event.getProgress());
    }

    @Test
    @DisplayName("Should handle special characters in correlation ID")
    void testSpecialCharactersInId() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        String id = "test-123_ABC!@#$%";
        event.setCorrelationId(id);
        assertEquals(id, event.getCorrelationId());
    }

    @Test
    @DisplayName("Should handle various status strings")
    void testVariousStatusStrings() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        
        String[] statuses = {"PENDING", "PROCESSING", "COMPLETED", "FAILED", "CANCELLED"};
        for (String status : statuses) {
            event.setStatus(status);
            assertEquals(status, event.getStatus());
        }
    }

    @Test
    @DisplayName("Should handle complex result objects")
    void testComplexResultObjects() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        Map<String, Object> complexResult = new HashMap<>();
        complexResult.put("key1", "value1");
        complexResult.put("key2", 123);
        complexResult.put("key3", Map.of("nested", "value"));
        
        event.setResult(complexResult);
        assertNotNull(event.getResult());
    }

    @Test
    @DisplayName("Should handle result as string")
    void testResultAsString() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        event.setResult("Simple string result");
        assertEquals("Simple string result", event.getResult());
    }

    @Test
    @DisplayName("Should handle result as number")
    void testResultAsNumber() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        event.setResult(12345);
        assertEquals(12345, event.getResult());
    }

    @Test
    @DisplayName("Should builder pattern for progress event")
    void testProgressEventBuilder() {
        WorkflowProgressEvent event = WorkflowProgressEvent.builder()
                .correlationId("test")
                .status("PROCESSING")
                .progress(75)
                .build();
        
        assertNotNull(event);
        assertEquals(75, event.getProgress());
    }

    @Test
    @DisplayName("Should builder pattern for completion event")
    void testCompletionEventBuilder() {
        WorkflowCompletionEvent event = WorkflowCompletionEvent.builder()
                .correlationId("test")
                .status("COMPLETED")
                .build();
        
        assertNotNull(event);
        assertEquals("COMPLETED", event.getStatus());
    }

    @Test
    @DisplayName("Should builder pattern for error event")
    void testErrorEventBuilder() {
        WorkflowErrorEvent event = WorkflowErrorEvent.builder()
                .correlationId("test")
                .errorCode("ERR-001")
                .build();
        
        assertNotNull(event);
        assertEquals("ERR-001", event.getErrorCode());
    }

    @Test
    @DisplayName("Should handle message updates")
    void testMessageUpdates() {
        WorkflowProgressEvent event = new WorkflowProgressEvent();
        event.setMessage("Initial message");
        assertEquals("Initial message", event.getMessage());
        
        event.setMessage("Updated message");
        assertEquals("Updated message", event.getMessage());
    }

    @Test
    @DisplayName("Should handle completion message updates")
    void testCompletionMessageUpdates() {
        WorkflowCompletionEvent event = new WorkflowCompletionEvent();
        event.setMessage("Initial");
        assertEquals("Initial", event.getMessage());
        
        event.setMessage("Final");
        assertEquals("Final", event.getMessage());
    }

    @Test
    @DisplayName("Should handle error message updates")
    void testErrorMessageUpdates() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        event.setErrorMessage("First error");
        assertEquals("First error", event.getErrorMessage());
        
        event.setErrorMessage("Second error");
        assertEquals("Second error", event.getErrorMessage());
    }

    @Test
    @DisplayName("Should handle all fields in error event")
    void testAllErrorEventFields() {
        WorkflowErrorEvent event = new WorkflowErrorEvent();
        event.setCorrelationId("123");
        event.setStatus("FAILED");
        event.setErrorCode("TEST-001");
        event.setErrorMessage("Test error");
        event.setTimestamp(1000L);

        assertEquals("123", event.getCorrelationId());
        assertEquals("FAILED", event.getStatus());
        assertEquals("TEST-001", event.getErrorCode());
        assertEquals("Test error", event.getErrorMessage());
        assertEquals(1000L, event.getTimestamp());
    }
}

