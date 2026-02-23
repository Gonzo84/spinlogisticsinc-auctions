-- ==========================================================================
-- V001: Create the auction_events event-store table
-- ==========================================================================
-- Stores the immutable event stream for Auction aggregates. Each row is a
-- serialised domain event with version-based optimistic concurrency control.
-- ==========================================================================

CREATE SCHEMA IF NOT EXISTS app;

CREATE TABLE app.auction_events (
    event_id       UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_id   UUID         NOT NULL,
    aggregate_type TEXT         NOT NULL DEFAULT 'Auction',
    event_type     TEXT         NOT NULL,
    event_data     TEXT         NOT NULL,
    version        BIGINT       NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    brand          TEXT         NOT NULL,
    metadata       TEXT,
    UNIQUE (aggregate_id, version)
);

-- Primary query path: rehydrate an aggregate by replaying events in order
CREATE INDEX idx_ae_aggregate ON app.auction_events(aggregate_id, version);

-- Event-type filtering for projections and analytics
CREATE INDEX idx_ae_type ON app.auction_events(event_type);

-- Brand / tenant scoped queries
CREATE INDEX idx_ae_brand ON app.auction_events(brand);

-- Time-range queries for auditing and replay
CREATE INDEX idx_ae_created ON app.auction_events(created_at);
