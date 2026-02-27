-- =============================================================================
-- Make lead_id optional for standalone lot intakes
-- =============================================================================

ALTER TABLE app.lot_intakes ALTER COLUMN lead_id DROP NOT NULL;
