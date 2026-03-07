#!/usr/bin/env bash
# =============================================================================
# seed-demo-data.sh — Populate the platform with realistic demo data via API
#
# Idempotent: safe to run multiple times. Creates lots, auctions, and bids
# for a compelling live demo of the EU B2B auction platform.
#
# Prerequisites:
#   - Docker infrastructure running (Keycloak, PostgreSQL, NATS, etc.)
#   - Backend services running (at minimum: gateway, catalog, auction-engine, user, search)
#   - curl and jq installed
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="auction-platform"
API="$GATEWAY_URL/api/v1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ---------------------------------------------------------------------------
# Helper: Get access token for a user
# ---------------------------------------------------------------------------
get_token() {
    local client_id="$1"
    local username="$2"
    local password="${3:-password123}"

    local response
    response=$(curl -sf -X POST \
        "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
        -d "client_id=$client_id" \
        -d "grant_type=password" \
        -d "username=$username" \
        -d "password=$password" \
        2>/dev/null) || {
        log_error "Failed to get token for $username (client: $client_id)"
        return 1
    }

    echo "$response" | jq -r '.access_token'
}

# ---------------------------------------------------------------------------
# Helper: Authenticated API call
# ---------------------------------------------------------------------------
api_call() {
    local method="$1"
    local endpoint="$2"
    local token="$3"
    local data="${4:-}"

    local args=(-sf -X "$method" "$API$endpoint" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json")

    if [ -n "$data" ]; then
        args+=(-d "$data")
    fi

    curl "${args[@]}" 2>/dev/null || echo '{"error": "request_failed"}'
}

# ---------------------------------------------------------------------------
# Category IDs (from V001 migration seed data)
# ---------------------------------------------------------------------------
CAT_EXCAVATORS="20000000-0000-0000-0000-000000000001"
CAT_CRANES="20000000-0000-0000-0000-000000000003"
CAT_CNC="20000000-0000-0000-0000-000000000014"
CAT_FORKLIFTS="20000000-0000-0000-0000-000000000012"
CAT_TRUCKS="20000000-0000-0000-0000-000000000010"
CAT_WELDING="20000000-0000-0000-0000-000000000017"
CAT_TRACTORS="20000000-0000-0000-0000-000000000006"
CAT_FOOD="10000000-0000-0000-0000-000000000006"
CAT_ENERGY="10000000-0000-0000-0000-000000000012"
CAT_PRINTING="10000000-0000-0000-0000-000000000007"

# =============================================================================
# MAIN
# =============================================================================
echo ""
echo "============================================="
echo "  Tradex Demo Data Seeder"
echo "============================================="
echo ""

# ---------------------------------------------------------------------------
# Step 1: Get tokens
# ---------------------------------------------------------------------------
log_info "Authenticating users..."

SELLER_TOKEN=$(get_token "seller-portal" "seller@test.com") || exit 1
log_ok "Seller token acquired"

ADMIN_TOKEN=$(get_token "admin-dashboard" "admin@test.com") || exit 1
log_ok "Admin token acquired"

BUYER_TOKEN=$(get_token "buyer-web" "buyer@test.com") || exit 1
log_ok "Buyer token acquired"

# ---------------------------------------------------------------------------
# Step 2: Ensure user profiles exist (auto-registration)
# ---------------------------------------------------------------------------
log_info "Ensuring user profiles exist..."
api_call GET "/users/me" "$SELLER_TOKEN" > /dev/null
api_call GET "/users/me" "$ADMIN_TOKEN" > /dev/null
api_call GET "/users/me" "$BUYER_TOKEN" > /dev/null
log_ok "User profiles ready"

# ---------------------------------------------------------------------------
# Step 3: Create demo lots
# ---------------------------------------------------------------------------
log_info "Creating demo lots..."

declare -a LOT_IDS=()

create_lot() {
    local title="$1"
    local description="$2"
    local category_id="$3"
    local starting_bid="$4"
    local reserve_price="$5"
    local city="$6"
    local country="$7"
    local brand="${8:-troostwijk}"
    local specs="${9:-{}}"

    local payload
    payload=$(cat <<EOJSON
{
    "brand": "$brand",
    "title": "$title",
    "description": "$description",
    "categoryId": "$category_id",
    "startingBid": $starting_bid,
    "reservePrice": $reserve_price,
    "locationCity": "$city",
    "locationCountry": "$country",
    "specifications": $specs
}
EOJSON
)

    local response
    response=$(api_call POST "/lots" "$SELLER_TOKEN" "$payload")
    local lot_id
    lot_id=$(echo "$response" | jq -r '.data.id // .id // empty' 2>/dev/null)

    if [ -n "$lot_id" ] && [ "$lot_id" != "null" ]; then
        LOT_IDS+=("$lot_id")
        log_ok "  Created lot: $title ($lot_id)"
    else
        log_warn "  Lot may already exist or creation failed: $title"
        # Try to find it by searching
        local search_response
        search_response=$(api_call GET "/lots?search=$(echo "$title" | head -c 30 | sed 's/ /%20/g')&pageSize=1" "$SELLER_TOKEN")
        lot_id=$(echo "$search_response" | jq -r '.data.items[0].id // .items[0].id // empty' 2>/dev/null)
        if [ -n "$lot_id" ] && [ "$lot_id" != "null" ]; then
            LOT_IDS+=("$lot_id")
            log_ok "  Found existing lot: $title ($lot_id)"
        fi
    fi
}

# Lot 1: Caterpillar Excavator
create_lot \
    "Caterpillar 320F L Hydraulic Excavator" \
    "Well-maintained 2019 Caterpillar 320F L hydraulic excavator with 4,200 operating hours. Full service history available. Equipped with climate-controlled cab, GPS, and quick coupler. Ready for immediate deployment. EU Stage V emission compliant." \
    "$CAT_EXCAVATORS" \
    15000 \
    45000 \
    "Rotterdam" \
    "NL" \
    "troostwijk" \
    '{"Manufacturer": "Caterpillar", "Model": "320F L", "Year": "2019", "Operating Hours": "4,200 h", "Engine Power": "122 kW", "Operating Weight": "22,200 kg", "Emission Standard": "EU Stage V"}'

# Lot 2: Siemens CNC Machine
create_lot \
    "Siemens SINUMERIK 840D CNC Milling Center" \
    "High-precision 5-axis CNC milling center with Siemens SINUMERIK 840D control. Table size 800x500mm. Includes tool magazine (30 positions), chip conveyor, and coolant system. Calibration certificate current until 2027." \
    "$CAT_CNC" \
    25000 \
    85000 \
    "Stuttgart" \
    "DE" \
    "surplex" \
    '{"Manufacturer": "DMG Mori", "Control": "Siemens SINUMERIK 840D", "Axes": "5", "Table Size": "800 x 500 mm", "Spindle Speed": "12,000 RPM", "Year": "2020", "Condition": "Excellent"}'

# Lot 3: ABB Industrial Robot
create_lot \
    "ABB IRB 6700 Industrial Robot with Controller" \
    "ABB IRB 6700-150/3.2 industrial robot arm with IRC5 controller. Payload capacity 150 kg, reach 3.2m. Previously used in automotive welding line. Complete with pendant, cables, and software license. Low cycle count." \
    "$CAT_WELDING" \
    20000 \
    65000 \
    "Eindhoven" \
    "NL" \
    "troostwijk" \
    '{"Manufacturer": "ABB", "Model": "IRB 6700-150/3.2", "Payload": "150 kg", "Reach": "3.2 m", "Controller": "IRC5", "Year": "2021", "Axes": "6"}'

# Lot 4: Liebherr Mobile Crane
create_lot \
    "Liebherr LTM 1100-5.2 Mobile Crane" \
    "Liebherr LTM 1100-5.2 all-terrain mobile crane. Maximum lifting capacity 100 tonnes, maximum boom length 52m. EU road-legal, 5-axle carrier. Full service documentation. VarioBase Plus equipped." \
    "$CAT_CRANES" \
    50000 \
    180000 \
    "Antwerp" \
    "BE" \
    "troostwijk" \
    '{"Manufacturer": "Liebherr", "Model": "LTM 1100-5.2", "Max Capacity": "100 t", "Max Boom": "52 m", "Year": "2018", "Axles": "5", "Mileage": "45,000 km"}'

# Lot 5: Trumpf Laser Cutter
create_lot \
    "Trumpf TruLaser 3030 Fiber Laser Cutting Machine" \
    "Trumpf TruLaser 3030 fiber laser cutting system. 6kW laser source, cutting area 3000x1500mm. Processes steel up to 25mm, stainless up to 20mm, aluminum up to 15mm. Includes LiftMaster loading system and dust extraction." \
    "$CAT_CNC" \
    30000 \
    120000 \
    "Milan" \
    "IT" \
    "surplex" \
    '{"Manufacturer": "Trumpf", "Model": "TruLaser 3030", "Laser Power": "6 kW", "Cutting Area": "3000 x 1500 mm", "Max Steel": "25 mm", "Year": "2020", "Laser Type": "Fiber"}'

# Lot 6: Toyota Forklift
create_lot \
    "Toyota 8FBE18T Electric Forklift" \
    "Toyota 8FBE18T 3-wheel electric counterbalance forklift. Capacity 1,800 kg, lift height 4.7m. New battery installed January 2025. Side shift and fork positioner included. Indoor use only, excellent condition." \
    "$CAT_FORKLIFTS" \
    3000 \
    12000 \
    "Warsaw" \
    "PL" \
    "industrial-auctions" \
    '{"Manufacturer": "Toyota", "Model": "8FBE18T", "Capacity": "1,800 kg", "Lift Height": "4.7 m", "Drive": "Electric", "Year": "2020", "Battery": "New (2025)"}'

# Lot 7: Volvo Truck
create_lot \
    "Volvo FH 500 6x2 Tractor Unit" \
    "Volvo FH 500 Euro 6 tractor unit. I-Shift automatic gearbox, dual fuel tanks (2x400L). Adaptive cruise control, lane departure warning, EBS. Full Volvo service history. Ready for international transport." \
    "$CAT_TRUCKS" \
    18000 \
    55000 \
    "Gothenburg" \
    "SE" \
    "troostwijk" \
    '{"Manufacturer": "Volvo", "Model": "FH 500", "Configuration": "6x2", "Engine": "D13K Euro 6", "Power": "500 hp", "Mileage": "380,000 km", "Year": "2020"}'

# Lot 8: John Deere Tractor
create_lot \
    "John Deere 6250R Premium Tractor" \
    "John Deere 6250R with CommandPRO joystick and AutoPowr transmission. 250 hp, front PTO, front loader ready. StarFire 6000 GPS receiver with AutoTrac. 2,800 engine hours. Excellent condition for arable or livestock operations." \
    "$CAT_TRACTORS" \
    35000 \
    95000 \
    "Rennes" \
    "FR" \
    "troostwijk" \
    '{"Manufacturer": "John Deere", "Model": "6250R", "Power": "250 hp", "Transmission": "AutoPowr", "Engine Hours": "2,800 h", "Year": "2021", "GPS": "StarFire 6000"}'

# Lot 9: Industrial Packaging Line (for admin pending approval demo)
create_lot \
    "Krones Modulfill PET Bottling Line" \
    "Complete Krones Modulfill PET bottling line. Capacity 12,000 bottles/hour. Includes rinser, filler, capper, labeller, and shrink wrapper. Recently overhauled with new seals and bearings. Ideal for beverage or food-grade applications." \
    "$CAT_FOOD" \
    40000 \
    150000 \
    "Munich" \
    "DE" \
    "surplex" \
    '{"Manufacturer": "Krones", "Model": "Modulfill", "Capacity": "12,000 bph", "Type": "PET Bottling", "Year": "2019", "Condition": "Overhauled"}'

# Lot 10: Solar Panel Installation Equipment
create_lot \
    "SMA Sunny Central 2500-EV Solar Inverter Station" \
    "SMA Sunny Central 2500-EV central inverter with medium voltage transformer. 2,500 kW rated power. Complete containerized solution. Previously deployed in a 10 MW solar farm. Full commissioning documentation available." \
    "$CAT_ENERGY" \
    15000 \
    60000 \
    "Valencia" \
    "ES" \
    "industrial-auctions" \
    '{"Manufacturer": "SMA", "Model": "Sunny Central 2500-EV", "Rated Power": "2,500 kW", "Type": "Central Inverter", "Year": "2020", "Installation": "Containerized"}'

echo ""
log_info "Created ${#LOT_IDS[@]} lots"

# ---------------------------------------------------------------------------
# Step 4: Submit lots for review
# ---------------------------------------------------------------------------
log_info "Submitting lots for review..."

for lot_id in "${LOT_IDS[@]}"; do
    api_call POST "/lots/$lot_id/submit" "$SELLER_TOKEN" > /dev/null 2>&1 || true
done
log_ok "Lots submitted for review"

# Give search indexing a moment
sleep 2

# ---------------------------------------------------------------------------
# Step 5: Approve lots (leave last 2 pending for admin demo)
# ---------------------------------------------------------------------------
log_info "Approving lots (keeping last 2 pending for admin demo)..."

APPROVED_COUNT=0
TOTAL=${#LOT_IDS[@]}
APPROVE_LIMIT=$((TOTAL > 2 ? TOTAL - 2 : TOTAL))

for i in $(seq 0 $((APPROVE_LIMIT - 1))); do
    lot_id="${LOT_IDS[$i]}"
    api_call POST "/lots/$lot_id/approve" "$ADMIN_TOKEN" > /dev/null 2>&1 || true
    APPROVED_COUNT=$((APPROVED_COUNT + 1))
done

log_ok "Approved $APPROVED_COUNT lots, ${#LOT_IDS[@]} - $APPROVED_COUNT pending for admin demo"

# ---------------------------------------------------------------------------
# Step 6: Create auctions for approved lots
# ---------------------------------------------------------------------------
log_info "Creating auctions..."

# Current time + offsets for auction scheduling
NOW_EPOCH=$(date +%s)
START_PAST=$((NOW_EPOCH - 7200))           # 2 hours ago
END_FUTURE=$((NOW_EPOCH + 86400))          # 24 hours from now
END_SOON=$((NOW_EPOCH + 180))              # 3 minutes from now (for anti-sniping demo)
END_FUTURE_2=$((NOW_EPOCH + 172800))       # 48 hours from now

format_iso() {
    date -u -d "@$1" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || \
    date -u -r "$1" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || \
    echo "2026-03-15T12:00:00Z"
}

START_TIME=$(format_iso $START_PAST)
END_TIME_1=$(format_iso $END_FUTURE)
END_TIME_SOON=$(format_iso $END_SOON)
END_TIME_2=$(format_iso $END_FUTURE_2)

declare -a AUCTION_IDS=()

create_auction() {
    local lot_id="$1"
    local start_time="$2"
    local end_time="$3"
    local starting_bid="$4"

    local payload
    payload=$(cat <<EOJSON
{
    "lotId": "$lot_id",
    "brand": "troostwijk",
    "sellerId": "00000000-0000-0000-0000-000000000002",
    "startTime": "$start_time",
    "endTime": "$end_time",
    "startingBid": $starting_bid
}
EOJSON
)

    local response
    response=$(api_call POST "/auctions" "$ADMIN_TOKEN" "$payload")
    local auction_id
    auction_id=$(echo "$response" | jq -r '.data.auctionId // .data.id // .auctionId // .id // empty' 2>/dev/null)

    if [ -n "$auction_id" ] && [ "$auction_id" != "null" ]; then
        AUCTION_IDS+=("$auction_id")
        log_ok "  Created auction: $auction_id (lot: $lot_id)"
    else
        log_warn "  Auction creation may have failed for lot: $lot_id"
    fi
}

# Create auctions for the first several approved lots
if [ ${#LOT_IDS[@]} -ge 1 ]; then
    create_auction "${LOT_IDS[0]}" "$START_TIME" "$END_TIME_1" 15000    # Caterpillar - active
fi
if [ ${#LOT_IDS[@]} -ge 2 ]; then
    create_auction "${LOT_IDS[1]}" "$START_TIME" "$END_TIME_2" 25000    # Siemens CNC - active
fi
if [ ${#LOT_IDS[@]} -ge 3 ]; then
    create_auction "${LOT_IDS[2]}" "$START_TIME" "$END_TIME_SOON" 20000 # ABB Robot - ending soon!
fi
if [ ${#LOT_IDS[@]} -ge 4 ]; then
    create_auction "${LOT_IDS[3]}" "$START_TIME" "$END_TIME_1" 50000    # Liebherr Crane - active
fi
if [ ${#LOT_IDS[@]} -ge 5 ]; then
    create_auction "${LOT_IDS[4]}" "$START_TIME" "$END_TIME_2" 30000    # Trumpf Laser - active
fi

echo ""
log_info "Created ${#AUCTION_IDS[@]} auctions"

# ---------------------------------------------------------------------------
# Step 7: Place bids to create bid history
# ---------------------------------------------------------------------------
log_info "Placing demo bids..."

place_bid() {
    local auction_id="$1"
    local amount="$2"
    local token="$3"
    local label="$4"

    local payload="{\"amount\": $amount}"
    local response
    response=$(api_call POST "/auctions/$auction_id/bids" "$token" "$payload")
    local success
    success=$(echo "$response" | jq -r '.data.bidId // .bidId // .data.id // .id // empty' 2>/dev/null)

    if [ -n "$success" ] && [ "$success" != "null" ]; then
        log_ok "  Bid placed: EUR $amount on auction ($label)"
    else
        log_warn "  Bid may have failed: EUR $amount ($label)"
    fi
}

# Place bids on the Caterpillar auction (auction 0)
if [ ${#AUCTION_IDS[@]} -ge 1 ]; then
    place_bid "${AUCTION_IDS[0]}" 16000 "$BUYER_TOKEN" "Caterpillar - Bid 1"
    sleep 0.5
    place_bid "${AUCTION_IDS[0]}" 18000 "$BUYER_TOKEN" "Caterpillar - Bid 2"
    sleep 0.5
    place_bid "${AUCTION_IDS[0]}" 20000 "$BUYER_TOKEN" "Caterpillar - Bid 3"
fi

# Place bids on the ABB Robot auction (auction 2) - this is the "ending soon" one
if [ ${#AUCTION_IDS[@]} -ge 3 ]; then
    place_bid "${AUCTION_IDS[2]}" 21000 "$BUYER_TOKEN" "ABB Robot - Bid 1"
    sleep 0.5
    place_bid "${AUCTION_IDS[2]}" 23000 "$BUYER_TOKEN" "ABB Robot - Bid 2"
fi

# Place bids on the Liebherr Crane auction (auction 3)
if [ ${#AUCTION_IDS[@]} -ge 4 ]; then
    place_bid "${AUCTION_IDS[3]}" 55000 "$BUYER_TOKEN" "Liebherr Crane - Bid 1"
    sleep 0.5
    place_bid "${AUCTION_IDS[3]}" 60000 "$BUYER_TOKEN" "Liebherr Crane - Bid 2"
fi

# ---------------------------------------------------------------------------
# Step 8: Add watchlist items for buyer
# ---------------------------------------------------------------------------
log_info "Adding watchlist items for buyer..."

if [ ${#LOT_IDS[@]} -ge 3 ]; then
    api_call POST "/users/me/watchlist/${LOT_IDS[0]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    api_call POST "/users/me/watchlist/${LOT_IDS[2]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    api_call POST "/users/me/watchlist/${LOT_IDS[4]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    log_ok "Added 3 lots to buyer watchlist"
fi

# ---------------------------------------------------------------------------
# Done!
# ---------------------------------------------------------------------------
echo ""
echo "============================================="
echo -e "${GREEN}  Demo data seeded successfully!${NC}"
echo "============================================="
echo ""
echo "Summary:"
echo "  - ${#LOT_IDS[@]} lots created (${APPROVED_COUNT} approved, $((TOTAL - APPROVED_COUNT)) pending)"
echo "  - ${#AUCTION_IDS[@]} auctions created (1 ending soon for anti-sniping demo)"
echo "  - Bids placed on active auctions"
echo "  - Buyer watchlist populated"
echo ""
echo "Demo accounts:"
echo "  Buyer:  buyer@test.com  / password123"
echo "  Seller: seller@test.com / password123"
echo "  Admin:  admin@test.com  / password123"
echo ""
echo "Frontends:"
echo "  Buyer:  http://localhost:3000"
echo "  Seller: http://localhost:5174"
echo "  Admin:  http://localhost:5175"
echo ""
