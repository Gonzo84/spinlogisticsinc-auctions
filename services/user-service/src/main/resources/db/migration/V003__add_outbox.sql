CREATE TABLE IF NOT EXISTS app.outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    nats_subject TEXT NOT NULL,
    published BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    published_at TIMESTAMPTZ,
    retry_count INT DEFAULT 0,
    dead_letter BOOLEAN DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_outbox_pending
    ON app.outbox (created_at) WHERE published = FALSE AND dead_letter = FALSE;
