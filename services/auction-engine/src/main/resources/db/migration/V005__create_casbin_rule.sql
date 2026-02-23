-- ==========================================================================
-- V005: Create the casbin_rule table and seed default policies
-- ==========================================================================
-- Casbin policy storage for the auction-engine service. Policies control
-- access to REST endpoints based on user roles derived from JWT claims.
--
-- Policy format (RBAC with resource matching):
--   p, <role>, <resource_pattern>, <http_methods>, <effect>
--
-- Role inheritance:
--   g, <child_role>, <parent_role>
-- ==========================================================================

CREATE TABLE IF NOT EXISTS app.casbin_rule (
    id    BIGSERIAL    PRIMARY KEY,
    ptype VARCHAR(100) NOT NULL DEFAULT '',
    v0    VARCHAR(100) NOT NULL DEFAULT '',
    v1    VARCHAR(100) NOT NULL DEFAULT '',
    v2    VARCHAR(100) NOT NULL DEFAULT '',
    v3    VARCHAR(100) NOT NULL DEFAULT '',
    v4    VARCHAR(100) NOT NULL DEFAULT '',
    v5    VARCHAR(100) NOT NULL DEFAULT ''
);

-- Default policies for auction-engine
INSERT INTO app.casbin_rule (ptype, v0, v1, v2, v3) VALUES
    -- Active buyers: read auctions and place bids / manage auto-bids
    ('p', 'role:buyer_active', '/api/v1/auctions/*', 'GET', 'allow'),
    ('p', 'role:buyer_active', '/api/v1/auctions/*/bids', 'POST', 'allow'),
    ('p', 'role:buyer_active', '/api/v1/auctions/*/auto-bids', 'POST|DELETE', 'allow'),

    -- Blocked buyers: read-only, bidding denied
    ('p', 'role:buyer_blocked', '/api/v1/auctions/*', 'GET', 'allow'),
    ('p', 'role:buyer_blocked', '/api/v1/auctions/*/bids', 'POST', 'deny'),

    -- Verified sellers: read-only access to auctions
    ('p', 'role:seller_verified', '/api/v1/auctions/*', 'GET', 'allow'),

    -- Ops admins: create and manage auctions
    ('p', 'role:admin_ops', '/api/v1/auctions', 'POST', 'allow'),
    ('p', 'role:admin_ops', '/api/v1/auctions/*', 'PUT|DELETE', 'allow'),

    -- Super admins: full access to all auction endpoints
    ('p', 'role:admin_super', '/api/v1/**', 'GET|POST|PUT|DELETE|PATCH', 'allow'),

    -- Role inheritance: super admins inherit ops admin permissions
    ('g', 'role:admin_super', 'role:admin_ops', '', '');
