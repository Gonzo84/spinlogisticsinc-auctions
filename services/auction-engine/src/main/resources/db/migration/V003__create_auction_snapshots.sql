-- ==========================================================================
-- V003: Create the auction_snapshots table
-- ==========================================================================
-- Stores periodic snapshots of auction aggregate state to avoid full event
-- replay on every load. One snapshot per aggregate (upsert on aggregate_id).
-- ==========================================================================

CREATE TABLE app.auction_snapshots (
    aggregate_id  UUID         PRIMARY KEY,
    version       BIGINT       NOT NULL,
    state         TEXT         NOT NULL,
    created_at    TIMESTAMPTZ  DEFAULT NOW()
);
