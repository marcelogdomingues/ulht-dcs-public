-- W3C-style credential status registry (revocation / suspension).
--
-- Each row records the lifecycle status of an issued credential so a verifier
-- (or the /status-list endpoint) can determine whether a credential is still
-- VALID, has been REVOKED, or is temporarily SUSPENDED. The row's
-- status_list_index is the bit position of this credential in the published
-- W3C Bitstring Status List (https://www.w3.org/TR/vc-bitstring-status-list/).

CREATE TABLE IF NOT EXISTS credential_status (
    id                 VARCHAR(255) PRIMARY KEY,
    status_list_index  BIGINT       NOT NULL,
    credential_type    VARCHAR(255),
    subject_id         VARCHAR(255),
    status             VARCHAR(32)  NOT NULL,
    reason             TEXT,
    issued_at          TIMESTAMP,
    updated_at         TIMESTAMP
);

-- status_list_index is assigned by the application as MAX(index)+1 so it stays
-- portable across PostgreSQL (production) and H2 (tests) without a native sequence.
CREATE UNIQUE INDEX IF NOT EXISTS uq_credential_status_index ON credential_status (status_list_index);
CREATE INDEX IF NOT EXISTS idx_credential_status_status ON credential_status (status);
CREATE INDEX IF NOT EXISTS idx_credential_status_subject ON credential_status (subject_id);
