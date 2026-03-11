-- ==========================================================================
-- V007: Add composite indexes to payments table for query performance
-- ==========================================================================
-- These indexes support common query patterns:
-- - Payment lookup by auction (checkout, settlement flows)
-- - Buyer payment history filtered by status (buyer dashboard)
--
-- Uses CONCURRENTLY to avoid table locks in production deployments.
-- flyway:postgresql:executeInTransaction=false
-- ==========================================================================

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_auction
    ON app.payments(auction_id);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_payments_buyer_status
    ON app.payments(buyer_id, status);
