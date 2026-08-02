package com.example.dcs.credential.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka producer for wallet-related events.
 * Publishes wallet operations (login, register, account access, etc.) to Kafka topics
 * for event-driven processing and auditing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletEventProducer {
    
    private final KafkaTemplate<String, Map<String, Object>> workflowKafkaTemplate;
    
    private static final String WALLET_LOGIN_TOPIC = "wallet.login";
    private static final String WALLET_REGISTER_TOPIC = "wallet.register";
    private static final String WALLET_ACCOUNT_ACCESS_TOPIC = "wallet.account.access";
    private static final String WALLET_OPERATION_TOPIC = "wallet.operation";
    
    /**
     * Publishes wallet login event
     * 
     * @param userId User identifier (email)
     * @param loginRequest Login request details
     * @param sessionCookie Session cookie returned from login
     * @param success Whether login was successful
     */
    public void publishWalletLogin(String userId, Object loginRequest, String sessionCookie, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("eventType", "WALLET_LOGIN");
        event.put("status", success ? "SUCCESS" : "FAILED");
        event.put("loginRequest", loginRequest);
        event.put("hasSessionCookie", sessionCookie != null);
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send(WALLET_LOGIN_TOPIC, userId, event);
            log.info("Published wallet login event to {} topic (userId: {}, success: {})", 
                    WALLET_LOGIN_TOPIC, userId, success);
        } catch (Exception e) {
            log.error("Failed to publish wallet login event for userId: {}", userId, e);
        }
    }
    
    /**
     * Publishes wallet registration event
     * 
     * @param userId User identifier (email)
     * @param registerRequest Registration request details
     * @param success Whether registration was successful
     */
    public void publishWalletRegister(String userId, Object registerRequest, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("eventType", "WALLET_REGISTER");
        event.put("status", success ? "SUCCESS" : "FAILED");
        event.put("registerRequest", registerRequest);
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send(WALLET_REGISTER_TOPIC, userId, event);
            log.info("Published wallet registration event to {} topic (userId: {}, success: {})", 
                    WALLET_REGISTER_TOPIC, userId, success);
        } catch (Exception e) {
            log.error("Failed to publish wallet registration event for userId: {}", userId, e);
        }
    }
    
    /**
     * Publishes wallet account access event (get accounts, keys, DIDs)
     * 
     * @param userId User identifier
     * @param walletId Wallet identifier
     * @param operationType Type of operation (GET_ACCOUNTS, GET_KEYS, GET_DIDS)
     * @param success Whether operation was successful
     */
    public void publishWalletAccountAccess(String userId, String walletId, String operationType, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("walletId", walletId);
        event.put("eventType", "WALLET_ACCOUNT_ACCESS");
        event.put("operationType", operationType);
        event.put("status", success ? "SUCCESS" : "FAILED");
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send(WALLET_ACCOUNT_ACCESS_TOPIC, userId, event);
            log.debug("Published wallet account access event to {} topic (userId: {}, operation: {})", 
                    WALLET_ACCOUNT_ACCESS_TOPIC, userId, operationType);
        } catch (Exception e) {
            log.error("Failed to publish wallet account access event for userId: {}", userId, e);
        }
    }
    
    /**
     * Publishes generic wallet operation event
     * 
     * @param userId User identifier
     * @param operationType Type of wallet operation
     * @param details Additional operation details
     * @param success Whether operation was successful
     */
    public void publishWalletOperation(String userId, String operationType, Map<String, Object> details, boolean success) {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", userId);
        event.put("eventType", "WALLET_OPERATION");
        event.put("operationType", operationType);
        event.put("status", success ? "SUCCESS" : "FAILED");
        event.put("details", details != null ? details : new HashMap<>());
        event.put("timestamp", Instant.now().toString());
        
        try {
            workflowKafkaTemplate.send(WALLET_OPERATION_TOPIC, userId, event);
            log.debug("Published wallet operation event to {} topic (userId: {}, operation: {})", 
                    WALLET_OPERATION_TOPIC, userId, operationType);
        } catch (Exception e) {
            log.error("Failed to publish wallet operation event for userId: {}", userId, e);
        }
    }
}

