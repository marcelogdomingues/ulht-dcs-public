-- Idempotency guard for Kafka consumers.
-- Records the (topic, partition, offset) of every event a listener has fully
-- processed so a redelivered event (at-least-once delivery) is skipped instead
-- of being processed twice.

CREATE TABLE IF NOT EXISTS processed_event (
    event_key    VARCHAR(512) PRIMARY KEY,
    consumer     VARCHAR(255),
    processed_at TIMESTAMP
);
