package com.example.dcs.credential.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.dcs.credential.monitoring.BusinessMetricsService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmartMessageHandler Tests")
class SmartMessageHandlerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private BusinessMetricsService metricsService;

    @Mock
    private SendResult<String, Object> sendResult;

    private SmartMessageHandler smartMessageHandler;

    @BeforeEach
    void setUp() {
        smartMessageHandler = new SmartMessageHandler();
        ReflectionTestUtils.setField(smartMessageHandler, "kafkaTemplate", kafkaTemplate);
        ReflectionTestUtils.setField(smartMessageHandler, "metricsService", metricsService);
        ReflectionTestUtils.setField(smartMessageHandler, "maxMessageSize", 104857600); // 100MB
        ReflectionTestUtils.setField(smartMessageHandler, "chunkSize", 52428800); // 50MB
        ReflectionTestUtils.setField(smartMessageHandler, "externalStorageThreshold", 209715200); // 200MB
    }

    @Test
    @DisplayName("Should send normal message successfully")
    void testSendNormalMessage() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        String message = "Hello, World!";
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(metricsService).recordKafkaMessageSize(eq(topic), eq("String"), anyInt());
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should send moderately large message with compression")
    void testSendModeratelyLargeMessage() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        String message = createLargeString(15 * 1024 * 1024); // 15MB
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(metricsService).recordKafkaMessageSize(eq(topic), eq("String"), anyInt());
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should send large message with chunking warning")
    void testSendLargeMessage() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        String message = createLargeString(150 * 1024 * 1024); // 150MB
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(metricsService).recordKafkaMessageSize(eq(topic), eq("String"), anyInt());
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle byte array message")
    void testSendByteArrayMessage() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        byte[] message = "Hello, World!".getBytes();
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(metricsService).recordKafkaMessageSize(eq(topic), eq("byte[]"), anyInt());
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle custom object message")
    void testSendCustomObjectMessage() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        TestObject message = new TestObject("test data");
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(metricsService).recordKafkaMessageSize(eq(topic), eq("TestObject"), anyInt());
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle null topic")
    void testSendMessageWithNullTopic() {
        // When & Then
        assertThrows(Exception.class, () -> {
            smartMessageHandler.sendMessage(null, "key", "message").get();
        });
    }

    @Test
    @DisplayName("Should handle null key")
    void testSendMessageWithNullKey() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = null;
        String message = "Hello, World!";
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle empty topic")
    void testSendMessageWithEmptyTopic() {
        // When & Then
        assertThrows(Exception.class, () -> {
            smartMessageHandler.sendMessage("", "key", "message").get();
        });
    }

    @Test
    @DisplayName("Should handle empty key")
    void testSendMessageWithEmptyKey() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "";
        String message = "Hello, World!";
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle special characters in topic and key")
    void testSendMessageWithSpecialCharacters() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic@#$%";
        String key = "test-key@#$%";
        String message = "Hello, World!";
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(kafkaTemplate).send(topic, key, message);
    }

    @Test
    @DisplayName("Should handle very long topic and key")
    void testSendMessageWithVeryLongTopicAndKey() throws ExecutionException, InterruptedException {
        // Given
        String longString = "a".repeat(1000);
        String topic = longString;
        String key = longString;
        String message = "Hello, World!";
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(sendResult);
        when(kafkaTemplate.send(eq(topic), eq(key), eq(message))).thenReturn(future);

        // When
        CompletableFuture<Void> result = smartMessageHandler.sendMessage(topic, key, message);

        // Then
        result.get(); // Should not throw exception
        verify(kafkaTemplate).send(topic, key, message);
    }

    // Helper methods
    private String createLargeString(int sizeInBytes) {
        StringBuilder sb = new StringBuilder();
        while (sb.length() * 2 < sizeInBytes) {
            sb.append("a");
        }
        return sb.toString();
    }

    // Test object for custom object testing
    private static class TestObject {
        private String data;

        public TestObject(String data) {
            this.data = data;
        }

        @Override
        public String toString() {
            return "TestObject{data='" + data + "'}";
        }
    }
} 