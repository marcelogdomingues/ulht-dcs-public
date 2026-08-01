package pt.ulusofona.ulht.credential.persistence;

/**
 * Lifecycle status of an issued credential in the status registry.
 *
 * <p>Maps to the W3C Bitstring Status List {@code statusPurpose}: {@link #REVOKED}
 * and {@link #SUSPENDED} set the credential's index bit in the published list,
 * while {@link #VALID} leaves it unset.</p>
 */
public enum CredentialStatus {
    VALID,
    REVOKED,
    SUSPENDED
}
