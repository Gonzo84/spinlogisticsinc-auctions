-- ==========================================================================
-- V007: Add composite indexes to auction_read_model for query performance
-- ==========================================================================
-- These indexes support common query patterns:
-- - Active auctions ending soon (status + end_time)
-- - Seller dashboard (seller_id + status)
-- - Bidder activity lookup (current_high_bidder_id)
-- - Brand-scoped listing (brand + status)
--
-- Note: Cannot use CONCURRENTLY inside Flyway transactions.
-- For production, run these indexes manually with CONCURRENTLY if needed.
-- ==========================================================================

CREATE INDEX IF NOT EXISTS idx_arm_status_end
    ON app.auction_read_model(status, end_time);

CREATE INDEX IF NOT EXISTS idx_arm_seller_status
    ON app.auction_read_model(seller_id, status);

CREATE INDEX IF NOT EXISTS idx_arm_bidder
    ON app.auction_read_model(current_high_bidder_id)
    WHERE current_high_bidder_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_arm_brand_status
    ON app.auction_read_model(brand, status);
