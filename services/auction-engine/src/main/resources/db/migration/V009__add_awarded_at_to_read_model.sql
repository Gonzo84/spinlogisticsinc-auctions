-- Track when auction was awarded (for revoke window calculation)
ALTER TABLE app.auction_read_model
    ADD COLUMN awarded_at TIMESTAMPTZ;

-- Track auto-award vs manual award
ALTER TABLE app.auction_read_model
    ADD COLUMN auto_awarded BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for finding recently awarded auctions (admin dashboard)
CREATE INDEX IF NOT EXISTS idx_arm_awarded_at ON app.auction_read_model (awarded_at)
    WHERE status = 'AWARDED';
