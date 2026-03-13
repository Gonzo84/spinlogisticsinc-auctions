-- Add featured flag to auction read model for homepage/search promotion
ALTER TABLE app.auction_read_model ADD COLUMN IF NOT EXISTS featured BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE app.auction_read_model ADD COLUMN IF NOT EXISTS featured_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_arm_featured ON app.auction_read_model (featured) WHERE featured = TRUE;
