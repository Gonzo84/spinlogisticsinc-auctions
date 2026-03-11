-- =============================================================================
-- V003: Add payment_id column to seller_settlements
-- =============================================================================
-- Enables settlement status updates when PaymentSettledEvent arrives,
-- which references the originating payment UUID.
-- =============================================================================

ALTER TABLE app.seller_settlements ADD COLUMN IF NOT EXISTS payment_id UUID;

CREATE INDEX IF NOT EXISTS idx_seller_settlements_payment_id
    ON app.seller_settlements(payment_id);
