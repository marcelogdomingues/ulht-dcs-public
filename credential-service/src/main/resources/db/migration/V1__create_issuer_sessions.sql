-- Durable store for issuer sessions and their student registrations
-- (replaces the in-memory sessions / registeredStudents maps in SessionService).

CREATE TABLE IF NOT EXISTS issuer_session (
    id              VARCHAR(255) PRIMARY KEY,
    title           VARCHAR(255),
    description     TEXT,
    conference_name VARCHAR(255),
    start_time      VARCHAR(64),
    end_time        VARCHAR(64),
    location        VARCHAR(255),
    qr_code_url     TEXT,
    created_at      TIMESTAMP,
    is_active       BOOLEAN
);

CREATE TABLE IF NOT EXISTS issuer_registration (
    id            VARCHAR(255) PRIMARY KEY,
    session_id    VARCHAR(255) NOT NULL,
    student_id    VARCHAR(255),
    student_name  VARCHAR(255),
    email         VARCHAR(255),
    registered_at TIMESTAMP,
    credential_id VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_issuer_registration_session ON issuer_registration (session_id);
