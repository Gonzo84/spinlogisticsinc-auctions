-- =============================================================================
-- V005: Rename vatId/country columns to ein/state for US market adaptation
-- =============================================================================

-- Rename vat_id -> ein in seller_profiles
ALTER TABLE app.seller_profiles RENAME COLUMN vat_id TO ein;

-- Rename country -> state in seller_profiles
ALTER TABLE app.seller_profiles RENAME COLUMN country TO state;

-- Update default currency in seller_settlements from EUR to USD
ALTER TABLE app.seller_settlements ALTER COLUMN currency SET DEFAULT 'USD';
