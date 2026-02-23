-- =============================================================================
-- CO2 Service Schema
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Emission factors table
-- Reference data for CO2 avoidance calculations per product category.
-- -----------------------------------------------------------------------------
CREATE TABLE app.emission_factors (
    id                       UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id              UUID          NOT NULL UNIQUE,
    product_type             TEXT          NOT NULL,
    new_manufacturing_co2_kg DECIMAL(12, 2) NOT NULL,
    reuse_factor             DECIMAL(4, 2)  NOT NULL DEFAULT 0.85,
    source                   TEXT          NOT NULL DEFAULT 'EU EF Database 3.1',
    last_updated             TIMESTAMPTZ   DEFAULT NOW()
);

CREATE INDEX idx_emission_factors_category ON app.emission_factors(category_id);
CREATE INDEX idx_emission_factors_type     ON app.emission_factors(product_type);

-- -----------------------------------------------------------------------------
-- CO2 calculations table
-- Stores individual CO2 avoidance calculations per lot.
-- Multiple versions may exist per lot when recalculations occur.
-- -----------------------------------------------------------------------------
CREATE TABLE app.co2_calculations (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id          UUID          NOT NULL,
    category_id     UUID          NOT NULL,
    co2_avoided_kg  DECIMAL(12, 2) NOT NULL,
    calculated_at   TIMESTAMPTZ   DEFAULT NOW(),
    version         INT           NOT NULL DEFAULT 1
);

CREATE INDEX idx_co2_calc_lot     ON app.co2_calculations(lot_id);
CREATE INDEX idx_co2_calc_cat     ON app.co2_calculations(category_id);
CREATE INDEX idx_co2_calc_lot_ver ON app.co2_calculations(lot_id, version DESC);

-- -----------------------------------------------------------------------------
-- Lot-to-seller mapping table
-- Used by seller-level CO2 queries to associate calculations with sellers.
-- Populated by event consumers when lots are created.
-- -----------------------------------------------------------------------------
CREATE TABLE app.co2_lot_seller_mapping (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id UUID NOT NULL,
    CONSTRAINT uq_lot_seller UNIQUE (id)
);

CREATE INDEX idx_co2_lot_seller ON app.co2_lot_seller_mapping(seller_id);

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

-- =============================================================================
-- Seed Data: Emission Factors for 15+ Equipment Categories
--
-- Sources: EU Environmental Footprint Database 3.1, LCA studies, industry
-- averages. reuse_factor represents the fraction of CO2 avoided when an
-- existing unit is auctioned for reuse rather than scrapped.
-- =============================================================================

INSERT INTO app.emission_factors (id, category_id, product_type, new_manufacturing_co2_kg, reuse_factor, source) VALUES
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000001', 'Excavator',       45000.00, 0.85, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000002', 'Wheel Loader',    35000.00, 0.85, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000003', 'Truck',           25000.00, 0.80, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000004', 'Crane',           80000.00, 0.90, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000005', 'Forklift',         8000.00, 0.82, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000006', 'Generator',        5000.00, 0.78, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000007', 'Compressor',       3000.00, 0.75, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000008', 'Tractor',         20000.00, 0.83, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000009', 'Bus',             40000.00, 0.88, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000010', 'Van',             12000.00, 0.78, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000011', 'Bulldozer',       55000.00, 0.87, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000012', 'Concrete Mixer',  15000.00, 0.80, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000013', 'Drilling Rig',    70000.00, 0.92, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000014', 'Road Roller',     18000.00, 0.82, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000015', 'Trailer',          6000.00, 0.70, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000016', 'Dump Truck',      30000.00, 0.83, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000017', 'Paving Machine',  22000.00, 0.85, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000018', 'Telehandler',     16000.00, 0.81, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000019', 'Mini Excavator',  10000.00, 0.80, 'EU EF Database 3.1'),
    (gen_random_uuid(), '00000000-0000-0000-0001-000000000020', 'Aerial Platform',  9000.00, 0.79, 'EU EF Database 3.1');
