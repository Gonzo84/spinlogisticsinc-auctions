-- ==========================================================================
-- V007: Add composite indexes to auction_read_model for query performance
-- ==========================================================================
-- These indexes support common query patterns:
-- - Active auctions ending soon (status + end_time)
-- - Seller dashboard (seller_id + status)
-- - Bidder activity lookup (current_high_bidder_id)
-- - Brand-scoped listing (brand + status)
--
-- Uses CONCURRENTLY to avoid table locks in production deployments.
-- flyway:postgresql:executeInTransaction=false
-- ==========================================================================

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_arm_status_end
    ON app.auction_read_model(status, end_time);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_arm_seller_status
    ON app.auction_read_model(seller_id, status);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_arm_bidder
    ON app.auction_read_model(current_high_bidder_id)
    WHERE current_high_bidder_id IS NOT NULL;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_arm_brand_status
    ON app.auction_read_model(brand, status);
