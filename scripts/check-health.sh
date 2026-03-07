#!/usr/bin/env bash
# =============================================================================
# check-health.sh — Pre-demo health check for all platform services
#
# Validates that all infrastructure and application services are up and
# responsive. Run this 10-15 minutes before the demo to catch issues early.
# =============================================================================

set -uo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
API="$GATEWAY_URL/api/v1"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

PASS=0
FAIL=0
WARN=0

check_service() {
    local name="$1"
    local url="$2"
    local expected="${3:-200}"

    local status
    status=$(curl -sf -o /dev/null -w "%{http_code}" --max-time 5 "$url" 2>/dev/null) || status="000"

    if [ "$status" = "$expected" ] || [ "$status" = "200" ] || [ "$status" = "204" ]; then
        echo -e "  ${GREEN}PASS${NC}  $name ($url) — HTTP $status"
        PASS=$((PASS + 1))
    elif [ "$status" = "000" ]; then
        echo -e "  ${RED}FAIL${NC}  $name ($url) — Connection refused"
        FAIL=$((FAIL + 1))
    elif [ "$status" = "401" ] || [ "$status" = "403" ]; then
        echo -e "  ${YELLOW}AUTH${NC}  $name ($url) — HTTP $status (service up, auth required)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${YELLOW}WARN${NC}  $name ($url) — HTTP $status"
        WARN=$((WARN + 1))
    fi
}

check_port() {
    local name="$1"
    local host="$2"
    local port="$3"

    if timeout 3 bash -c "echo > /dev/tcp/$host/$port" 2>/dev/null; then
        echo -e "  ${GREEN}PASS${NC}  $name ($host:$port)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}FAIL${NC}  $name ($host:$port) — Not reachable"
        FAIL=$((FAIL + 1))
    fi
}

echo ""
echo -e "${BOLD}=============================================${NC}"
echo -e "${BOLD}  Tradex Platform Health Check${NC}"
echo -e "${BOLD}=============================================${NC}"
echo ""

# ---------------------------------------------------------------------------
# 1. Infrastructure Services
# ---------------------------------------------------------------------------
echo -e "${BLUE}--- Infrastructure ---${NC}"

check_port "PostgreSQL"     "localhost" 5432
check_port "NATS"           "localhost" 4222
check_port "Redis"          "localhost" 6379
check_port "Elasticsearch"  "localhost" 9200
check_port "MinIO"          "localhost" 9000

check_service "Keycloak" "$KEYCLOAK_URL/realms/auction-platform"
check_service "Prometheus" "http://localhost:9090/-/healthy"
check_service "Grafana" "http://localhost:3333/api/health"
check_service "MailHog" "http://localhost:8025"

echo ""

# ---------------------------------------------------------------------------
# 2. Backend Microservices (via direct ports)
# ---------------------------------------------------------------------------
echo -e "${BLUE}--- Backend Services ---${NC}"

check_service "gateway-service"     "http://localhost:8080/q/health/ready"
check_service "auction-engine"      "http://localhost:8081/q/health/ready"
check_service "catalog-service"     "http://localhost:8082/q/health/ready"
check_service "user-service"        "http://localhost:8083/q/health/ready"
check_service "payment-service"     "http://localhost:8084/q/health/ready"
check_service "notification-service" "http://localhost:8085/q/health/ready"
check_service "media-service"       "http://localhost:8086/q/health/ready"
check_service "search-service"      "http://localhost:8087/q/health/ready"
check_service "seller-service"      "http://localhost:8088/q/health/ready"
check_service "broker-service"      "http://localhost:8089/q/health/ready"
check_service "analytics-service"   "http://localhost:8090/q/health/ready"
check_service "compliance-service"  "http://localhost:8091/q/health/ready"
check_service "co2-service"         "http://localhost:8092/q/health/ready"

echo ""

# ---------------------------------------------------------------------------
# 3. Frontend Dev Servers
# ---------------------------------------------------------------------------
echo -e "${BLUE}--- Frontend Dev Servers ---${NC}"

check_service "buyer-web"       "http://localhost:3000"
check_service "seller-portal"   "http://localhost:5174"
check_service "admin-dashboard" "http://localhost:5175"

echo ""

# ---------------------------------------------------------------------------
# 4. Keycloak Authentication
# ---------------------------------------------------------------------------
echo -e "${BLUE}--- Keycloak Auth ---${NC}"

TOKEN_RESPONSE=$(curl -sf -X POST \
    "$KEYCLOAK_URL/realms/auction-platform/protocol/openid-connect/token" \
    -d "client_id=buyer-web" \
    -d "grant_type=password" \
    -d "username=buyer@test.com" \
    -d "password=password123" 2>/dev/null) || TOKEN_RESPONSE=""

if echo "$TOKEN_RESPONSE" | jq -e '.access_token' > /dev/null 2>&1; then
    echo -e "  ${GREEN}PASS${NC}  Keycloak token exchange (buyer@test.com)"
    PASS=$((PASS + 1))

    # Test API call with token
    BUYER_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
    API_RESPONSE=$(curl -sf -o /dev/null -w "%{http_code}" --max-time 5 \
        -H "Authorization: Bearer $BUYER_TOKEN" \
        "$API/users/me" 2>/dev/null) || API_RESPONSE="000"

    if [ "$API_RESPONSE" = "200" ]; then
        echo -e "  ${GREEN}PASS${NC}  Authenticated API call (GET /users/me)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${RED}FAIL${NC}  Authenticated API call — HTTP $API_RESPONSE"
        FAIL=$((FAIL + 1))
    fi
else
    echo -e "  ${RED}FAIL${NC}  Keycloak token exchange — check credentials/realm config"
    FAIL=$((FAIL + 1))
fi

echo ""

# ---------------------------------------------------------------------------
# 5. Demo Data Validation
# ---------------------------------------------------------------------------
echo -e "${BLUE}--- Demo Data ---${NC}"

if [ -n "${BUYER_TOKEN:-}" ]; then
    # Check for lots
    LOT_COUNT=$(curl -sf -H "Authorization: Bearer $BUYER_TOKEN" \
        "$API/lots?pageSize=1" 2>/dev/null | jq -r '.data.total // .total // 0' 2>/dev/null) || LOT_COUNT=0

    if [ "$LOT_COUNT" -gt 0 ] 2>/dev/null; then
        echo -e "  ${GREEN}PASS${NC}  Demo lots exist ($LOT_COUNT lots in catalog)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${YELLOW}WARN${NC}  No demo lots found — run: bash scripts/seed-demo-data.sh"
        WARN=$((WARN + 1))
    fi

    # Check for categories
    CAT_RESPONSE=$(curl -sf -H "Authorization: Bearer $BUYER_TOKEN" \
        "$API/categories" 2>/dev/null) || CAT_RESPONSE=""
    CAT_COUNT=$(echo "$CAT_RESPONSE" | jq -r '.data | length // 0' 2>/dev/null) || CAT_COUNT=0

    if [ "$CAT_COUNT" -gt 0 ] 2>/dev/null; then
        echo -e "  ${GREEN}PASS${NC}  Categories exist ($CAT_COUNT categories)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${YELLOW}WARN${NC}  No categories found (check catalog-service migration)"
        WARN=$((WARN + 1))
    fi

    # Check for active auctions
    AUCTION_RESPONSE=$(curl -sf -H "Authorization: Bearer $BUYER_TOKEN" \
        "$API/auctions?status=ACTIVE&pageSize=1" 2>/dev/null) || AUCTION_RESPONSE=""
    AUCTION_COUNT=$(echo "$AUCTION_RESPONSE" | jq -r '.data.total // .total // 0' 2>/dev/null) || AUCTION_COUNT=0

    if [ "$AUCTION_COUNT" -gt 0 ] 2>/dev/null; then
        echo -e "  ${GREEN}PASS${NC}  Active auctions exist ($AUCTION_COUNT active)"
        PASS=$((PASS + 1))
    else
        echo -e "  ${YELLOW}WARN${NC}  No active auctions — run: bash scripts/seed-demo-data.sh"
        WARN=$((WARN + 1))
    fi
else
    echo -e "  ${YELLOW}SKIP${NC}  Demo data checks skipped (no auth token)"
    WARN=$((WARN + 1))
fi

echo ""

# ---------------------------------------------------------------------------
# Summary
# ---------------------------------------------------------------------------
echo -e "${BOLD}=============================================${NC}"
TOTAL=$((PASS + FAIL + WARN))
echo -e "  Results: ${GREEN}$PASS passed${NC} / ${RED}$FAIL failed${NC} / ${YELLOW}$WARN warnings${NC} (${TOTAL} total)"

if [ $FAIL -eq 0 ]; then
    echo -e "  ${GREEN}${BOLD}Platform is READY for demo!${NC}"
elif [ $FAIL -le 3 ]; then
    echo -e "  ${YELLOW}${BOLD}Platform has issues — review failures above${NC}"
else
    echo -e "  ${RED}${BOLD}Platform is NOT ready — too many failures${NC}"
fi
echo -e "${BOLD}=============================================${NC}"
echo ""

exit $FAIL
