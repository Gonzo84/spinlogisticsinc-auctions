#!/usr/bin/env bash
# =============================================================================
# seed-demo-data.sh — Populate the platform with SPC demo data via API
#
# Idempotent: safe to run multiple times. Creates lots, auctions, and bids
# tailored for SPC (Storitveno Prodajni Center) pitch demo:
# containers, climate control equipment, construction machinery, fencing.
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
# Category IDs (SPC-specific categories from V003 migration)
#
# SPC Product Line          -> SPC Category (V003)
# Office/Residential Cont.  -> Office Containers (30..0001)
# Shipping Containers       -> Shipping Containers (30..0002)
# Sanitary Containers       -> Sanitary Containers (30..0003)
# Modular Structures        -> Modular Structures (30..0005)
# Climate Control           -> Climate Control (30..0006)
# Construction Equipment    -> Construction Equipment (30..0007)
# Fencing & Accessories     -> Fencing & Barriers (30..0008)
# Mini Excavators           -> Construction Equipment (30..0007)
# ---------------------------------------------------------------------------
CAT_OFFICE_CONTAINERS="30000000-0000-0000-0000-000000000001"
CAT_SHIPPING_CONTAINERS="30000000-0000-0000-0000-000000000002"
CAT_SANITARY_CONTAINERS="30000000-0000-0000-0000-000000000003"
CAT_MODULAR_STRUCTURES="30000000-0000-0000-0000-000000000005"
CAT_CLIMATE_CONTROL="30000000-0000-0000-0000-000000000006"
CAT_CONSTRUCTION="30000000-0000-0000-0000-000000000007"
CAT_FENCING="30000000-0000-0000-0000-000000000008"
CAT_EXCAVATORS="30000000-0000-0000-0000-000000000007"

# =============================================================================
# MAIN
# =============================================================================
echo ""
echo "============================================="
echo "  SPC Aukcije — Demo Data Seeder"
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
# Step 3: Create SPC demo lots
# ---------------------------------------------------------------------------
log_info "Creating SPC demo lots..."

declare -a LOT_IDS=()

create_lot() {
    local title="$1"
    local description="$2"
    local category_id="$3"
    local starting_bid="$4"
    local reserve_price="$5"
    local city="$6"
    local country="$7"
    local brand="${8:-spc}"
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

# ---- Lot 1: Office Container 6m (ACTIVE auction, has bids) ----
create_lot \
    "Pisarniski kontejner 6m — Custom Office Container" \
    "Insulated office container SPC K6, dimensions 6058 x 2438 x 2800 mm. Galvanized steel frame powder-coated RAL 7021. 60mm polyurethane wall insulation (U=0.37 W/m2K), 70mm stone wool roof insulation. Includes aluminum entry door, 2x PVC double-glazed windows, full electrical system (distribution box, 3-phase 32A connection, LED lighting, switches, outlets). Vinyl flooring, 250 kg/m2 floor load capacity. Stackable up to 3 units. Ready for immediate delivery from Ljubljana warehouse." \
    "$CAT_OFFICE_CONTAINERS" \
    4500 \
    5800 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Manufacturer": "SPC d.o.o.", "Model": "SPC K6", "Dimensions": "6058 x 2438 x 2800 mm", "Wall Insulation": "60mm PUR (U=0.37)", "Roof Insulation": "70mm Stone Wool", "Floor Capacity": "250 kg/m2", "Electrical": "3-phase 32A", "Condition": "New", "Stackable": "Up to 3 units"}'

# ---- Lot 2: Shipping Container 20ft HC (ACTIVE auction, 48h) ----
create_lot \
    "Ladijski zabojnik 20ft HC — Used Shipping Container" \
    "Used 20ft High Cube shipping container in good structural condition. External dimensions 6058 x 2438 x 2896 mm. Double-wing cargo doors, wooden floor. Minor surface rust patched and treated. Wind and water tight certified. Ideal for storage, conversion, or shipping. Available for pickup or delivery from Ljubljana IOC Rudnik." \
    "$CAT_SHIPPING_CONTAINERS" \
    2800 \
    3500 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Type": "20ft High Cube", "Dimensions": "6058 x 2438 x 2896 mm", "Doors": "Double-wing cargo", "Floor": "Wood", "Condition": "Used — Good", "Certification": "Wind & Water Tight", "Year": "2018"}'

# ---- Lot 3: Sanitary Container (ACTIVE auction, ending soon — anti-sniping demo) ----
create_lot \
    "Sanitarni kontejner z tusem — Sanitary Unit SPC SAN C01" \
    "Complete sanitary container SPC SAN C01 with 2 WC cabins, 2 ceramic sinks, 1 urinal, and shower section. Dimensions 3130 x 2400 x 2715 mm. 4 entry doors, 2 sanitary windows. Full mechanical and electrical installations including 5L water heater. Hot-dip galvanized steel frame. Ideal for construction sites, events, and temporary facilities. Manufactured in-house at SPC Ljubljana." \
    "$CAT_SANITARY_CONTAINERS" \
    7200 \
    8500 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Manufacturer": "SPC d.o.o.", "Model": "SPC SAN C01", "Dimensions": "3130 x 2400 x 2715 mm", "WC Cabins": "2", "Sinks": "2 ceramic", "Urinals": "1", "Shower": "Yes", "Water Heater": "5L", "Doors": "4", "Condition": "New"}'

# ---- Lot 4: Dehumidifier (ACTIVE auction, has bids) ----
create_lot \
    "Master Climate DH 92 razvlazilec — Industrial Dehumidifier" \
    "Master Climate Solutions DH 92 professional condensation dehumidifier. Dehumidification capacity 80 litres/24h at 30C/80% RH. Air flow 1,000 m3/h. Operating range 5-35C. Built-in hygrostat, continuous drainage option. Robust steel housing on wheels. Ideal for construction drying, water damage restoration, warehouse humidity control. Low operating hours, excellent condition." \
    "$CAT_CLIMATE_CONTROL" \
    850 \
    1200 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Manufacturer": "Master Climate Solutions", "Model": "DH 92", "Capacity": "80 L/24h", "Air Flow": "1,000 m3/h", "Operating Range": "5-35C", "Power": "1,500 W", "Weight": "55 kg", "Condition": "Used — Excellent"}'

# ---- Lot 5: Diesel Heater (no auction) ----
create_lot \
    "Dieselski grelec 30kW — Master B 100 CED Diesel Heater" \
    "Master B 100 CED direct-fired diesel heater with 30 kW heating capacity. Air flow 1,850 m3/h, fuel consumption 2.5 L/h. Built-in thermostat, flame safety device, tip-over switch. Stainless steel combustion chamber. Portable on integrated wheels. Perfect for construction site heating, workshop warming, temporary event spaces. Recently serviced, runs perfectly." \
    "$CAT_CLIMATE_CONTROL" \
    650 \
    900 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Manufacturer": "Master", "Model": "B 100 CED", "Heating Capacity": "30 kW", "Air Flow": "1,850 m3/h", "Fuel": "Diesel", "Consumption": "2.5 L/h", "Weight": "23 kg", "Condition": "Used — Serviced"}'

# ---- Lot 6: Metal Fencing Set (no auction) ----
create_lot \
    "Kovinska ograja ECONOMICO 30m komplet — Metal Fence Set" \
    "Complete 30-meter temporary metal fencing set ECONOMICO. Includes 15 mesh fence panels (2m x 1.1m each), 16 concrete bases (25 kg each), and 30 fixing clamps. Hot-dip galvanized steel, weather resistant. Easy assembly without tools — panels slide into bases. Ideal for construction site security, events, temporary barriers. Available for purchase or rental." \
    "$CAT_FENCING" \
    1200 \
    1600 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Type": "ECONOMICO Mesh Fence", "Total Length": "30 m", "Panel Size": "2.0 x 1.1 m", "Panels": "15", "Bases": "16 (25 kg each)", "Clamps": "30", "Material": "Hot-dip galvanized steel", "Assembly": "Tool-free"}'

# ---- Lot 7: Modular Double Office (no auction) ----
create_lot \
    "Modularna pisarna 2x 20ft — Double Office Module SPC DV K6" \
    "Double modular office container SPC DV K6, composed of two 20ft modules joined side-by-side. Total interior area approximately 28 m2. Galvanized steel frame, 60mm PUR insulated walls, 70mm stone wool roof insulation. Includes 2x aluminum doors, 4x PVC double-glazed windows, full electrical system with LED lighting. Optional sanitary corner available. Stackable up to 3 units high. Manufactured by SPC in Ljubljana." \
    "$CAT_MODULAR_STRUCTURES" \
    12500 \
    15000 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Manufacturer": "SPC d.o.o.", "Model": "SPC DV K6", "Configuration": "Double (2x 20ft)", "Interior Area": "~28 m2", "Wall Insulation": "60mm PUR", "Roof Insulation": "70mm Stone Wool", "Windows": "4x PVC double-glazed", "Doors": "2x Aluminum", "Stackable": "Up to 3 units"}'

# ---- Lot 8: Mini Excavator (no auction — cross-border lot from Zagreb) ----
create_lot \
    "Caterpillar mini bager 1.5t (2019) — Mini Excavator" \
    "Caterpillar 301.5 mini excavator, 2019 model with 1,800 operating hours. Operating weight 1,500 kg, digging depth 2.3 m. Rubber tracks, expandable undercarriage, ROPS/FOPS canopy. Equipped with standard bucket (300mm) and quick coupler. Full service history at authorized dealer. Low hours, excellent condition for landscaping, utility work, or confined spaces. Located in Zagreb, delivery to Slovenia available." \
    "$CAT_EXCAVATORS" \
    18000 \
    24000 \
    "Zagreb" \
    "HR" \
    "spc" \
    '{"Manufacturer": "Caterpillar", "Model": "301.5", "Year": "2019", "Operating Hours": "1,800 h", "Operating Weight": "1,500 kg", "Digging Depth": "2.3 m", "Tracks": "Rubber", "Bucket": "300 mm", "Condition": "Excellent"}'

# ---- Lot 9: AC Unit (pending approval — admin demo) ----
create_lot \
    "Klimatska naprava 5kW — Portable Air Conditioning Unit" \
    "Industrial portable air conditioning unit with 5 kW cooling capacity. Suitable for containers, temporary offices, server rooms, and workshops. Includes condensate pump and 3m flexible exhaust duct. Digital thermostat with timer function. Energy class A. Low noise operation (52 dB). Ready for plug-and-play use with standard 230V power." \
    "$CAT_CLIMATE_CONTROL" \
    1100 \
    1500 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Type": "Portable AC", "Cooling Capacity": "5 kW", "Power Supply": "230V", "Noise Level": "52 dB", "Energy Class": "A", "Exhaust Duct": "3 m flexible", "Weight": "38 kg", "Condition": "Used — Good"}'

# ---- Lot 10: Container House 40ft (pending approval — admin demo) ----
create_lot \
    "Kontejnerska hisa 40ft — Premium Container House" \
    "Premium 40ft container house conversion. Fully insulated living space with bedroom, kitchen area, bathroom with shower, and living/office area. Total area approximately 30 m2. Triple-glazed windows, underfloor heating preparation, LED lighting throughout. External cladding in timber composite. Connected to standard utilities (water, electricity, sewage). Turnkey solution — delivered fully finished. Ideal as holiday home, guest house, home office, or temporary accommodation." \
    "$CAT_MODULAR_STRUCTURES" \
    22000 \
    28000 \
    "Ljubljana" \
    "SI" \
    "spc" \
    '{"Type": "Container House Conversion", "Base": "40ft HC Container", "Area": "~30 m2", "Rooms": "Bedroom, Kitchen, Bathroom, Living/Office", "Insulation": "Full (walls, roof, floor)", "Windows": "Triple-glazed", "Heating": "Underfloor preparation", "Cladding": "Timber composite", "Condition": "New — Turnkey"}'

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
    "brand": "spc",
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

# Create auctions for SPC lots:
# Lot 0: Office container — active 24h
if [ ${#LOT_IDS[@]} -ge 1 ]; then
    create_auction "${LOT_IDS[0]}" "$START_TIME" "$END_TIME_1" 4500
fi
# Lot 1: Shipping container — active 48h
if [ ${#LOT_IDS[@]} -ge 2 ]; then
    create_auction "${LOT_IDS[1]}" "$START_TIME" "$END_TIME_2" 2800
fi
# Lot 2: Sanitary container — ending soon (anti-sniping demo!)
if [ ${#LOT_IDS[@]} -ge 3 ]; then
    create_auction "${LOT_IDS[2]}" "$START_TIME" "$END_TIME_SOON" 7200
fi
# Lot 3: Dehumidifier — active 24h
if [ ${#LOT_IDS[@]} -ge 4 ]; then
    create_auction "${LOT_IDS[3]}" "$START_TIME" "$END_TIME_1" 850
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

# Bids on Office Container (auction 0): EUR 4,600 -> 4,700 -> 4,900
if [ ${#AUCTION_IDS[@]} -ge 1 ]; then
    place_bid "${AUCTION_IDS[0]}" 4600 "$BUYER_TOKEN" "Office Container - Bid 1"
    sleep 0.5
    place_bid "${AUCTION_IDS[0]}" 4700 "$BUYER_TOKEN" "Office Container - Bid 2"
    sleep 0.5
    place_bid "${AUCTION_IDS[0]}" 4900 "$BUYER_TOKEN" "Office Container - Bid 3"
fi

# Bids on Sanitary Container (auction 2 — ending soon): EUR 7,300 -> 7,500
if [ ${#AUCTION_IDS[@]} -ge 3 ]; then
    place_bid "${AUCTION_IDS[2]}" 7300 "$BUYER_TOKEN" "Sanitary Container - Bid 1"
    sleep 0.5
    place_bid "${AUCTION_IDS[2]}" 7500 "$BUYER_TOKEN" "Sanitary Container - Bid 2"
fi

# Bids on Dehumidifier (auction 3): EUR 900 -> 950
if [ ${#AUCTION_IDS[@]} -ge 4 ]; then
    place_bid "${AUCTION_IDS[3]}" 900 "$BUYER_TOKEN" "Dehumidifier - Bid 1"
    sleep 0.5
    place_bid "${AUCTION_IDS[3]}" 950 "$BUYER_TOKEN" "Dehumidifier - Bid 2"
fi

# ---------------------------------------------------------------------------
# Step 8: Add watchlist items for buyer
# ---------------------------------------------------------------------------
log_info "Adding watchlist items for buyer..."

# Watchlist: Office Container (lot 0), Sanitary Container (lot 2), Modular Office (lot 6)
if [ ${#LOT_IDS[@]} -ge 7 ]; then
    api_call POST "/users/me/watchlist/${LOT_IDS[0]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    api_call POST "/users/me/watchlist/${LOT_IDS[2]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    api_call POST "/users/me/watchlist/${LOT_IDS[6]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    log_ok "Added 3 lots to buyer watchlist (Office Container, Sanitary Container, Modular Office)"
fi

# ---------------------------------------------------------------------------
# Done!
# ---------------------------------------------------------------------------
echo ""
echo "============================================="
echo -e "${GREEN}  SPC demo data seeded successfully!${NC}"
echo "============================================="
echo ""
echo "Summary:"
echo "  - ${#LOT_IDS[@]} SPC lots created (${APPROVED_COUNT} approved, $((TOTAL - APPROVED_COUNT)) pending)"
echo "  - ${#AUCTION_IDS[@]} auctions created (1 ending soon for anti-sniping demo)"
echo "  - Bids placed on active auctions"
echo "  - Buyer watchlist populated"
echo ""
echo "SPC Product Lines Seeded:"
echo "  - Office containers (Pisarniski kontejner 6m)"
echo "  - Shipping containers (Ladijski zabojnik 20ft HC)"
echo "  - Sanitary containers (Sanitarni kontejner SPC SAN C01)"
echo "  - Climate control (Dehumidifier, Diesel Heater, AC Unit)"
echo "  - Construction equipment (Metal Fencing ECONOMICO)"
echo "  - Modular structures (Double Office, Container House)"
echo "  - Construction machinery (Caterpillar Mini Excavator)"
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
