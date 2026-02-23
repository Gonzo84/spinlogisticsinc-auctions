-- =============================================================================
-- Analytics Service Schema
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Platform metrics table
-- Stores periodic snapshots of platform-wide metrics.
-- Each row is an immutable snapshot; the latest row is the "current" state.
-- -----------------------------------------------------------------------------
CREATE TABLE app.platform_metrics (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    active_auctions   INT           NOT NULL DEFAULT 0,
    total_bids_24h    BIGINT        NOT NULL DEFAULT 0,
    total_revenue_30d DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    registered_users  BIGINT        NOT NULL DEFAULT 0,
    active_buyers     BIGINT        NOT NULL DEFAULT 0,
    active_sellers    BIGINT        NOT NULL DEFAULT 0,
    calculated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_platform_metrics_time ON app.platform_metrics(calculated_at DESC);

-- -----------------------------------------------------------------------------
-- Auction metrics table
-- Stores aggregated metrics per auction. Updated incrementally as events arrive.
-- -----------------------------------------------------------------------------
CREATE TABLE app.auction_metrics (
    auction_id       UUID          PRIMARY KEY,
    total_bids       BIGINT        NOT NULL DEFAULT 0,
    unique_bidders   INT           NOT NULL DEFAULT 0,
    avg_bid_amount   DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    max_bid          DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    extension_count  INT           NOT NULL DEFAULT 0,
    duration_seconds BIGINT        NOT NULL DEFAULT 0
);

-- -----------------------------------------------------------------------------
-- Daily revenue table
-- Stores aggregated revenue data per day. Upserted as checkout events arrive.
-- -----------------------------------------------------------------------------
CREATE TABLE app.daily_revenue (
    report_date       DATE          PRIMARY KEY,
    revenue_eur       DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    transaction_count INT           NOT NULL DEFAULT 0,
    avg_transaction_eur DECIMAL(15, 2) NOT NULL DEFAULT 0.00
);

-- -----------------------------------------------------------------------------
-- User growth table
-- Stores daily user registration and growth data.
-- -----------------------------------------------------------------------------
CREATE TABLE app.user_growth (
    report_date       DATE          PRIMARY KEY,
    new_registrations INT           NOT NULL DEFAULT 0,
    total_users       BIGINT        NOT NULL DEFAULT 0,
    new_buyers        INT           NOT NULL DEFAULT 0,
    new_sellers       INT           NOT NULL DEFAULT 0
);

-- -----------------------------------------------------------------------------
-- Casbin rule table
-- Used by the Casbin RBAC/ABAC policy engine.
-- -----------------------------------------------------------------------------
CREATE TABLE app.casbin_rule (
    id    BIGSERIAL PRIMARY KEY,
    ptype TEXT      NOT NULL DEFAULT 'p',
    v0    TEXT      DEFAULT '',
    v1    TEXT      DEFAULT '',
    v2    TEXT      DEFAULT '',
    v3    TEXT      DEFAULT '',
    v4    TEXT      DEFAULT '',
    v5    TEXT      DEFAULT ''
);
