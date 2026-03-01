-- ==========================================================================
-- V006: Add enrichment columns to payments table
-- ==========================================================================
-- Stores denormalized names for display purposes, avoiding cross-service
-- lookups at query time. Populated when the LotAwardedEvent is consumed
-- or during checkout initiation.
-- ==========================================================================

ALTER TABLE app.payments ADD COLUMN IF NOT EXISTS lot_title TEXT;
ALTER TABLE app.payments ADD COLUMN IF NOT EXISTS buyer_name TEXT;
ALTER TABLE app.payments ADD COLUMN IF NOT EXISTS seller_name TEXT;
