-- Fix: Add missing lot_id column to co2_lot_seller_mapping
ALTER TABLE app.co2_lot_seller_mapping ADD COLUMN lot_id UUID NOT NULL;

-- Drop redundant unique constraint (id is already PRIMARY KEY)
ALTER TABLE app.co2_lot_seller_mapping DROP CONSTRAINT IF EXISTS uq_lot_seller;

-- Add proper unique constraint on the mapping
ALTER TABLE app.co2_lot_seller_mapping ADD CONSTRAINT uq_lot_seller_mapping UNIQUE (lot_id, seller_id);

-- Add index for lot-based lookups
CREATE INDEX idx_co2_lot_mapping_lot ON app.co2_lot_seller_mapping(lot_id);
