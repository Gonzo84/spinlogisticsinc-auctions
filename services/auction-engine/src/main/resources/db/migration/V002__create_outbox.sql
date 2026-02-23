-- ==========================================================================
-- V002: Create the transactional outbox table
-- ==========================================================================
-- Implements the outbox pattern for reliable at-least-once event delivery
-- to NATS JetStream. Events are written to this table in the same DB
-- transaction as the event store append. A background poller publishes
-- pending entries and marks them as delivered.
-- ==========================================================================

CREATE TABLE app.outbox (
    id            BIGSERIAL    PRIMARY KEY,
    aggregate_id  UUID         NOT NULL,
    event_type    TEXT         NOT NULL,
    payload       TEXT         NOT NULL,
    nats_subject  TEXT         NOT NULL,
    published     BOOLEAN      DEFAULT FALSE,
    created_at    TIMESTAMPTZ  DEFAULT NOW(),
    published_at  TIMESTAMPTZ,
    retry_count   INT          DEFAULT 0,
    dead_letter   BOOLEAN      DEFAULT FALSE
);

-- Partial index for the polling query: only unpublished, non-DLQ entries
CREATE INDEX idx_outbox_pending
    ON app.outbox(published, created_at)
    WHERE published = FALSE AND dead_letter = FALSE;
