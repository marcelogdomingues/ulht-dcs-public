package pt.ulusofona.ulht.credential.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity that durably stores a student registration for an issuer session,
 * replacing the in-memory {@code registeredStudents} map (keyed by session id)
 * previously held in {@code SessionService}.
 *
 * <p>Registrations reference their session via {@code sessionId}; the number of
 * registrations for a session is the session's registered count (derived, never
 * stored on the session).</p>
 */
@Entity
@Table(name = "issuer_registration",
        indexes = @Index(name = "idx_issuer_registration_session", columnList = "session_id"))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuerRegistrationEntity {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "student_id")
    private String studentId;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "email")
    private String email;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @Column(name = "credential_id")
    private String credentialId;
}
