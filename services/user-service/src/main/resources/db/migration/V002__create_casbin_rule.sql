-- =============================================================================
-- V002: Create Casbin RBAC policy table and seed user-service policies
-- =============================================================================
-- The casbin_rule table stores RBAC policies loaded by the Casbin adapter.
-- Each row maps to a policy line in the Casbin model:
--   p, sub, obj, act, eft   (policy rule)
--   g, user, role           (role assignment / grouping)
-- =============================================================================

CREATE TABLE IF NOT EXISTS app.casbin_rule (
    id    BIGSERIAL    PRIMARY KEY,
    ptype VARCHAR(10)  NOT NULL,
    v0    VARCHAR(256) DEFAULT '',
    v1    VARCHAR(256) DEFAULT '',
    v2    VARCHAR(256) DEFAULT '',
    v3    VARCHAR(256) DEFAULT '',
    v4    VARCHAR(256) DEFAULT '',
    v5    VARCHAR(256) DEFAULT ''
);

CREATE INDEX idx_casbin_ptype ON app.casbin_rule(ptype);
CREATE INDEX idx_casbin_v0    ON app.casbin_rule(v0);

-- =============================================================================
-- Seed policies for user-service
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Role hierarchy: admin inherits all moderator permissions
-- ---------------------------------------------------------------------------
INSERT INTO app.casbin_rule (ptype, v0, v1) VALUES
    ('g', 'admin', 'moderator');

-- ---------------------------------------------------------------------------
-- Authenticated user self-service endpoints
-- ---------------------------------------------------------------------------
INSERT INTO app.casbin_rule (ptype, v0, v1, v2, v3) VALUES
    -- Any authenticated user can read their own profile
    ('p', 'user',      '/api/v1/users/me',                 'GET',    'allow'),
    -- Any authenticated user can update their own profile
    ('p', 'user',      '/api/v1/users/me',                 'PUT',    'allow'),
    -- Any authenticated user can add a company profile
    ('p', 'user',      '/api/v1/users/me/company',         'POST',   'allow'),
    -- Any authenticated user can view their deposit status
    ('p', 'user',      '/api/v1/users/me/deposit',         'GET',    'allow'),
    -- Any authenticated user can initiate a deposit
    ('p', 'user',      '/api/v1/users/me/deposit',         'POST',   'allow'),
    -- Any authenticated user can request a deposit refund
    ('p', 'user',      '/api/v1/users/me/deposit/refund',  'POST',   'allow');

-- ---------------------------------------------------------------------------
-- Registration (unauthenticated / pre-role assignment)
-- ---------------------------------------------------------------------------
INSERT INTO app.casbin_rule (ptype, v0, v1, v2, v3) VALUES
    ('p', 'anonymous', '/api/v1/users/register',           'POST',   'allow');

-- ---------------------------------------------------------------------------
-- Admin / moderator endpoints
-- ---------------------------------------------------------------------------
INSERT INTO app.casbin_rule (ptype, v0, v1, v2, v3) VALUES
    -- Moderators can view any user by ID
    ('p', 'moderator', '/api/v1/users/:id',                'GET',    'allow'),
    -- Admins can change user status (block, unblock, suspend)
    ('p', 'admin',     '/api/v1/users/:id/status',         'PUT',    'allow'),
    -- Admins can list all users
    ('p', 'admin',     '/api/v1/users',                    'GET',    'allow');

-- ---------------------------------------------------------------------------
-- Deny rules (explicit denials take precedence in the policy effect)
-- ---------------------------------------------------------------------------
INSERT INTO app.casbin_rule (ptype, v0, v1, v2, v3) VALUES
    -- Blocked users cannot access self-service endpoints
    ('p', 'blocked',   '/api/v1/users/me',                 'GET',    'deny'),
    ('p', 'blocked',   '/api/v1/users/me',                 'PUT',    'deny'),
    ('p', 'blocked',   '/api/v1/users/me/company',         'POST',   'deny'),
    ('p', 'blocked',   '/api/v1/users/me/deposit',         '(GET|POST)', 'deny'),
    ('p', 'blocked',   '/api/v1/users/me/deposit/refund',  'POST',   'deny');
