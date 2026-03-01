-- =============================================================================
-- V004: Add user activity projection tables (bids, purchases, watchlist)
-- =============================================================================
-- These tables are populated by NATS consumers projecting auction and payment
-- domain events into user-specific read models.
-- =============================================================================

CREATE TABLE app.user_bids (
    id         BIGSERIAL       PRIMARY KEY,
    user_id    UUID            NOT NULL,
    auction_id UUID            NOT NULL,
    lot_id     UUID,
    amount     NUMERIC(15,2)   NOT NULL,
    currency   VARCHAR(3)      DEFAULT 'EUR',
    bid_at     TIMESTAMPTZ     NOT NULL,
    UNIQUE(user_id, auction_id, bid_at)
);
CREATE INDEX idx_user_bids_user ON app.user_bids(user_id);

CREATE TABLE app.user_purchases (
    id            BIGSERIAL       PRIMARY KEY,
    user_id       UUID            NOT NULL,
    lot_id        UUID            NOT NULL,
    auction_id    UUID,
    hammer_price  NUMERIC(15,2),
    currency      VARCHAR(3)      DEFAULT 'EUR',
    status        VARCHAR(32)     DEFAULT 'PENDING_PAYMENT',
    awarded_at    TIMESTAMPTZ     NOT NULL,
    UNIQUE(user_id, lot_id)
);
CREATE INDEX idx_user_purchases_user ON app.user_purchases(user_id);

CREATE TABLE app.user_watchlist (
    id       BIGSERIAL       PRIMARY KEY,
    user_id  UUID            NOT NULL,
    lot_id   UUID            NOT NULL,
    added_at TIMESTAMPTZ     DEFAULT NOW(),
    UNIQUE(user_id, lot_id)
);
CREATE INDEX idx_user_watchlist_user ON app.user_watchlist(user_id);
