package pt.ulusofona.ulht.credential.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity backing the credential status registry.
 *
 * <p>One row is written per issued credential. The {@code statusListIndex} is the
 * credential's bit position in the published W3C Bitstring Status List
 * (https://www.w3.org/TR/vc-bitstring-status-list/); when the credential is
 * revoked or suspended that bit is set in the encoded list.</p>
 */
@Entity
@Table(name = "credential_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialStatusEntity {

    /** Credential id / status-list identifier (a VC id or generated UUID). */
    @Id
    @Column(name = "id", nullable = false, length = 255)
    private String id;

    /** Bit position of this credential in the Bitstring Status List. */
    @Column(name = "status_list_index", nullable = false)
    private long statusListIndex;

    @Column(name = "credential_type")
    private String credentialType;

    @Column(name = "subject_id")
    private String subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private CredentialStatus status;

    @Column(name = "reason")
    private String reason;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
