-- Fraud alerts table for compliance fraud detection system
CREATE TABLE app.fraud_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type TEXT NOT NULL,           -- SUSPICIOUS_BIDDING, SHILL_BIDDING, PRICE_MANIPULATION, ACCOUNT_TAKEOVER, VELOCITY_ANOMALY
    severity TEXT NOT NULL,       -- LOW, MEDIUM, HIGH, CRITICAL
    status TEXT NOT NULL DEFAULT 'NEW', -- NEW, INVESTIGATING, RESOLVED, DISMISSED
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    user_id UUID NOT NULL,
    lot_id UUID,
    auction_id UUID,
    risk_score DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    evidence JSONB NOT NULL DEFAULT '[]'::jsonb,
    resolution TEXT,
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fraud_alerts_status ON app.fraud_alerts(status);
CREATE INDEX idx_fraud_alerts_severity ON app.fraud_alerts(severity);
CREATE INDEX idx_fraud_alerts_type ON app.fraud_alerts(type);
CREATE INDEX idx_fraud_alerts_user ON app.fraud_alerts(user_id);
CREATE INDEX idx_fraud_alerts_detected ON app.fraud_alerts(detected_at DESC);
