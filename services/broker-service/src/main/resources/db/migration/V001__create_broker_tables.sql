-- =============================================================================
-- Broker Service Schema
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Leads table
-- Tracks sales leads assigned to brokers through the full lifecycle.
-- -----------------------------------------------------------------------------
CREATE TABLE app.leads (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id            UUID        NOT NULL,
    broker_id            UUID        NOT NULL,
    company_name         TEXT        NOT NULL,
    contact_name         TEXT        NOT NULL,
    contact_email        TEXT        NOT NULL,
    contact_phone        TEXT,
    status               TEXT        NOT NULL DEFAULT 'NEW',
    notes                TEXT,
    scheduled_visit_date TIMESTAMPTZ,
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    updated_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_leads_broker ON app.leads(broker_id);
CREATE INDEX idx_leads_seller ON app.leads(seller_id);
CREATE INDEX idx_leads_status ON app.leads(status);

-- -----------------------------------------------------------------------------
-- Lot intakes table
-- Records lot details captured by brokers during site visits.
-- -----------------------------------------------------------------------------
CREATE TABLE app.lot_intakes (
    id                UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    broker_id         UUID            NOT NULL,
    seller_id         UUID            NOT NULL,
    lead_id           UUID            NOT NULL,
    title             TEXT            NOT NULL,
    category_id       UUID,
    description       TEXT,
    specifications    JSONB,
    reserve_price     DECIMAL(15, 2),
    location_address  TEXT,
    location_country  TEXT,
    location_lat      DOUBLE PRECISION,
    location_lng      DOUBLE PRECISION,
    image_keys        JSONB,
    status            TEXT            DEFAULT 'DRAFT',
    created_at        TIMESTAMPTZ     DEFAULT NOW()
);

CREATE INDEX idx_intakes_broker ON app.lot_intakes(broker_id);
CREATE INDEX idx_intakes_lead   ON app.lot_intakes(lead_id);
CREATE INDEX idx_intakes_seller ON app.lot_intakes(seller_id);

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
