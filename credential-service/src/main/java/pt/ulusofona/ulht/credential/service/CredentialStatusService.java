package pt.ulusofona.ulht.credential.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.ulusofona.ulht.credential.persistence.CredentialStatus;
import pt.ulusofona.ulht.credential.persistence.CredentialStatusEntity;
import pt.ulusofona.ulht.credential.persistence.CredentialStatusRepository;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

/**
 * Credential status registry service.
 *
 * <p>Tracks the lifecycle status (VALID / REVOKED / SUSPENDED) of every issued
 * credential and renders the set of revoked/suspended credentials as a
 * <a href="https://www.w3.org/TR/vc-bitstring-status-list/">W3C Bitstring Status
 * List</a>: a bitstring, one bit per credential index, GZIP-compressed and
 * base64url-encoded. A verifier fetches the list, inflates it, and checks the
 * bit at the credential's {@code statusListIndex}.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CredentialStatusService {

    /**
     * Minimum bitstring length in bits. The W3C spec recommends a herd-privacy
     * minimum of 131,072 entries (16 KB) so the presence of a specific index in
     * the list does not leak which credential it belongs to.
     */
    static final int MINIMUM_BITSTRING_LENGTH = 131_072;

    private final CredentialStatusRepository repository;

    /**
     * Records a newly issued credential as VALID. Idempotent: if a row already
     * exists for {@code credentialId} it is returned unchanged.
     */
    @Transactional
    public CredentialStatusEntity record(String credentialId, String credentialType, String subjectId) {
        String id = (credentialId == null || credentialId.isBlank())
                ? UUID.randomUUID().toString()
                : credentialId;

        Optional<CredentialStatusEntity> existing = repository.findById(id);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant now = Instant.now();
        CredentialStatusEntity entity = CredentialStatusEntity.builder()
                .id(id)
                .statusListIndex(nextIndex())
                .credentialType(credentialType)
                .subjectId(subjectId)
                .status(CredentialStatus.VALID)
                .issuedAt(now)
                .updatedAt(now)
                .build();

        CredentialStatusEntity saved = repository.save(entity);
        log.info("📄 Recorded credential status VALID id={} index={} type={}",
                saved.getId(), saved.getStatusListIndex(), credentialType);
        return saved;
    }

    /** Marks a credential REVOKED (creating a row if it does not yet exist). */
    @Transactional
    public CredentialStatusEntity revoke(String credentialId, String reason) {
        return transition(credentialId, CredentialStatus.REVOKED, reason);
    }

    /** Temporarily suspends a credential. */
    @Transactional
    public CredentialStatusEntity suspend(String credentialId, String reason) {
        return transition(credentialId, CredentialStatus.SUSPENDED, reason);
    }

    /** Reactivates a suspended credential back to VALID. */
    @Transactional
    public CredentialStatusEntity reactivate(String credentialId) {
        return transition(credentialId, CredentialStatus.VALID, null);
    }

    /** Current status row for a credential, if known. */
    @Transactional(readOnly = true)
    public Optional<CredentialStatusEntity> getStatus(String credentialId) {
        return repository.findById(credentialId);
    }

    private CredentialStatusEntity transition(String credentialId, CredentialStatus status, String reason) {
        CredentialStatusEntity entity = repository.findById(credentialId)
                .orElseGet(() -> CredentialStatusEntity.builder()
                        .id(credentialId)
                        .statusListIndex(nextIndex())
                        .status(CredentialStatus.VALID)
                        .issuedAt(Instant.now())
                        .build());
        entity.setStatus(status);
        entity.setReason(reason);
        entity.setUpdatedAt(Instant.now());
        CredentialStatusEntity saved = repository.save(entity);
        log.info("📄 Credential {} -> {} (index={}, reason={})",
                credentialId, status, saved.getStatusListIndex(), reason);
        return saved;
    }

    private long nextIndex() {
        Long max = repository.findMaxIndex();
        return max == null ? 0L : max + 1L;
    }

    /**
     * Builds the {@code encodedList} for the Bitstring Status List: sets the bit at
     * each revoked/suspended credential's index, GZIP-compresses the bitstring and
     * base64url-encodes it (unpadded), per the W3C spec.
     */
    @Transactional(readOnly = true)
    public String buildEncodedList() {
        List<CredentialStatusEntity> revoked = repository.findByStatus(CredentialStatus.REVOKED);
        List<CredentialStatusEntity> suspended = repository.findByStatus(CredentialStatus.SUSPENDED);

        long maxIndex = -1L;
        for (CredentialStatusEntity e : revoked) {
            maxIndex = Math.max(maxIndex, e.getStatusListIndex());
        }
        for (CredentialStatusEntity e : suspended) {
            maxIndex = Math.max(maxIndex, e.getStatusListIndex());
        }

        int lengthInBits = Math.max(MINIMUM_BITSTRING_LENGTH, (int) (maxIndex + 1));
        byte[] bitstring = new byte[(lengthInBits + 7) / 8];

        for (CredentialStatusEntity e : revoked) {
            setBit(bitstring, e.getStatusListIndex());
        }
        for (CredentialStatusEntity e : suspended) {
            setBit(bitstring, e.getStatusListIndex());
        }

        return gzipBase64Url(bitstring);
    }

    /**
     * Sets the bit for a given index. The W3C Bitstring Status List numbers bits
     * from the most-significant bit of the first byte (big-endian within each byte).
     */
    private static void setBit(byte[] bitstring, long index) {
        int byteIndex = (int) (index / 8);
        int bitInByte = (int) (index % 8);
        bitstring[byteIndex] |= (byte) (0x80 >>> bitInByte);
    }

    private static String gzipBase64Url(byte[] data) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
            gzip.write(data);
            gzip.finish();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encode Bitstring Status List", e);
        }
    }
}
