-- Fix: original_url should never be NULL
-- First ensure no existing NULLs
UPDATE app.images SET original_url = object_key WHERE original_url IS NULL;
ALTER TABLE app.images ALTER COLUMN original_url SET NOT NULL;
