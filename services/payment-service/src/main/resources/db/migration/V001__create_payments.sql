-- ==========================================================================
-- V001: Create the payments table
-- ==========================================================================
-- Stores payment records for won auction lots. Each payment captures the
-- hammer price, buyer premium, VAT calculation, and lifecycle status.
-- ==========================================================================

CREATE SCHEMA IF NOT EXISTS app;

CREATE TABLE app.payments (
    id                UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_id          UUID           NOT NULL,
    auction_id        UUID           NOT NULL,
    lot_id            UUID           NOT NULL,
    hammer_price      DECIMAL(15,2)  NOT NULL,
    buyer_premium     DECIMAL(15,2)  NOT NULL,
    buyer_premium_rate DECIMAL(5,4)  NOT NULL,
    vat_amount        DECIMAL(15,2)  NOT NULL,
    vat_rate          DECIMAL(5,4)   NOT NULL,
    vat_scheme        TEXT           NOT NULL,
    total_amount      DECIMAL(15,2)  NOT NULL,
    currency          TEXT           NOT NULL DEFAULT 'EUR',
    country           TEXT           NOT NULL,
    payment_method    TEXT,
    psp_reference     TEXT,
    status            TEXT           NOT NULL DEFAULT 'PENDING',
    due_date          TIMESTAMPTZ    NOT NULL,
    paid_at           TIMESTAMPTZ,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Index for buyer-scoped queries (my payments)
CREATE INDEX idx_payments_buyer ON app.payments(buyer_id);

-- Index for lot-scoped queries (payment for a specific lot)
CREATE INDEX idx_payments_lot ON app.payments(lot_id);

-- Index for status-based queries (admin dashboards, batch processing)
CREATE INDEX idx_payments_status ON app.payments(status);

-- Partial index for overdue payment detection (only PENDING payments)
CREATE INDEX idx_payments_due ON app.payments(due_date) WHERE status = 'PENDING';
