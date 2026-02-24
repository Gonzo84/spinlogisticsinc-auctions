-- ==========================================================================
-- V005: Add seller_id column to payments table
-- ==========================================================================
-- The seller_id is needed so that settlements can correctly identify the
-- seller who should receive the payout, rather than incorrectly using the
-- auction_id as a placeholder.
-- ==========================================================================

ALTER TABLE app.payments ADD COLUMN seller_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

-- Remove the default after backfill in production
ALTER TABLE app.payments ALTER COLUMN seller_id DROP DEFAULT;

-- Index for seller-scoped queries
CREATE INDEX idx_payments_seller ON app.payments(seller_id);
