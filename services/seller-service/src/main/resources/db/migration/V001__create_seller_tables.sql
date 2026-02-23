-- =============================================================================
-- V001: Create seller tables
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Seller Profiles
-- -----------------------------------------------------------------------------
CREATE TABLE app.seller_profiles (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID NOT NULL UNIQUE,
    company_name     TEXT NOT NULL,
    registration_no  TEXT,
    vat_id           TEXT,
    country          TEXT NOT NULL,
    status           TEXT NOT NULL DEFAULT 'PENDING',
    commission_rate  DECIMAL(5,4) DEFAULT 0.0500,
    verified_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_seller_user ON app.seller_profiles(user_id);

-- -----------------------------------------------------------------------------
-- Seller Metrics (dashboard KPIs)
-- -----------------------------------------------------------------------------
CREATE TABLE app.seller_metrics (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id             UUID NOT NULL REFERENCES app.seller_profiles(id),
    period                TEXT NOT NULL,
    active_lots           INT DEFAULT 0,
    total_bids            INT DEFAULT 0,
    hammer_sales          DECIMAL(15,2) DEFAULT 0,
    pending_settlements   DECIMAL(15,2) DEFAULT 0,
    total_settled         DECIMAL(15,2) DEFAULT 0,
    sell_through_rate     DECIMAL(5,4) DEFAULT 0,
    updated_at            TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(seller_id, period)
);

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
