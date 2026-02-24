#!/usr/bin/env bash
# =============================================================================
# Start the full auction platform (infrastructure + all backend services)
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "=========================================="
echo " EU Auction Platform — Full Stack Startup"
echo "=========================================="

# Step 1: Build all backend services
echo ""
echo "[1/3] Building all backend services..."
./gradlew quarkusBuild -x test --parallel

# Step 2: Start infrastructure
echo ""
echo "[2/3] Starting infrastructure services..."
docker compose -f docker-compose-full.yaml up -d postgres nats redis keycloak minio minio-init elasticsearch otel-collector prometheus grafana mailhog

echo "    Waiting for infrastructure to be healthy..."
docker compose -f docker-compose-full.yaml up -d --wait postgres nats redis minio elasticsearch 2>/dev/null || true
sleep 10

# Step 3: Start all backend services
echo ""
echo "[3/3] Starting all backend services..."
docker compose -f docker-compose-full.yaml up -d --build

echo ""
echo "=========================================="
echo " Platform startup initiated!"
echo "=========================================="
echo ""
echo " Infrastructure:"
echo "   PostgreSQL      http://localhost:5432"
echo "   NATS            nats://localhost:4222"
echo "   Keycloak        http://localhost:8180"
echo "   Redis           redis://localhost:6379"
echo "   MinIO           http://localhost:9000  (console: http://localhost:9001)"
echo "   Elasticsearch   http://localhost:9200"
echo "   Prometheus      http://localhost:9090"
echo "   Grafana         http://localhost:3333"
echo "   MailHog         http://localhost:8025"
echo ""
echo " Backend Services:"
echo "   gateway-service         http://localhost:8080"
echo "   auction-engine          http://localhost:8081"
echo "   catalog-service         http://localhost:8082"
echo "   user-service            http://localhost:8083"
echo "   payment-service         http://localhost:8084"
echo "   notification-service    http://localhost:8085"
echo "   media-service           http://localhost:8086"
echo "   search-service          http://localhost:8087"
echo "   seller-service          http://localhost:8088"
echo "   broker-service          http://localhost:8089"
echo "   analytics-service       http://localhost:8090"
echo "   compliance-service      http://localhost:8091"
echo "   co2-service             http://localhost:8092"
echo ""
echo " Monitor logs:  docker compose -f docker-compose-full.yaml logs -f"
echo " Stop all:      docker compose -f docker-compose-full.yaml down"
echo " Stop + clean:  docker compose -f docker-compose-full.yaml down -v"
echo ""
