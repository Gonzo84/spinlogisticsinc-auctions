-- Add commission_rate column to seller_settlements
-- Stores the rate as a decimal ratio (e.g., 0.10 = 10%)
ALTER TABLE app.seller_settlements
    ADD COLUMN commission_rate DECIMAL(5,4);

-- Backfill from existing data where possible
UPDATE app.seller_settlements
SET commission_rate = CASE
    WHEN hammer_price > 0 THEN ROUND(commission / hammer_price, 4)
    ELSE 0.10
END
WHERE commission_rate IS NULL;

-- Make non-null after backfill
ALTER TABLE app.seller_settlements
    ALTER COLUMN commission_rate SET NOT NULL;

ALTER TABLE app.seller_settlements
    ALTER COLUMN commission_rate SET DEFAULT 0.10;
