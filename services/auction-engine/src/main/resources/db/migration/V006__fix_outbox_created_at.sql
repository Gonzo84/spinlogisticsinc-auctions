-- Fix: Ensure created_at is never NULL
UPDATE app.outbox SET created_at = NOW() WHERE created_at IS NULL;
ALTER TABLE app.outbox ALTER COLUMN created_at SET NOT NULL;
