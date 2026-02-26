-- Drop the foreign key constraint on lots.auction_id → auction_events(id).
-- The auction_id is set by the auction-engine service (different database),
-- so this FK can never be satisfied in a microservices architecture.
ALTER TABLE app.lots DROP CONSTRAINT IF EXISTS lots_auction_id_fkey;
