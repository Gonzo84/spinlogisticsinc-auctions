-- Allow NULL URLs during the upload phase (before processing completes)
ALTER TABLE app.images ALTER COLUMN original_url DROP NOT NULL;
