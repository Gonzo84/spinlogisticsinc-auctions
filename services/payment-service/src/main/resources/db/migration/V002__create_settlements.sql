-- ==========================================================================
-- V002: Create the settlements table
-- ==========================================================================
-- Stores seller settlement (payout) records. A settlement is created
-- after a buyer payment is confirmed, capturing the net amount after
-- platform commission deduction.
-- ==========================================================================

CREATE TABLE app.settlements (
    id              UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    seller_id       UUID           NOT NULL,
    payment_id      UUID           NOT NULL REFERENCES app.payments(id),
    net_amount      DECIMAL(15,2)  NOT NULL,
    commission      DECIMAL(15,2)  NOT NULL,
    commission_rate DECIMAL(5,4)   NOT NULL,
    status          TEXT           NOT NULL DEFAULT 'PENDING',
    settled_at      TIMESTAMPTZ,
    bank_reference  TEXT,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Index for seller-scoped queries (my settlements)
CREATE INDEX idx_settlements_seller ON app.settlements(seller_id);

-- Index for status-based queries (batch processing)
CREATE INDEX idx_settlements_status ON app.settlements(status);

-- Ensure one settlement per payment
CREATE UNIQUE INDEX idx_settlements_payment ON app.settlements(payment_id);
