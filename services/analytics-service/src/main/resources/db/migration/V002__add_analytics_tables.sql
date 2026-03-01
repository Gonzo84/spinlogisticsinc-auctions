-- =============================================================================
-- V002: Add category_metrics and bid_volume tables
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Category metrics table
-- Stores aggregated metrics per category. Updated periodically or via events.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app.category_metrics (
    category          TEXT            PRIMARY KEY,
    lot_count         INT             NOT NULL DEFAULT 0,
    bid_count         INT             NOT NULL DEFAULT 0,
    revenue           DECIMAL(15, 2)  NOT NULL DEFAULT 0.00,
    sell_through_rate DECIMAL(5, 4)   NOT NULL DEFAULT 0.0000,
    avg_price         DECIMAL(15, 2)  NOT NULL DEFAULT 0.00
);

-- -----------------------------------------------------------------------------
-- Bid volume table
-- Stores daily bid volume data. Upserted as bid events arrive.
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app.bid_volume (
    report_date    DATE    PRIMARY KEY,
    total_bids     BIGINT  NOT NULL DEFAULT 0,
    unique_bidders INT     NOT NULL DEFAULT 0
);
