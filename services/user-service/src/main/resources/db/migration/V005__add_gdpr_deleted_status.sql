-- ==========================================================================
-- V005: Add partial index for GDPR_DELETED user status
-- ==========================================================================
-- Supports efficient querying of GDPR-deleted users for compliance auditing
-- and prevents duplicate erasure processing.
-- ==========================================================================

CREATE INDEX idx_users_gdpr ON app.users(status) WHERE status = 'GDPR_DELETED';
