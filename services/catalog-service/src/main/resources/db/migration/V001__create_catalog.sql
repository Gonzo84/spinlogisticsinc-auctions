-- =============================================================================
-- V001: Create core catalog-service tables
-- =============================================================================
-- Covers: categories, lots, lot_images, auction_events
-- Schema: app (application schema, separate from system/public)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- -----------------------------------------------------------------------------
-- Categories (hierarchical tree via parent_id)
-- -----------------------------------------------------------------------------
CREATE TABLE app.categories (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id  UUID        REFERENCES app.categories(id),
    name       VARCHAR(255) NOT NULL,
    slug       VARCHAR(255) NOT NULL UNIQUE,
    icon       VARCHAR(255),
    level      INT          NOT NULL DEFAULT 0,
    sort_order INT          NOT NULL DEFAULT 0,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Auction Events (grouping entity for lots)
-- -----------------------------------------------------------------------------
CREATE TABLE app.auction_events (
    id                    UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    title                 VARCHAR(500)   NOT NULL,
    brand                 VARCHAR(100)   NOT NULL,
    start_date            TIMESTAMPTZ    NOT NULL,
    end_date              TIMESTAMPTZ    NOT NULL,
    country               VARCHAR(3)     NOT NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    buyer_premium_percent DECIMAL(5,2)   NOT NULL DEFAULT 18.00,
    total_lots            INT            NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ    DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Lots (the core catalog items)
-- -----------------------------------------------------------------------------
CREATE TABLE app.lots (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id         UUID           NOT NULL,
    brand             VARCHAR(100)   NOT NULL,
    title             VARCHAR(500)   NOT NULL,
    description       TEXT           NOT NULL DEFAULT '',
    category_id       UUID           NOT NULL REFERENCES app.categories(id),
    specifications    JSONB          DEFAULT '{}'::jsonb,
    location_lat      DOUBLE PRECISION,
    location_lng      DOUBLE PRECISION,
    location_address  TEXT,
    location_country  VARCHAR(3)     NOT NULL,
    location_city     VARCHAR(100)   NOT NULL,
    reserve_price     DECIMAL(12,2),
    starting_bid      DECIMAL(12,2)  NOT NULL DEFAULT 1.00,
    auction_id        UUID           REFERENCES app.auction_events(id),
    status            VARCHAR(30)    NOT NULL DEFAULT 'DRAFT',
    co2_avoided_kg    DOUBLE PRECISION,
    pickup_info       TEXT,
    created_at        TIMESTAMPTZ    DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    DEFAULT NOW()
);

-- -----------------------------------------------------------------------------
-- Lot Images
-- -----------------------------------------------------------------------------
CREATE TABLE app.lot_images (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id         UUID         NOT NULL REFERENCES app.lots(id) ON DELETE CASCADE,
    image_url      VARCHAR(1024) NOT NULL,
    thumbnail_url  VARCHAR(1024),
    display_order  INT          NOT NULL DEFAULT 0,
    is_primary     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  DEFAULT NOW()
);

-- =============================================================================
-- Indexes
-- =============================================================================

-- Categories
CREATE INDEX idx_categories_parent   ON app.categories(parent_id);
CREATE INDEX idx_categories_slug     ON app.categories(slug);
CREATE INDEX idx_categories_level    ON app.categories(level, sort_order);

-- Auction Events
CREATE INDEX idx_auction_events_brand    ON app.auction_events(brand);
CREATE INDEX idx_auction_events_status   ON app.auction_events(status, start_date);
CREATE INDEX idx_auction_events_country  ON app.auction_events(country, status);

-- Lots
CREATE INDEX idx_lots_seller       ON app.lots(seller_id);
CREATE INDEX idx_lots_category     ON app.lots(category_id);
CREATE INDEX idx_lots_auction      ON app.lots(auction_id);
CREATE INDEX idx_lots_status       ON app.lots(status);
CREATE INDEX idx_lots_brand        ON app.lots(brand, status);
CREATE INDEX idx_lots_country      ON app.lots(location_country, status);
CREATE INDEX idx_lots_specs        ON app.lots USING gin (specifications);

-- Lot Images
CREATE INDEX idx_lot_images_lot    ON app.lot_images(lot_id, display_order);

-- =============================================================================
-- Seed data: ~30 major categories for EU B2B industrial auctions
-- =============================================================================

-- Level 0: Root categories
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('10000000-0000-0000-0000-000000000001', NULL, 'Construction Machinery',   'construction-machinery',   'icon-construction',   0,  1),
    ('10000000-0000-0000-0000-000000000002', NULL, 'Agriculture',              'agriculture',              'icon-agriculture',    0,  2),
    ('10000000-0000-0000-0000-000000000003', NULL, 'Transport & Logistics',    'transport-logistics',      'icon-transport',      0,  3),
    ('10000000-0000-0000-0000-000000000004', NULL, 'Metalworking',             'metalworking',             'icon-metalworking',   0,  4),
    ('10000000-0000-0000-0000-000000000005', NULL, 'Woodworking',              'woodworking',              'icon-woodworking',    0,  5),
    ('10000000-0000-0000-0000-000000000006', NULL, 'Food Processing',          'food-processing',          'icon-food',           0,  6),
    ('10000000-0000-0000-0000-000000000007', NULL, 'Printing & Packaging',     'printing-packaging',       'icon-printing',       0,  7),
    ('10000000-0000-0000-0000-000000000008', NULL, 'Medical Equipment',        'medical-equipment',        'icon-medical',        0,  8),
    ('10000000-0000-0000-0000-000000000009', NULL, 'IT & Electronics',         'it-electronics',           'icon-electronics',    0,  9),
    ('10000000-0000-0000-0000-000000000010', NULL, 'Office & Furniture',       'office-furniture',         'icon-office',         0, 10),
    ('10000000-0000-0000-0000-000000000011', NULL, 'Catering & Hospitality',   'catering-hospitality',     'icon-catering',       0, 11),
    ('10000000-0000-0000-0000-000000000012', NULL, 'Energy & Utilities',       'energy-utilities',         'icon-energy',         0, 12),
    ('10000000-0000-0000-0000-000000000013', NULL, 'Textile & Leather',        'textile-leather',          'icon-textile',        0, 13),
    ('10000000-0000-0000-0000-000000000014', NULL, 'Vehicles',                 'vehicles',                 'icon-vehicles',       0, 14),
    ('10000000-0000-0000-0000-000000000015', NULL, 'Real Estate',              'real-estate',              'icon-realestate',     0, 15);

-- Level 1: Subcategories under Construction Machinery
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000001', 'Excavators',             'excavators',             NULL, 1, 1),
    ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000001', 'Wheel Loaders',          'wheel-loaders',          NULL, 1, 2),
    ('20000000-0000-0000-0000-000000000003', '10000000-0000-0000-0000-000000000001', 'Cranes',                 'cranes',                 NULL, 1, 3),
    ('20000000-0000-0000-0000-000000000004', '10000000-0000-0000-0000-000000000001', 'Concrete Equipment',     'concrete-equipment',     NULL, 1, 4),
    ('20000000-0000-0000-0000-000000000005', '10000000-0000-0000-0000-000000000001', 'Compactors & Rollers',   'compactors-rollers',     NULL, 1, 5);

-- Level 1: Subcategories under Agriculture
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000006', '10000000-0000-0000-0000-000000000002', 'Tractors',               'tractors',               NULL, 1, 1),
    ('20000000-0000-0000-0000-000000000007', '10000000-0000-0000-0000-000000000002', 'Harvesters',             'harvesters',             NULL, 1, 2),
    ('20000000-0000-0000-0000-000000000008', '10000000-0000-0000-0000-000000000002', 'Sprayers',               'sprayers',               NULL, 1, 3),
    ('20000000-0000-0000-0000-000000000009', '10000000-0000-0000-0000-000000000002', 'Tillage Equipment',      'tillage-equipment',      NULL, 1, 4);

-- Level 1: Subcategories under Transport & Logistics
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000010', '10000000-0000-0000-0000-000000000003', 'Trucks',                 'trucks',                 NULL, 1, 1),
    ('20000000-0000-0000-0000-000000000011', '10000000-0000-0000-0000-000000000003', 'Trailers',               'trailers',               NULL, 1, 2),
    ('20000000-0000-0000-0000-000000000012', '10000000-0000-0000-0000-000000000003', 'Forklifts',              'forklifts',              NULL, 1, 3),
    ('20000000-0000-0000-0000-000000000013', '10000000-0000-0000-0000-000000000003', 'Warehouse Equipment',    'warehouse-equipment',    NULL, 1, 4);

-- Level 1: Subcategories under Metalworking
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000014', '10000000-0000-0000-0000-000000000004', 'CNC Machines',           'cnc-machines',           NULL, 1, 1),
    ('20000000-0000-0000-0000-000000000015', '10000000-0000-0000-0000-000000000004', 'Lathes',                 'lathes',                 NULL, 1, 2),
    ('20000000-0000-0000-0000-000000000016', '10000000-0000-0000-0000-000000000004', 'Milling Machines',       'milling-machines',       NULL, 1, 3),
    ('20000000-0000-0000-0000-000000000017', '10000000-0000-0000-0000-000000000004', 'Welding Equipment',      'welding-equipment',      NULL, 1, 4);

-- Level 1: Subcategories under Vehicles
INSERT INTO app.categories (id, parent_id, name, slug, icon, level, sort_order) VALUES
    ('20000000-0000-0000-0000-000000000018', '10000000-0000-0000-0000-000000000014', 'Passenger Cars',         'passenger-cars',         NULL, 1, 1),
    ('20000000-0000-0000-0000-000000000019', '10000000-0000-0000-0000-000000000014', 'Commercial Vans',        'commercial-vans',        NULL, 1, 2),
    ('20000000-0000-0000-0000-000000000020', '10000000-0000-0000-0000-000000000014', 'Motorcycles & Scooters', 'motorcycles-scooters',   NULL, 1, 3);
