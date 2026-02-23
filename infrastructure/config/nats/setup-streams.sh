#!/bin/bash
# NATS JetStream Stream Configuration
# Run this after NATS server is running to set up all streams and consumers

set -e

NATS_URL="${NATS_URL:-nats://localhost:4222}"
REPLICAS="${REPLICAS:-1}"

echo "Setting up NATS JetStream streams at ${NATS_URL}..."

# ─── Streams ───────────────────────────────────────────────────────────────────

# AUCTION_EVENTS - Core bidding events
nats stream add AUCTION_EVENTS \
  --server="${NATS_URL}" \
  --subjects="auction.>" \
  --retention=limits \
  --max-age=365d \
  --max-bytes=53687091200 \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --dupe-window=2m \
  --max-msg-size=1048576 \
  --defaults 2>/dev/null || echo "Stream AUCTION_EVENTS already exists"

# CATALOG_EVENTS - Lot lifecycle events
nats stream add CATALOG_EVENTS \
  --server="${NATS_URL}" \
  --subjects="catalog.>" \
  --retention=limits \
  --max-age=90d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --dupe-window=2m \
  --defaults 2>/dev/null || echo "Stream CATALOG_EVENTS already exists"

# PAYMENT_EVENTS - Financial events (7-year retention for EU compliance)
nats stream add PAYMENT_EVENTS \
  --server="${NATS_URL}" \
  --subjects="payment.>" \
  --retention=limits \
  --max-age=2555d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --dupe-window=2m \
  --defaults 2>/dev/null || echo "Stream PAYMENT_EVENTS already exists"

# USER_EVENTS - Registration, KYC events
nats stream add USER_EVENTS \
  --server="${NATS_URL}" \
  --subjects="user.>" \
  --retention=limits \
  --max-age=365d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --defaults 2>/dev/null || echo "Stream USER_EVENTS already exists"

# MEDIA_EVENTS - Image processing events
nats stream add MEDIA_EVENTS \
  --server="${NATS_URL}" \
  --subjects="media.>" \
  --retention=limits \
  --max-age=30d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --defaults 2>/dev/null || echo "Stream MEDIA_EVENTS already exists"

# COMPLIANCE_EVENTS - GDPR, AML, audit (10-year retention)
nats stream add COMPLIANCE_EVENTS \
  --server="${NATS_URL}" \
  --subjects="compliance.>" \
  --retention=limits \
  --max-age=3650d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --defaults 2>/dev/null || echo "Stream COMPLIANCE_EVENTS already exists"

# CO2_EVENTS - Environmental impact calculations
nats stream add CO2_EVENTS \
  --server="${NATS_URL}" \
  --subjects="co2.>" \
  --retention=limits \
  --max-age=365d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --defaults 2>/dev/null || echo "Stream CO2_EVENTS already exists"

# NOTIFICATION_EVENTS - Notification dispatching
nats stream add NOTIFICATION_EVENTS \
  --server="${NATS_URL}" \
  --subjects="notify.>" \
  --retention=limits \
  --max-age=7d \
  --storage=file \
  --replicas="${REPLICAS}" \
  --discard=old \
  --defaults 2>/dev/null || echo "Stream NOTIFICATION_EVENTS already exists"

echo ""
echo "Setting up consumers..."

# ─── Consumers ─────────────────────────────────────────────────────────────────

# Auction engine consumes its own events for projections
nats consumer add AUCTION_EVENTS auction-engine \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=30s \
  --max-deliver=5 \
  --replay=instant \
  --filter="" \
  --pull 2>/dev/null || echo "Consumer auction-engine already exists"

# Notification service listens for bid and auction events
nats consumer add AUCTION_EVENTS notification-svc \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=30s \
  --max-deliver=5 \
  --filter="auction.bid.>" \
  --pull 2>/dev/null || echo "Consumer notification-svc already exists"

# Search service indexes bids
nats consumer add AUCTION_EVENTS search-svc-bids \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=30s \
  --max-deliver=5 \
  --filter="auction.bid.>" \
  --pull 2>/dev/null || echo "Consumer search-svc-bids already exists"

# Search service indexes catalog changes
nats consumer add CATALOG_EVENTS search-catalog \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=30s \
  --max-deliver=5 \
  --filter="catalog.lot.>" \
  --pull 2>/dev/null || echo "Consumer search-catalog already exists"

# Payment service listens for auction closings
nats consumer add AUCTION_EVENTS payment-svc \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=30s \
  --max-deliver=10 \
  --filter="auction.lot.>" \
  --pull 2>/dev/null || echo "Consumer payment-svc already exists"

# Analytics service consumes all auction events
nats consumer add AUCTION_EVENTS analytics-svc \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=60s \
  --max-deliver=3 \
  --filter="" \
  --pull 2>/dev/null || echo "Consumer analytics-svc already exists"

# CO2 service listens for new lots
nats consumer add CATALOG_EVENTS co2-svc \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=30s \
  --max-deliver=5 \
  --filter="catalog.lot.created.>" \
  --pull 2>/dev/null || echo "Consumer co2-svc already exists"

# Media service listens for uploads
nats consumer add MEDIA_EVENTS media-svc \
  --server="${NATS_URL}" \
  --deliver=all \
  --ack=explicit \
  --wait=60s \
  --max-deliver=5 \
  --filter="media.image.uploaded.>" \
  --pull 2>/dev/null || echo "Consumer media-svc already exists"

echo ""
echo "Setting up KV buckets..."

# ─── KV Buckets ────────────────────────────────────────────────────────────────

# Auction state cache for fast reads
nats kv add AUCTION_STATE \
  --server="${NATS_URL}" \
  --ttl=48h \
  --history=1 \
  --storage=file \
  --replicas="${REPLICAS}" 2>/dev/null || echo "KV AUCTION_STATE already exists"

# Bid deduplication
nats kv add BID_DEDUP \
  --server="${NATS_URL}" \
  --ttl=5m \
  --history=1 \
  --storage=memory \
  --replicas="${REPLICAS}" 2>/dev/null || echo "KV BID_DEDUP already exists"

# Rate limiting per user
nats kv add RATE_LIMITS \
  --server="${NATS_URL}" \
  --ttl=1m \
  --history=1 \
  --storage=memory \
  --replicas="${REPLICAS}" 2>/dev/null || echo "KV RATE_LIMITS already exists"

echo ""
echo "NATS JetStream setup complete!"
echo "Streams:"
nats stream list --server="${NATS_URL}" 2>/dev/null || true
