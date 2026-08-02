-- Durable store for workflow status (replaces the in-memory workflowStatuses map).
CREATE TABLE IF NOT EXISTS workflow_record (
    correlation_id VARCHAR(255) PRIMARY KEY,
    status         VARCHAR(255),
    progress       INTEGER,
    message        TEXT,
    error_code     VARCHAR(255),
    error_name     VARCHAR(255),
    error_message  TEXT,
    result         TEXT,
    timestamp      BIGINT,
    last_updated   BIGINT,
    version        BIGINT
);

CREATE INDEX IF NOT EXISTS idx_workflow_record_last_updated ON workflow_record (last_updated);
