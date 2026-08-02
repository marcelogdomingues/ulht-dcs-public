package com.example.dcs.sis.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import com.example.dcs.sis.generated.api.AuthenticationApi;
import com.example.dcs.sis.generated.model.*;
import com.example.dcs.sis.service.SisService;
import com.example.dcs.sis.kafka.CredentialWorkflowProducer;
import com.example.dcs.sis.exception.http.ExternalServiceException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;

@Slf4j
@Controller
public class AuthenticationController implements AuthenticationApi {

    private final SisService sisService;
    private final CredentialWorkflowProducer credentialWorkflowProducer;

    public AuthenticationController(SisService sisService, 
                                   CredentialWorkflowProducer credentialWorkflowProducer) {
        this.sisService = sisService;
        this.credentialWorkflowProducer = credentialWorkflowProducer;
    }

    @Override
    public ResponseEntity<LoginResponse> studentLogin(@Valid StudentServiceRequest studentServiceRequest) {
        log.info("Received request to login student: {}", studentServiceRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertToMap(studentServiceRequest);
            
            // 1. Generate correlation ID for workflow tracking
            String correlationId = java.util.UUID.randomUUID().toString();
            log.info("Generated correlationId: {} for user: {}", correlationId, studentServiceRequest.getUserName());
            
            // 2. Get student data from external DCS services (NO MOCK DATA FALLBACK!)
            log.info("Calling SIS API for student data: {}", studentServiceRequest.getUserName());
            Object studentData = sisService.login(requestMap);
            log.info("✅ Got real student data from SIS API");
            
            // 3. Start credential workflow (pass correlationId to maintain traceability)
            credentialWorkflowProducer.startCredentialWorkflow(
                correlationId,  // Use generated correlationId
                studentServiceRequest.getUserName(), 
                studentData
            );
            
            // 4. Return async response with correlation ID and expiration
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setStatus("PROCESSING");
            loginResponse.setCorrelationId(correlationId);
            loginResponse.setExpiresAt(OffsetDateTime.now().plusHours(24)); // 24 hours expiration
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(loginResponse);
            
        } catch (Exception e) {
            // CRITICAL: SIS API failed - DO NOT issue credentials with fake data!
            log.error("❌ SIS API failed for user {}: {} - Request will FAIL", 
                     studentServiceRequest.getUserName(), e.getMessage(), e);
            
            // Throw specific exception (handled by GlobalExceptionHandler)
            throw new ExternalServiceException(
                "Failed to retrieve student data from SIS API: " + e.getMessage(), e);
        }
    }

    @Override
    public ResponseEntity<RegistrationResponse> studentRegistration(@Valid StudentRegistrationRequest studentRegistrationRequest) {
        log.info("Received request to register student: {}", studentRegistrationRequest.getUserName());
        
        try {
            Map<String, Object> requestMap = convertRegistrationToMap(studentRegistrationRequest);
            
            // Call the service to potentially throw exceptions for testing
            sisService.registration(requestMap);
            
            // For now, return a simple response - you can implement proper conversion later
            RegistrationResponse registrationResponse = new RegistrationResponse();
            registrationResponse.setStatus("success");
            registrationResponse.setMessage("Student registered successfully");
            
            RegistrationResponseData data = new RegistrationResponseData();
            data.setStudentId("generated-id");
            data.setRegistrationDate(java.time.OffsetDateTime.now());
            registrationResponse.setData(data);
            
            return ResponseEntity.ok(registrationResponse);
        } catch (Exception e) {
            log.error("Error during student registration for user: {}", studentRegistrationRequest.getUserName(), e);
            throw new ExternalServiceException("Failed to register student: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> convertToMap(StudentServiceRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", request.getUserName());
        map.put("installKey", request.getInstallKey());
        map.put("language", request.getLanguage() != null ? request.getLanguage().getValue() : null);
        map.put("platform", request.getPlatform() != null ? request.getPlatform().getValue() : null);
        map.put("application", request.getApplication());
        map.put("versionCode", request.getVersionCode());
        return map;
    }

    private Map<String, Object> convertRegistrationToMap(StudentRegistrationRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", request.getUserName());
        map.put("language", request.getLanguage() != null ? request.getLanguage().getValue() : null);
        map.put("platform", request.getPlatform() != null ? request.getPlatform().getValue() : null);
        map.put("password", request.getPassword());
        map.put("application", request.getApplication());
        map.put("versionCode", request.getVersionCode());
        return map;
    }

}