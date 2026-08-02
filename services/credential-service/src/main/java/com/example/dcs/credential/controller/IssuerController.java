package com.example.dcs.credential.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.dcs.credential.domain.issuer.Session;
import com.example.dcs.credential.exception.ExternalServiceException;
import com.example.dcs.credential.service.issuer.SessionCredentialService;
import com.example.dcs.credential.service.issuer.SessionService;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for Issuer functionality (Session Management and Credential Issuance).
 */
@Slf4j
@RestController
@RequestMapping("/issuer")
@RequiredArgsConstructor
public class IssuerController {

    private final SessionService sessionService;
    private final SessionCredentialService sessionCredentialService;

    @PostMapping("/sessions/{sessionId}/issue")
    public ResponseEntity<Map<String, Object>> issueSessionCredential(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request) {
        
        log.info("📝 Issuing session credential for session: {}", sessionId);
        
        try {
            // Get student ID from request
            String studentId = (String) request.get("studentId");
            if (studentId == null || studentId.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "BAD_REQUEST");
                error.put("message", "studentId is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }
            
            // Get session
            Session session = sessionService.getSession(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
            
            // Issue credential
            Map<String, Object> result = sessionCredentialService.issueSessionCredential(
                sessionId, session, studentId);
            
            // Register student and track credential
            String credentialId = (String) result.getOrDefault("credentialId", "");
            sessionService.registerStudent(sessionId, studentId, credentialId);
            
            // Get updated session with new registered count
            Session updatedSession = sessionService.getSession(sessionId)
                .orElse(session);
            
            log.info("✅ Session credential issued successfully for student {} to session {} (total registered: {})", 
                studentId, sessionId, updatedSession.getRegisteredCount());
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            log.warn("Bad request for session credential issuance: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "BAD_REQUEST");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (ExternalServiceException e) {
            log.error("Failed to issue session credential: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "EXTERNAL_SERVICE_ERROR");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        } catch (Exception e) {
            log.error("Unexpected error issuing session credential: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "INTERNAL_ERROR");
            error.put("message", "An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> getSession(@PathVariable String sessionId) {
        log.debug("Getting session: {}", sessionId);
        
        return sessionService.getSession(sessionId)
            .map(session -> {
                Map<String, Object> response = sessionToMap(session);
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "NOT_FOUND");
                error.put("message", "Session not found: " + sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            });
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, Object> request) {
        log.info("Creating new session: {}", request.get("title"));
        
        try {
            Session session = Session.builder()
                .title((String) request.get("title"))
                .description((String) request.get("description"))
                .conferenceName((String) request.get("conferenceName"))
                .location((String) request.get("location"))
                .startTime(parseDateTime(request.get("startTime")))
                .endTime(parseDateTime(request.get("endTime")))
                .isActive(true)
                .build();
            
            Session created = sessionService.createSession(session);
            
            // Generate QR code URL
            String baseUrl = "http://localhost:8086/api/v1";
            String qrCodeUrl = baseUrl + "/issuer/sessions/" + created.getId() + "/register";
            created.setQrCodeUrl(qrCodeUrl);
            session = sessionService.updateSession(created.getId(), created);
            
            Map<String, Object> response = sessionToMap(session);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Failed to create session: {}", e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "BAD_REQUEST");
            error.put("message", "Failed to create session: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Map<String, Object>>> listSessions() {
        log.debug("Listing all sessions");
        
        List<Map<String, Object>> sessions = sessionService.getAllSessions().stream()
            .sorted((a, b) -> {
                // Sort by creation date (newest first)
                java.time.Instant dateA = a.getCreatedAt() != null ? a.getCreatedAt() : java.time.Instant.MIN;
                java.time.Instant dateB = b.getCreatedAt() != null ? b.getCreatedAt() : java.time.Instant.MIN;
                return dateB.compareTo(dateA);
            })
            .map(this::sessionToMap)
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(sessions);
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, Object>> updateSession(
            @PathVariable String sessionId,
            @RequestBody Map<String, Object> request) {
        
        log.info("Updating session: {}", sessionId);
        
        return sessionService.getSession(sessionId)
            .map(existing -> {
                Session updated = Session.builder()
                    .id(sessionId)
                    .title((String) request.getOrDefault("title", existing.getTitle()))
                    .description((String) request.getOrDefault("description", existing.getDescription()))
                    .conferenceName((String) request.getOrDefault("conferenceName", existing.getConferenceName()))
                    .location((String) request.getOrDefault("location", existing.getLocation()))
                    .startTime(parseDateTime(request.getOrDefault("startTime", existing.getStartTime())))
                    .endTime(parseDateTime(request.getOrDefault("endTime", existing.getEndTime())))
                    .qrCodeUrl((String) request.getOrDefault("qrCodeUrl", existing.getQrCodeUrl()))
                    .registeredCount(existing.getRegisteredCount())
                    .createdAt(existing.getCreatedAt())
                    .isActive((Boolean) request.getOrDefault("isActive", existing.isActive()))
                    .build();
                
                Session saved = sessionService.updateSession(sessionId, updated);
                Map<String, Object> response = sessionToMap(saved);
                return ResponseEntity.ok(response);
            })
            .orElseGet(() -> {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "NOT_FOUND");
                error.put("message", "Session not found: " + sessionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            });
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable String sessionId) {
        log.info("Deleting session: {}", sessionId);
        
        if (sessionService.getSession(sessionId).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        sessionService.deleteSession(sessionId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/sessions/{sessionId}/registrations")
    public ResponseEntity<List<Map<String, Object>>> getRegisteredStudents(@PathVariable String sessionId) {
        log.debug("Getting registered students for session: {}", sessionId);
        
        if (sessionService.getSession(sessionId).isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "NOT_FOUND");
            error.put("message", "Session not found: " + sessionId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        
        List<com.example.dcs.credential.domain.issuer.RegisteredStudent> students = 
            sessionService.getRegisteredStudents(sessionId);
        
        List<Map<String, Object>> result = students.stream()
            .map(this::registeredStudentToMap)
            .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(result);
    }

    // Helper methods
    
    private Map<String, Object> sessionToMap(Session session) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", session.getId());
        map.put("title", session.getTitle());
        map.put("description", session.getDescription());
        map.put("conferenceName", session.getConferenceName());
        map.put("location", session.getLocation());
        map.put("startTime", session.getStartTime() != null ? session.getStartTime().toString() : null);
        map.put("endTime", session.getEndTime() != null ? session.getEndTime().toString() : null);
        map.put("qrCodeUrl", session.getQrCodeUrl());
        map.put("registeredCount", session.getRegisteredCount());
        map.put("createdAt", session.getCreatedAt() != null ? session.getCreatedAt().toString() : null);
        map.put("isActive", session.isActive());
        return map;
    }
    
    private OffsetDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            try {
                return OffsetDateTime.parse((String) value);
            } catch (Exception e) {
                log.warn("Failed to parse date time: {}", value);
                return null;
            }
        }
        if (value instanceof OffsetDateTime) {
            return (OffsetDateTime) value;
        }
        return null;
    }
    
    private Map<String, Object> registeredStudentToMap(com.example.dcs.credential.domain.issuer.RegisteredStudent student) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", student.getId());
        map.put("sessionId", student.getSessionId());
        map.put("studentId", student.getStudentId());
        map.put("studentName", student.getStudentName());
        map.put("email", student.getEmail());
        map.put("registeredAt", student.getRegisteredAt() != null ? student.getRegisteredAt().toString() : null);
        map.put("credentialId", student.getCredentialId());
        return map;
    }
}
