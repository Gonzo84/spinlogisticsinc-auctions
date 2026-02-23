-- ==========================================================================
-- V001: Create the search_metadata table for NATS consumer tracking
-- ==========================================================================
-- Tracks the last successfully processed NATS sequence number per durable
-- consumer. Used to detect gaps or replay from a known checkpoint after
-- service restarts when the JetStream durable consumer state alone is not
-- sufficient (e.g. for audit or manual replay scenarios).
-- ==========================================================================

CREATE SCHEMA IF NOT EXISTS app;

CREATE TABLE app.search_metadata (
    id              BIGSERIAL       PRIMARY KEY,
    consumer_name   TEXT            NOT NULL UNIQUE,
    last_sequence   BIGINT          NOT NULL DEFAULT 0,
    last_subject    TEXT,
    last_event_id   TEXT,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  app.search_metadata IS 'Tracks the last processed NATS JetStream sequence per consumer for checkpoint and replay purposes.';
COMMENT ON COLUMN app.search_metadata.consumer_name IS 'Durable consumer name (e.g. search-catalog-consumer, search-auction-consumer).';
COMMENT ON COLUMN app.search_metadata.last_sequence IS 'Last successfully processed JetStream message sequence number.';
COMMENT ON COLUMN app.search_metadata.last_subject  IS 'NATS subject of the last processed message (for debugging).';
COMMENT ON COLUMN app.search_metadata.last_event_id IS 'Event ID of the last processed message (for idempotency checks).';

-- Index for quick lookups by consumer name (already covered by UNIQUE, but explicit for clarity)
CREATE INDEX idx_sm_consumer ON app.search_metadata(consumer_name);

-- Index for time-range queries (audit, monitoring dashboards)
CREATE INDEX idx_sm_updated ON app.search_metadata(updated_at);
