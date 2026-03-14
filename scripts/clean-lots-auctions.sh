#!/usr/bin/env bash
# =============================================================================
# clean-lots-auctions.sh
#
# Clears all lot and auction data across the platform:
#   - PostgreSQL tables (auction-engine, catalog, payment, seller, user, media,
#     search, co2, analytics, compliance)
#   - Elasticsearch indexes (auction_lots_active, auction_lots_archive)
#   - Redis cache
#   - MinIO image buckets
#   - NATS JetStream streams (AUCTION, CATALOG, PAYMENT, MEDIA, CO2)
#
# Does NOT touch: Keycloak, user profiles, seller profiles, categories,
#                 casbin_rule tables, broker data, notification-service data
#
# Usage:
#   ./scripts/clean-lots-auctions.sh          # Interactive (prompts confirmation)
#   ./scripts/clean-lots-auctions.sh --force   # Skip confirmation
# =============================================================================

set -eu

POSTGRES_CONTAINER="auction-platform-postgres"
REDIS_CONTAINER="auction-platform-redis"
NATS_IMAGE="natsio/nats-box:0.14.5"
DOCKER_NETWORK="auction-platform-network"
DB_USER="dev"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log()  { echo -e "${GREEN}[CLEAN]${NC} $*"; }
warn() { echo -e "${YELLOW}[WARN]${NC} $*"; }
err()  { echo -e "${RED}[ERROR]${NC} $*"; }

# ---- Confirmation ----------------------------------------------------------

if [[ "${1:-}" != "--force" ]]; then
  echo ""
  echo -e "${RED}WARNING: This will permanently delete ALL lot and auction data.${NC}"
  echo ""
  echo "  Affected data:"
  echo "    - Auction events, read model, snapshots, outbox"
  echo "    - Catalog lots, lot images"
  echo "    - Payments, settlements, invoices"
  echo "    - Seller lots, settlements, metrics"
  echo "    - User bids, purchases, watchlist"
  echo "    - Media images"
  echo "    - Elasticsearch search indexes"
  echo "    - Redis cache"
  echo "    - MinIO image files"
  echo "    - NATS JetStream messages"
  echo ""
  echo -e "  ${GREEN}Preserved:${NC} Keycloak users, user/seller profiles, categories, RBAC policies"
  echo ""
  read -rp "Type 'yes' to continue: " confirm
  if [[ "$confirm" != "yes" ]]; then
    echo "Aborted."
    exit 0
  fi
fi

# ---- Helper: run psql on a specific database --------------------------------

psql_exec() {
  local db="$1"
  local sql="$2"
  docker exec "$POSTGRES_CONTAINER" psql -U "$DB_USER" -d "$db" -c "$sql" 2>/dev/null
}

# =============================================================================
# 1. PostgreSQL — truncate lot/auction tables per database
# =============================================================================

log "Cleaning PostgreSQL tables..."

# --- auction_engine ---
log "  auction_engine: events, read model, snapshots, outbox"
psql_exec "auction_engine" "
  TRUNCATE TABLE app.auction_events CASCADE;
  TRUNCATE TABLE app.auction_read_model CASCADE;
  TRUNCATE TABLE app.auction_snapshots CASCADE;
  TRUNCATE TABLE app.outbox CASCADE;
"

# --- auction_catalog ---
log "  auction_catalog: lots, lot_images (preserving categories)"
psql_exec "auction_catalog" "
  TRUNCATE TABLE app.lot_images CASCADE;
  TRUNCATE TABLE app.lots CASCADE;
"

# --- auction_payments ---
log "  auction_payments: invoices, settlements, payments, outbox"
psql_exec "auction_payments" "
  TRUNCATE TABLE app.invoices CASCADE;
  TRUNCATE TABLE app.settlements CASCADE;
  TRUNCATE TABLE app.payments CASCADE;
  TRUNCATE TABLE app.outbox CASCADE;
"

# --- auction_sellers ---
log "  auction_sellers: lots, settlements, metrics, co2 (preserving profiles)"
psql_exec "auction_sellers" "
  TRUNCATE TABLE app.seller_co2 CASCADE;
  TRUNCATE TABLE app.seller_settlements CASCADE;
  TRUNCATE TABLE app.seller_lots CASCADE;
  TRUNCATE TABLE app.seller_metrics CASCADE;
"

# --- auction_users ---
log "  auction_users: bids, purchases, watchlist (preserving user profiles)"
psql_exec "auction_users" "
  TRUNCATE TABLE app.user_bids CASCADE;
  TRUNCATE TABLE app.user_purchases CASCADE;
  TRUNCATE TABLE app.user_watchlist CASCADE;
"

# --- auction_media ---
log "  auction_media: images"
psql_exec "auction_media" "
  TRUNCATE TABLE app.images CASCADE;
"

# --- auction_search ---
log "  auction_search: search_metadata"
psql_exec "auction_search" "
  TRUNCATE TABLE app.search_metadata CASCADE;
"

# --- auction_co2 ---
log "  auction_co2: co2 calculations"
psql_exec "auction_co2" "
  TRUNCATE TABLE app.co2_calculations CASCADE;
" 2>/dev/null || warn "  auction_co2: table not found or already empty (skipped)"

# --- auction_analytics ---
log "  auction_analytics: metrics"
psql_exec "auction_analytics" "
  DO \$\$
  DECLARE t TEXT;
  BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'app' AND tablename NOT LIKE 'casbin%' AND tablename != 'flyway_schema_history'
    LOOP EXECUTE format('TRUNCATE TABLE app.%I CASCADE', t);
    END LOOP;
  END \$\$;
" 2>/dev/null || warn "  auction_analytics: cleanup skipped"

# --- auction_compliance ---
log "  auction_compliance: audit logs"
psql_exec "auction_compliance" "
  DO \$\$
  DECLARE t TEXT;
  BEGIN
    FOR t IN SELECT tablename FROM pg_tables WHERE schemaname = 'app' AND tablename NOT LIKE 'casbin%' AND tablename != 'flyway_schema_history'
    LOOP EXECUTE format('TRUNCATE TABLE app.%I CASCADE', t);
    END LOOP;
  END \$\$;
" 2>/dev/null || warn "  auction_compliance: cleanup skipped"

log "PostgreSQL cleanup done."

# =============================================================================
# 2. Elasticsearch — delete and let services recreate on restart
# =============================================================================

log "Cleaning Elasticsearch indexes..."
curl -s -X DELETE "http://localhost:9200/auction_lots_active" -o /dev/null 2>/dev/null && log "  Deleted auction_lots_active" || warn "  auction_lots_active not found"
curl -s -X DELETE "http://localhost:9200/auction_lots_archive" -o /dev/null 2>/dev/null && log "  Deleted auction_lots_archive" || warn "  auction_lots_archive not found"
log "Elasticsearch cleanup done."

# =============================================================================
# 3. Redis — flush all cached auction/bid state
# =============================================================================

log "Flushing Redis cache..."
docker exec "$REDIS_CONTAINER" redis-cli FLUSHALL > /dev/null 2>&1 && log "  Redis flushed" || warn "  Redis flush failed"

# =============================================================================
# 4. MinIO — clear image buckets
# =============================================================================

log "Cleaning MinIO image buckets..."
for bucket in auction-media auction-thumbnails; do
  docker exec auction-platform-minio mc rm --recursive --force "local/$bucket/" > /dev/null 2>&1 \
    && log "  Cleared $bucket" \
    || warn "  $bucket: empty or not found"
done
log "MinIO cleanup done."

# =============================================================================
# 5. NATS JetStream — purge streams (keeps stream config, removes messages)
# =============================================================================

log "Purging NATS JetStream streams..."
for stream in AUCTION CATALOG PAYMENT MEDIA CO2 NOTIFY COMPLIANCE; do
  docker run --rm --network "$DOCKER_NETWORK" "$NATS_IMAGE" \
    nats stream purge "$stream" -s nats://nats:4222 --force > /dev/null 2>&1 \
    && log "  Purged $stream" \
    || warn "  $stream: not found or already empty"
done
log "NATS cleanup done."

# =============================================================================
# Done
# =============================================================================

echo ""
log "All lot and auction data has been cleared."
echo ""
echo "  Next steps:"
echo "    1. Restart backend services to re-run Flyway and recreate ES indexes:"
echo "       docker compose -f docker/compose/docker-compose-full.yaml --env-file docker/compose/.env restart"
echo "    2. Re-seed data if needed (run seed script or create lots via seller portal)"
echo ""
