package com.example.dcs.credential.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import com.example.dcs.credential.persistence.CredentialStatus;
import com.example.dcs.credential.persistence.CredentialStatusEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the credential status registry against an in-memory H2 database:
 * the record → revoke → getStatus flow and the W3C Bitstring Status List encoding
 * (gzip + base64url, correct bit set for the revoked credential's index).
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(CredentialStatusService.class)
class CredentialStatusServiceTest {

    @Autowired
    private CredentialStatusService service;

    @Test
    void recordThenRevoke_updatesStatusAndReason() {
        service.record("cred-1", "UniversityDegree", "did:jwk:subject-1");

        Optional<CredentialStatusEntity> afterRecord = service.getStatus("cred-1");
        assertThat(afterRecord).isPresent();
        assertThat(afterRecord.get().getStatus()).isEqualTo(CredentialStatus.VALID);
        assertThat(afterRecord.get().getCredentialType()).isEqualTo("UniversityDegree");

        service.revoke("cred-1", "student withdrew");

        Optional<CredentialStatusEntity> afterRevoke = service.getStatus("cred-1");
        assertThat(afterRevoke).isPresent();
        assertThat(afterRevoke.get().getStatus()).isEqualTo(CredentialStatus.REVOKED);
        assertThat(afterRevoke.get().getReason()).isEqualTo("student withdrew");
        assertThat(afterRevoke.get().getUpdatedAt()).isNotNull();
    }

    @Test
    void record_assignsMonotonicIndexes() {
        long i0 = service.record("a", "T", "s").getStatusListIndex();
        long i1 = service.record("b", "T", "s").getStatusListIndex();
        long i2 = service.record("c", "T", "s").getStatusListIndex();

        assertThat(i0).isZero();
        assertThat(i1).isEqualTo(1L);
        assertThat(i2).isEqualTo(2L);
    }

    @Test
    void record_isIdempotent() {
        CredentialStatusEntity first = service.record("dup", "T", "s");
        CredentialStatusEntity second = service.record("dup", "T", "s");
        assertThat(second.getStatusListIndex()).isEqualTo(first.getStatusListIndex());
    }

    @Test
    void buildEncodedList_setsBitForRevokedCredentialOnly() throws Exception {
        // index 0 stays VALID, index 1 gets revoked
        service.record("valid-cred", "T", "s");
        CredentialStatusEntity revoked = service.record("revoked-cred", "T", "s");
        service.revoke("revoked-cred", "test");

        String encoded = service.buildEncodedList();
        assertThat(encoded).isNotBlank();

        byte[] bitstring = inflate(encoded);

        // Bitstring must cover the herd-privacy minimum (131072 bits = 16384 bytes).
        assertThat(bitstring.length).isEqualTo(16_384);

        assertThat(isBitSet(bitstring, 0)).isFalse();
        assertThat(isBitSet(bitstring, revoked.getStatusListIndex())).isTrue();
    }

    private static byte[] inflate(String encodedBase64Url) throws Exception {
        byte[] gzipped = Base64.getUrlDecoder().decode(encodedBase64Url);
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(gzipped));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            gzip.transferTo(out);
            return out.toByteArray();
        }
    }

    private static boolean isBitSet(byte[] bitstring, long index) {
        int byteIndex = (int) (index / 8);
        int bitInByte = (int) (index % 8);
        return (bitstring[byteIndex] & (0x80 >>> bitInByte)) != 0;
    }
}
