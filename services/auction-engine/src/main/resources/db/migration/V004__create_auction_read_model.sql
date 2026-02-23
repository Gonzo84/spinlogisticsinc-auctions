-- ==========================================================================
-- V004: Create the auction_read_model table (CQRS query side)
-- ==========================================================================
-- Denormalized projection of auction state optimised for read-heavy query
-- patterns: listing active auctions, lookup by ID/lot, filtering by
-- status and brand, and identifying auctions that are about to close.
-- ==========================================================================

CREATE TABLE app.auction_read_model (
    auction_id            UUID           PRIMARY KEY,
    lot_id                UUID           NOT NULL,
    brand                 TEXT           NOT NULL,
    status                TEXT           NOT NULL,
    start_time            TIMESTAMPTZ    NOT NULL,
    end_time              TIMESTAMPTZ    NOT NULL,
    original_end_time     TIMESTAMPTZ    NOT NULL,
    starting_bid          DECIMAL(15,2),
    current_high_bid      DECIMAL(15,2),
    current_high_bidder_id UUID,
    bid_count             INT            DEFAULT 0,
    reserve_met           BOOLEAN        DEFAULT FALSE,
    extension_count       INT            DEFAULT 0,
    seller_id             UUID           NOT NULL,
    created_at            TIMESTAMPTZ    DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    DEFAULT NOW()
);

-- Filter by auction lifecycle status
CREATE INDEX idx_arm_status ON app.auction_read_model(status);

-- Find auctions closing soon (used by the closing scheduler)
CREATE INDEX idx_arm_end_time ON app.auction_read_model(end_time);

-- Lookup by lot identifier
CREATE INDEX idx_arm_lot ON app.auction_read_model(lot_id);

-- Brand / tenant scoped queries
CREATE INDEX idx_arm_brand ON app.auction_read_model(brand);
