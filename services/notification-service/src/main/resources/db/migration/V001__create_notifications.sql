CREATE SCHEMA IF NOT EXISTS app;

-- Notification log: tracks all sent notifications
CREATE TABLE app.notification_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type TEXT NOT NULL,
    channel TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    subject TEXT,
    body TEXT,
    template_data JSONB,
    locale TEXT NOT NULL DEFAULT 'en',
    deep_link TEXT,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_user ON app.notification_log(user_id, created_at DESC);
CREATE INDEX idx_notif_user_unread ON app.notification_log(user_id)
    WHERE read_at IS NULL AND channel = 'IN_APP';
CREATE INDEX idx_notif_status ON app.notification_log(status);
CREATE INDEX idx_notif_type ON app.notification_log(type);

-- Notification preferences per user per type
CREATE TABLE app.notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    notification_type TEXT NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, notification_type)
);

CREATE INDEX idx_notif_pref_user ON app.notification_preferences(user_id);

-- Device tokens for push notifications
CREATE TABLE app.device_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    platform TEXT NOT NULL, -- ios, android, web
    token TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, platform, token)
);

CREATE INDEX idx_device_user ON app.device_tokens(user_id) WHERE active = TRUE;

-- Casbin rules
CREATE TABLE app.casbin_rule (
    id BIGSERIAL PRIMARY KEY,
    ptype TEXT NOT NULL DEFAULT 'p',
    v0 TEXT DEFAULT '',
    v1 TEXT DEFAULT '',
    v2 TEXT DEFAULT '',
    v3 TEXT DEFAULT '',
    v4 TEXT DEFAULT '',
    v5 TEXT DEFAULT ''
);

-- Default policies
INSERT INTO app.casbin_rule (ptype, v0, v1, v2, v3) VALUES
    ('p', 'buyer_active', '/api/v1/notifications', 'GET', 'allow'),
    ('p', 'buyer_active', '/api/v1/notifications/*', 'PUT', 'allow'),
    ('p', 'buyer_active', '/api/v1/notifications/preferences', 'GET|PUT', 'allow'),
    ('p', 'buyer_active', '/api/v1/notifications/device-token', 'POST|DELETE', 'allow'),
    ('p', 'seller_verified', '/api/v1/notifications', 'GET', 'allow'),
    ('p', 'seller_verified', '/api/v1/notifications/*', 'PUT', 'allow'),
    ('p', 'seller_verified', '/api/v1/notifications/preferences', 'GET|PUT', 'allow'),
    ('p', 'admin_super', '/api/v1/**', '.*', 'allow'),
    ('g', 'admin_ops', 'admin_super');
