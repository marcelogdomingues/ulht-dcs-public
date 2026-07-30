package pt.ulusofona.ulht.credential.service.issuer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pt.ulusofona.ulht.credential.domain.issuer.RegisteredStudent;
import pt.ulusofona.ulht.credential.domain.issuer.Session;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing conference sessions.
 * Uses in-memory storage (ConcurrentHashMap) for now.
 * For production, replace with database-backed repository.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<RegisteredStudent>> registeredStudents = new ConcurrentHashMap<>();
    
    public Session createSession(Session session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID().toString());
        }
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(java.time.Instant.now());
        }
        sessions.put(session.getId(), session);
        registeredStudents.put(session.getId(), new ArrayList<>());
        log.info("Created session: {} - {}", session.getId(), session.getTitle());
        return session;
    }
    
    public Optional<Session> getSession(String id) {
        Session session = sessions.get(id);
        if (session != null) {
            // Update registered count from actual registrations
            List<RegisteredStudent> students = registeredStudents.getOrDefault(id, new ArrayList<>());
            session.setRegisteredCount(students.size());
        }
        return Optional.ofNullable(session);
    }
    
    public List<Session> getAllSessions() {
        List<Session> allSessions = new ArrayList<>(sessions.values());
        // Update registered counts from actual registrations
        for (Session session : allSessions) {
            List<RegisteredStudent> students = registeredStudents.getOrDefault(session.getId(), new ArrayList<>());
            session.setRegisteredCount(students.size());
        }
        return allSessions;
    }
    
    public Session updateSession(String id, Session updatedSession) {
        Session existing = sessions.get(id);
        if (existing == null) {
            throw new NoSuchElementException("Session not found: " + id);
        }
        updatedSession.setId(id);
        updatedSession.setCreatedAt(existing.getCreatedAt());
        // Preserve registered count
        updatedSession.setRegisteredCount(existing.getRegisteredCount());
        sessions.put(id, updatedSession);
        log.info("Updated session: {} - {}", id, updatedSession.getTitle());
        return updatedSession;
    }
    
    public void deleteSession(String id) {
        sessions.remove(id);
        registeredStudents.remove(id);
        log.info("Deleted session: {}", id);
    }
    
    public void registerStudent(String sessionId, String studentId, String credentialId) {
        Session session = sessions.get(sessionId);
        if (session == null) {
            throw new NoSuchElementException("Session not found: " + sessionId);
        }
        
        List<RegisteredStudent> students = registeredStudents.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // Check if already registered
        boolean alreadyRegistered = students.stream()
            .anyMatch(s -> s.getStudentId().equals(studentId));
        
        if (!alreadyRegistered) {
            RegisteredStudent registeredStudent = RegisteredStudent.builder()
                .id(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .studentId(studentId)
                .email(studentId + "@students.example.edu")
                .registeredAt(java.time.Instant.now())
                .credentialId(credentialId)
                .build();
            
            students.add(registeredStudent);
            session.setRegisteredCount(students.size());
            log.info("Registered student {} for session {} (total: {})", studentId, sessionId, students.size());
        } else {
            log.debug("Student {} already registered for session {}", studentId, sessionId);
        }
    }
    
    public List<RegisteredStudent> getRegisteredStudents(String sessionId) {
        return new ArrayList<>(registeredStudents.getOrDefault(sessionId, new ArrayList<>()));
    }
    
    @Deprecated
    public void incrementRegisteredCount(String sessionId) {
        // This method is kept for backward compatibility
        // But now we track actual registered students
        log.warn("Using deprecated incrementRegisteredCount - should use registerStudent instead");
        Session session = sessions.get(sessionId);
        if (session != null) {
            session.setRegisteredCount(session.getRegisteredCount() + 1);
        }
    }
}

