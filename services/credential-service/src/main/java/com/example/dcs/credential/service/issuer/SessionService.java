package com.example.dcs.credential.service.issuer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.dcs.credential.domain.issuer.RegisteredStudent;
import com.example.dcs.credential.domain.issuer.Session;
import com.example.dcs.credential.persistence.IssuerRegistrationEntity;
import com.example.dcs.credential.persistence.IssuerRegistrationRepository;
import com.example.dcs.credential.persistence.IssuerSessionEntity;
import com.example.dcs.credential.persistence.IssuerSessionRepository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing conference sessions.
 *
 * <p>Durable session and registration state is persisted in PostgreSQL via Spring
 * Data JPA (replacing the previous in-memory {@code ConcurrentHashMap} store), so it
 * survives restarts and is shared across horizontally-scaled instances. Behaviour and
 * IDs are identical to the previous map-backed implementation; domain objects are
 * mapped to/from JPA entities at this boundary so controllers are unchanged.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final IssuerSessionRepository sessionRepository;
    private final IssuerRegistrationRepository registrationRepository;

    @Transactional
    public Session createSession(Session session) {
        if (session.getId() == null) {
            session.setId(UUID.randomUUID().toString());
        }
        if (session.getCreatedAt() == null) {
            session.setCreatedAt(Instant.now());
        }
        sessionRepository.save(toEntity(session));
        log.info("Created session: {} - {}", session.getId(), session.getTitle());
        // Registrations start empty for a new session.
        session.setRegisteredCount(0);
        return session;
    }

    @Transactional(readOnly = true)
    public Optional<Session> getSession(String id) {
        return sessionRepository.findById(id)
                .map(entity -> {
                    Session session = toDomain(entity);
                    // Update registered count from actual registrations
                    session.setRegisteredCount((int) registrationRepository.countBySessionId(id));
                    return session;
                });
    }

    @Transactional(readOnly = true)
    public List<Session> getAllSessions() {
        List<Session> allSessions = new ArrayList<>();
        for (IssuerSessionEntity entity : sessionRepository.findAll()) {
            Session session = toDomain(entity);
            // Update registered counts from actual registrations
            session.setRegisteredCount((int) registrationRepository.countBySessionId(entity.getId()));
            allSessions.add(session);
        }
        return allSessions;
    }

    @Transactional
    public Session updateSession(String id, Session updatedSession) {
        IssuerSessionEntity existing = sessionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + id));
        updatedSession.setId(id);
        updatedSession.setCreatedAt(existing.getCreatedAt());
        sessionRepository.save(toEntity(updatedSession));
        // Preserve registered count (derived from actual registrations)
        updatedSession.setRegisteredCount((int) registrationRepository.countBySessionId(id));
        log.info("Updated session: {} - {}", id, updatedSession.getTitle());
        return updatedSession;
    }

    @Transactional
    public void deleteSession(String id) {
        registrationRepository.deleteBySessionId(id);
        sessionRepository.deleteById(id);
        log.info("Deleted session: {}", id);
    }

    @Transactional
    public void registerStudent(String sessionId, String studentId, String credentialId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new NoSuchElementException("Session not found: " + sessionId);
        }

        // Check if already registered
        boolean alreadyRegistered =
                registrationRepository.existsBySessionIdAndStudentId(sessionId, studentId);

        if (!alreadyRegistered) {
            IssuerRegistrationEntity registration = IssuerRegistrationEntity.builder()
                    .id(UUID.randomUUID().toString())
                    .sessionId(sessionId)
                    .studentId(studentId)
                    .email(studentId + "@students.example.edu")
                    .registeredAt(Instant.now())
                    .credentialId(credentialId)
                    .build();

            registrationRepository.save(registration);
            long total = registrationRepository.countBySessionId(sessionId);
            log.info("Registered student {} for session {} (total: {})", studentId, sessionId, total);
        } else {
            log.debug("Student {} already registered for session {}", studentId, sessionId);
        }
    }

    @Transactional(readOnly = true)
    public List<RegisteredStudent> getRegisteredStudents(String sessionId) {
        List<RegisteredStudent> students = new ArrayList<>();
        for (IssuerRegistrationEntity entity : registrationRepository.findBySessionId(sessionId)) {
            students.add(toDomain(entity));
        }
        return students;
    }

    @Deprecated
    public void incrementRegisteredCount(String sessionId) {
        // This method is kept for backward compatibility.
        // Registered counts are now derived from actual registrations, so this is a no-op.
        log.warn("Using deprecated incrementRegisteredCount - should use registerStudent instead");
    }

    // ---------------------------------------------------------------------
    // Entity <-> domain mapping
    // ---------------------------------------------------------------------

    private IssuerSessionEntity toEntity(Session session) {
        return IssuerSessionEntity.builder()
                .id(session.getId())
                .title(session.getTitle())
                .description(session.getDescription())
                .conferenceName(session.getConferenceName())
                .startTime(session.getStartTime() != null ? session.getStartTime().toString() : null)
                .endTime(session.getEndTime() != null ? session.getEndTime().toString() : null)
                .location(session.getLocation())
                .qrCodeUrl(session.getQrCodeUrl())
                .createdAt(session.getCreatedAt())
                .active(session.isActive())
                .build();
    }

    private Session toDomain(IssuerSessionEntity entity) {
        return Session.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .conferenceName(entity.getConferenceName())
                .startTime(parseOffsetDateTime(entity.getStartTime()))
                .endTime(parseOffsetDateTime(entity.getEndTime()))
                .location(entity.getLocation())
                .qrCodeUrl(entity.getQrCodeUrl())
                .createdAt(entity.getCreatedAt())
                .isActive(entity.isActive())
                .build();
    }

    private RegisteredStudent toDomain(IssuerRegistrationEntity entity) {
        return RegisteredStudent.builder()
                .id(entity.getId())
                .sessionId(entity.getSessionId())
                .studentId(entity.getStudentId())
                .studentName(entity.getStudentName())
                .email(entity.getEmail())
                .registeredAt(entity.getRegisteredAt())
                .credentialId(entity.getCredentialId())
                .build();
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception e) {
            log.warn("Failed to parse stored offset date-time: {}", value);
            return null;
        }
    }
}
