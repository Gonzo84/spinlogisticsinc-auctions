CREATE SCHEMA IF NOT EXISTS app;

-- GDPR requests
CREATE TABLE app.gdpr_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type TEXT NOT NULL, -- EXPORT, ERASURE
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, REJECTED
    reason TEXT,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    rejection_reason TEXT,
    processed_by UUID,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_gdpr_user ON app.gdpr_requests(user_id);
CREATE INDEX idx_gdpr_status ON app.gdpr_requests(status);

-- AML screenings
CREATE TABLE app.aml_screenings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    provider TEXT NOT NULL DEFAULT 'INTERNAL',
    status TEXT NOT NULL DEFAULT 'PENDING', -- PENDING, CLEAR, FLAGGED, REJECTED
    check_id TEXT,
    risk_level TEXT,
    details JSONB,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_aml_user ON app.aml_screenings(user_id);
CREATE INDEX idx_aml_status ON app.aml_screenings(status);

-- DSA content reports
CREATE TABLE app.content_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    lot_id UUID,
    user_id UUID,
    reason TEXT NOT NULL,
    description TEXT,
    status TEXT NOT NULL DEFAULT 'OPEN', -- OPEN, INVESTIGATING, RESOLVED, DISMISSED
    resolved_by UUID,
    resolution_notes TEXT,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW()
);
CREATE INDEX idx_reports_status ON app.content_reports(status);

-- Audit log (append-only, partitioned by month)
CREATE TABLE app.audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_id UUID,
    action TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    entity_id TEXT,
    details JSONB,
    ip_address TEXT,
    source TEXT,
    trace_id TEXT
);
CREATE INDEX idx_audit_timestamp ON app.audit_log(timestamp DESC);
CREATE INDEX idx_audit_user ON app.audit_log(user_id);
CREATE INDEX idx_audit_action ON app.audit_log(action);
CREATE INDEX idx_audit_entity ON app.audit_log(entity_type, entity_id);

-- Casbin rule
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
