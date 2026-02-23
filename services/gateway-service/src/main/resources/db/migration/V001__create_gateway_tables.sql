-- =============================================================================
-- V001: Create gateway tables
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Webhook Deduplication
-- -----------------------------------------------------------------------------
CREATE TABLE app.webhook_dedup (
    event_id     TEXT PRIMARY KEY,
    source       TEXT NOT NULL,
    received_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_webhook_received ON app.webhook_dedup(received_at);

-- -----------------------------------------------------------------------------
-- Casbin RBAC rules
-- -----------------------------------------------------------------------------
CREATE TABLE app.casbin_rule (
    id     BIGSERIAL PRIMARY KEY,
    ptype  TEXT NOT NULL DEFAULT 'p',
    v0     TEXT DEFAULT '',
    v1     TEXT DEFAULT '',
    v2     TEXT DEFAULT '',
    v3     TEXT DEFAULT '',
    v4     TEXT DEFAULT '',
    v5     TEXT DEFAULT ''
);
