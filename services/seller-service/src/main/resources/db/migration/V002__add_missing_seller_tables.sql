-- =============================================================================
-- V002: Add missing seller tables and fix column mismatches
-- =============================================================================
-- Resolves:
--   ERROR: column "total_hammer_sales" does not exist (in seller dashboard query)
--   ERROR: relation "app.seller_lots" does not exist
--   ERROR: relation "app.seller_settlements" does not exist
--   ERROR: relation "app.seller_co2" does not exist
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Fix seller_metrics: rename hammer_sales -> total_hammer_sales
--    The repository (SellerProfileRepository) references "total_hammer_sales"
--    in SELECT_DASHBOARD, UPSERT_METRICS, ADD_HAMMER_SALE queries, but V001
--    created the column as "hammer_sales".
-- -----------------------------------------------------------------------------
ALTER TABLE app.seller_metrics
    RENAME COLUMN hammer_sales TO total_hammer_sales;

-- -----------------------------------------------------------------------------
-- 2. Fix seller_metrics: change pending_settlements from DECIMAL to INT
--    The repository uses setInt/getInt and treats pending_settlements as a
--    counter (pending_settlements + 1, pending_settlements - 1), not a
--    monetary amount.
-- -----------------------------------------------------------------------------
ALTER TABLE app.seller_metrics
    ALTER COLUMN pending_settlements TYPE INT USING pending_settlements::INT;

-- -----------------------------------------------------------------------------
-- 3. Add unique index on seller_id alone for seller_metrics
--    The repository UPSERT_METRICS uses ON CONFLICT (seller_id), but V001
--    only has UNIQUE(seller_id, period). We need a unique index on seller_id
--    alone so the upsert works. We also add a WHERE period = 'ALL' partial
--    unique index to avoid conflict with the existing composite unique.
--    Since the repository always operates on the single "live" row per seller
--    (no period filter in SELECT_DASHBOARD), we add the index unconditionally.
-- -----------------------------------------------------------------------------
CREATE UNIQUE INDEX IF NOT EXISTS idx_seller_metrics_seller_id
    ON app.seller_metrics(seller_id);

-- -----------------------------------------------------------------------------
-- 4. Create seller_lots table
--    Referenced by: SELECT_LOTS_BY_SELLER, COUNT_LOTS_BY_SELLER,
--    SELECT_LOT_BY_ID_AND_SELLER, UPDATE_LOT_ACCEPT_BELOW_RESERVE,
--    UPDATE_LOT_RELIST in SellerService.
--
--    This is a local projection of lot data owned by the catalog-service,
--    populated via NATS events (catalog.lot.created, auction.lot.closed, etc.).
-- -----------------------------------------------------------------------------
CREATE TABLE app.seller_lots (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL REFERENCES app.seller_profiles(id),
    title           TEXT NOT NULL DEFAULT '',
    status          TEXT NOT NULL DEFAULT 'ACTIVE',
    current_bid     DECIMAL(15,2),
    reserve_price   DECIMAL(15,2),
    bid_count       INT NOT NULL DEFAULT 0,
    closing_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_lots_seller_id ON app.seller_lots(seller_id);
CREATE INDEX idx_seller_lots_status ON app.seller_lots(status);
CREATE INDEX idx_seller_lots_created_at ON app.seller_lots(created_at DESC);

-- -----------------------------------------------------------------------------
-- 5. Create seller_settlements table
--    Referenced by: SELECT_SETTLEMENTS_BY_SELLER in SellerService.
--
--    Tracks settlement records for lots won at auction. Populated via NATS
--    events (payment.settlement.ready) and internal settlement processing.
-- -----------------------------------------------------------------------------
CREATE TABLE app.seller_settlements (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL REFERENCES app.seller_profiles(id),
    lot_id          UUID NOT NULL,
    lot_title       TEXT,
    hammer_price    DECIMAL(15,2) NOT NULL DEFAULT 0,
    commission      DECIMAL(15,2) NOT NULL DEFAULT 0,
    net_amount      DECIMAL(15,2) NOT NULL DEFAULT 0,
    currency        TEXT NOT NULL DEFAULT 'EUR',
    status          TEXT NOT NULL DEFAULT 'PENDING',
    settled_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_settlements_seller_id ON app.seller_settlements(seller_id);
CREATE INDEX idx_seller_settlements_status ON app.seller_settlements(status);
CREATE INDEX idx_seller_settlements_settled_at ON app.seller_settlements(settled_at DESC NULLS LAST);

-- -----------------------------------------------------------------------------
-- 6. Create seller_co2 table
--    Referenced by: SELECT_CO2_BY_SELLER in SellerService.
--
--    Stores per-lot CO2 emission savings data for each seller, populated via
--    NATS events from the co2-service.
-- -----------------------------------------------------------------------------
CREATE TABLE app.seller_co2 (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID NOT NULL REFERENCES app.seller_profiles(id),
    lot_id          UUID,
    co2_saved_kg    DECIMAL(12,4) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_seller_co2_seller_id ON app.seller_co2(seller_id);
