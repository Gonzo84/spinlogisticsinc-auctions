-- ==========================================================================
-- V001: Create the images table for the Media Service
-- ==========================================================================
-- Stores metadata for lot images, including processing status, URLs to
-- original/processed/thumbnail variants, and display ordering.
-- Schema: app (application schema, separate from system/public)
-- ==========================================================================

CREATE SCHEMA IF NOT EXISTS app;

-- ---------------------------------------------------------------------------
-- Images
-- ---------------------------------------------------------------------------
CREATE TABLE app.images (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id          UUID            NOT NULL,
    object_key      VARCHAR(500)    NOT NULL,
    original_url    TEXT,
    processed_url   TEXT,
    thumbnail_url   TEXT,
    display_order   INT             NOT NULL DEFAULT 0,
    is_primary      BOOLEAN         NOT NULL DEFAULT FALSE,
    status          VARCHAR(20)     NOT NULL DEFAULT 'UPLOADING',
    content_type    VARCHAR(100)    NOT NULL,
    file_size       BIGINT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- ---------------------------------------------------------------------------
-- Indexes
-- ---------------------------------------------------------------------------

-- Primary query path: list all images for a lot, ordered by display position
CREATE INDEX idx_images_lot_id ON app.images(lot_id, display_order);

-- Find the primary (hero) image for a lot
CREATE INDEX idx_images_primary ON app.images(lot_id) WHERE is_primary = TRUE;

-- Filter by processing status (for retry / monitoring dashboards)
CREATE INDEX idx_images_status ON app.images(status);

-- Lookup by object key (for MinIO bucket notification correlation)
CREATE INDEX idx_images_object_key ON app.images(object_key);

-- Time-based queries for cleanup of stale UPLOADING records
CREATE INDEX idx_images_created ON app.images(created_at);
