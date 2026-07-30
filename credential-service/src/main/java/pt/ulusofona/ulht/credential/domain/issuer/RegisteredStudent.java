package pt.ulusofona.ulht.credential.domain.issuer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents a student registered for a session.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisteredStudent {
    private String id;
    private String sessionId;
    private String studentId;
    private String studentName;
    private String email;
    private Instant registeredAt;
    private String credentialId;
}

