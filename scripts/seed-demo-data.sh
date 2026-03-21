#!/usr/bin/env bash
# =============================================================================
# seed-demo-data.sh — Populate the platform with 30 SPC demo lots + images
#
# Creates lots with real product images, auctions, and bids tailored for the
# SPC (Storitveno Prodajni Center) pitch demo: containers, climate control
# equipment, construction machinery, fencing.
#
# Prerequisites:
#   - Docker infrastructure running (Keycloak, PostgreSQL, NATS, MinIO, etc.)
#   - Backend services running (gateway, catalog, auction-engine, user, search, media)
#   - curl and jq installed
#   - assets/ folder with categorized product images
# =============================================================================

set -euo pipefail

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="auction-platform"
API="$GATEWAY_URL/api/v1"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ASSETS_DIR="${ASSETS_DIR:-$SCRIPT_DIR/../assets}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

log_info()  { echo -e "${BLUE}[INFO]${NC}  $*"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }
log_img()   { echo -e "${CYAN}[IMG]${NC}   $*"; }

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
# MinIO direct upload configuration
# Bypasses media-service presigned flow for reliability during seeding.
# Uploads images directly to MinIO via docker exec mc, then references the
# public URLs in lot creation. catalog-service stores whatever URL we pass.
# ---------------------------------------------------------------------------
MINIO_CONTAINER="auction-platform-minio"
MINIO_BUCKET="auction-media"
MINIO_SEED_PREFIX="uploads/seed"
MINIO_PUBLIC_URL="${MINIO_PUBLIC_URL:-http://localhost:9000}"

# Generate a deterministic UUID v5-style ID from a filename (for reproducibility)
gen_image_id() {
    echo "seed-img-$(echo -n "$1" | md5sum | cut -c1-32 | sed 's/\(........\)\(....\)\(....\)\(....\)\(............\)/\1-\2-\3-\4-\5/')"
}

# ---------------------------------------------------------------------------
# Helper: Upload all asset images to MinIO in a single batch
# ---------------------------------------------------------------------------
upload_all_assets_to_minio() {
    log_info "Uploading product images to MinIO..."

    # Ensure mc alias is configured with credentials
    docker exec "$MINIO_CONTAINER" mc alias set local http://localhost:9000 minioadmin minioadmin > /dev/null 2>&1 || {
        log_error "Failed to configure MinIO mc alias"
        return 1
    }

    # Clear any previous seed images
    docker exec "$MINIO_CONTAINER" mc rm --recursive --force "local/$MINIO_BUCKET/$MINIO_SEED_PREFIX/" > /dev/null 2>&1 || true

    local count=0
    for file_path in "$ASSETS_DIR"/*.{jpg,jpeg,png,webp} ; do
        [ -f "$file_path" ] || continue
        local filename
        filename=$(basename "$file_path")

        # Copy file into MinIO container, then mc cp to bucket
        docker cp "$file_path" "$MINIO_CONTAINER:/tmp/$filename" > /dev/null 2>&1 || continue
        docker exec "$MINIO_CONTAINER" mc cp "/tmp/$filename" "local/$MINIO_BUCKET/$MINIO_SEED_PREFIX/$filename" > /dev/null 2>&1 || {
            log_warn "  Failed to upload $filename to MinIO"
            continue
        }
        # Clean up temp file inside container
        docker exec "$MINIO_CONTAINER" rm -f "/tmp/$filename" > /dev/null 2>&1 || true
        count=$((count + 1))
    done

    log_ok "Uploaded $count images to MinIO ($MINIO_BUCKET/$MINIO_SEED_PREFIX/)"
}

# ---------------------------------------------------------------------------
# Helper: Build JSON images array for lot creation from filenames
# Usage: build_images_json file1 [file2 ...]
# Returns: JSON array like [{"id":"...","url":"http://localhost:9000/..."},...]
# ---------------------------------------------------------------------------
build_images_json() {
    local files=("$@")
    local images_json="["
    local first=true

    for filename in "${files[@]}"; do
        local img_id
        img_id=$(gen_image_id "$filename")
        local img_url="$MINIO_PUBLIC_URL/$MINIO_BUCKET/$MINIO_SEED_PREFIX/$filename"

        if [ "$first" = true ]; then
            first=false
        else
            images_json+=","
        fi
        images_json+="{\"id\":\"$img_id\",\"url\":\"$img_url\"}"
    done

    images_json+="]"
    echo "$images_json"
}

# ---------------------------------------------------------------------------
# Category IDs (SPC-specific categories from V003 migration)
# ---------------------------------------------------------------------------
CAT_OFFICE_CONTAINERS="30000000-0000-0000-0000-000000000001"
CAT_SHIPPING_CONTAINERS="30000000-0000-0000-0000-000000000002"
CAT_SANITARY_CONTAINERS="30000000-0000-0000-0000-000000000003"
CAT_STORAGE_CONTAINERS="30000000-0000-0000-0000-000000000004"
CAT_MODULAR_STRUCTURES="30000000-0000-0000-0000-000000000005"
CAT_CLIMATE_CONTROL="30000000-0000-0000-0000-000000000006"
CAT_CONSTRUCTION="30000000-0000-0000-0000-000000000007"
CAT_FENCING="30000000-0000-0000-0000-000000000008"

# =============================================================================
# MAIN
# =============================================================================
echo ""
echo "============================================="
echo "  SPC Aukcije — Demo Data Seeder (30 lots)"
echo "============================================="
echo ""

# Verify assets directory
if [ ! -d "$ASSETS_DIR" ]; then
    log_error "Assets directory not found: $ASSETS_DIR"
    log_error "Place product images in assets/ folder before running this script."
    exit 1
fi

IMAGE_COUNT=$(find "$ASSETS_DIR" -maxdepth 1 -type f \( -name "*.jpg" -o -name "*.jpeg" -o -name "*.png" -o -name "*.webp" \) | wc -l)
log_info "Found $IMAGE_COUNT product images in $ASSETS_DIR"

# Upload all images to MinIO first (single batch, fast)
upload_all_assets_to_minio

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

BUYER2_TOKEN=$(get_token "buyer-web" "buyer2@test.com") || {
    log_warn "buyer2@test.com token failed — will use single buyer for bids"
    BUYER2_TOKEN="$BUYER_TOKEN"
}

# ---------------------------------------------------------------------------
# Step 2: Ensure user profiles exist (auto-registration)
# ---------------------------------------------------------------------------
log_info "Ensuring user profiles exist..."
api_call GET "/users/me" "$SELLER_TOKEN" > /dev/null
api_call GET "/users/me" "$ADMIN_TOKEN" > /dev/null
api_call GET "/users/me" "$BUYER_TOKEN" > /dev/null
api_call GET "/users/me" "$BUYER2_TOKEN" > /dev/null 2>&1 || true
log_ok "User profiles ready"

# ---------------------------------------------------------------------------
# Step 3: Create 30 SPC demo lots with images
# ---------------------------------------------------------------------------
log_info "Creating 30 SPC demo lots with product images..."
echo ""

declare -a LOT_IDS=()
declare -a LOT_TITLES=()

# create_lot_with_images TITLE DESCRIPTION CATEGORY STARTING_BID RESERVE CITY COUNTRY BRAND SPECS IMAGE_FILENAME1 [IMAGE_FILENAME2 ...]
# Note: IMAGE_FILENAMEs are just basenames (e.g., "office_container_01.jpg"), not full paths.
# Images are already uploaded to MinIO in the batch step above.
create_lot_with_images() {
    local title="$1"
    local description="$2"
    local category_id="$3"
    local starting_bid="$4"
    local reserve_price="$5"
    local city="$6"
    local country="$7"
    local brand="${8:-spc}"
    local specs
    specs="${9:-"{}"}"
    shift 9
    local image_files=("$@")

    log_info "Creating: $title"

    # Build images JSON from filenames (already uploaded to MinIO)
    local images_json="[]"
    if [ ${#image_files[@]} -gt 0 ]; then
        images_json=$(build_images_json "${image_files[@]}")
    fi

    # Write payload to temp file (avoids heredoc/argument escaping issues)
    local tmpfile
    tmpfile=$(mktemp /tmp/seed-lot-XXXXXX.json)
    cat > "$tmpfile" <<EOJSON
{
    "brand": "$brand",
    "title": "$title",
    "description": "$description",
    "categoryId": "$category_id",
    "startingBid": $starting_bid,
    "reservePrice": $reserve_price,
    "locationCity": "$city",
    "locationCountry": "$country",
    "specifications": $specs,
    "images": $images_json
}
EOJSON

    # Send payload from file (bypasses bash argument length/escaping issues)
    local response
    response=$(curl -sf -X POST "$API/lots" \
        -H "Authorization: Bearer $SELLER_TOKEN" \
        -H "Content-Type: application/json" \
        -d @"$tmpfile" 2>/dev/null) || response='{"error": "request_failed"}'
    rm -f "$tmpfile"
    local lot_id
    lot_id=$(echo "$response" | jq -r '.data.id // .id // empty' 2>/dev/null)

    if [ -n "$lot_id" ] && [ "$lot_id" != "null" ]; then
        LOT_IDS+=("$lot_id")
        LOT_TITLES+=("$title")
        log_ok "  Created lot #${#LOT_IDS[@]}: $lot_id"
    else
        log_warn "  Lot creation failed: $title"
        log_warn "  Response: $(echo "$response" | head -c 200)"
    fi
    echo ""
}

# =====================================================================
# LOT 1: Office Container — SPC K6 Standard (ACTIVE auction, has bids)
# =====================================================================
create_lot_with_images \
    "Pisarniški kontejner 6m SPC K6 — Office Container Standard" \
    "Fully insulated standard office container SPC K6 with galvanized steel frame powder-coated RAL 7016 (anthracite grey). Dimensions 6058 x 2438 x 2800 mm. 60mm polyurethane wall insulation (U=0.37 W/m²K), 80mm stone wool roof insulation. Includes aluminum entry door with cylinder lock, 2x PVC double-glazed tilt-and-turn windows (1200 x 1000 mm), complete electrical system with distribution box, 3-phase 32A CEE connection, 6x Schuko outlets, 4x LED panel lights 40W, light switches. PVC vinyl flooring rated 250 kg/m². Stackable up to 3 units high. Ready for immediate delivery from Ljubljana warehouse." \
    "$CAT_OFFICE_CONTAINERS" \
    4500 5800 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"SPC d.o.o.","Model":"SPC K6","Dimensions":"6058 x 2438 x 2800 mm","Wall Insulation":"60mm PUR (U=0.37)","Roof Insulation":"80mm Stone Wool","Floor Capacity":"250 kg/m2","Electrical":"3-phase 32A CEE","Windows":"2x PVC double-glazed 1200x1000","Condition":"New","Stackable":"Up to 3 units","Year":"2025","Weight":"2,400 kg"}' \
    "office_container_01.jpg" "office_container_02.jpg"

# =====================================================================
# LOT 2: Office Container — Premium with AC
# =====================================================================
create_lot_with_images \
    "Pisarniški kontejner Premium z AC — Office Container Premium" \
    "Premium-grade office container with pre-installed split air conditioning (cooling + heating). Dimensions 6058 x 2438 x 2800 mm. Upgraded interior with laminate flooring, suspended ceiling with recessed LED lighting, and painted interior walls RAL 9010. Includes 2.5 kW Mitsubishi split AC unit, electric convector heater 2 kW backup, 4x double Schuko outlets, RJ45 network socket preparation. External finish RAL 7016 anthracite. Delivered with forklift pockets and crane lifting points. Ideal for site managers, architects, or extended project offices." \
    "$CAT_OFFICE_CONTAINERS" \
    6200 7800 "Maribor" "SI" "spc" \
    '{"Manufacturer":"SPC d.o.o.","Model":"SPC K6 Premium","Dimensions":"6058 x 2438 x 2800 mm","Insulation":"60mm PUR walls + 80mm roof","Air Conditioning":"Mitsubishi 2.5 kW split","Heating":"2 kW electric convector","Flooring":"Laminate","Ceiling":"Suspended with recessed LED","Condition":"New","Year":"2025"}' \
    "office_container_03.jpg"

# =====================================================================
# LOT 3: Portable Cabin — Site Security Guard House
# =====================================================================
create_lot_with_images \
    "Stražarska kabina — Security Guard House" \
    "Compact security guard house suitable for construction site entry checkpoints, parking facilities, and industrial gate control. Dimensions 3000 x 2438 x 2600 mm. Insulated sandwich panel walls 50mm, single entry door with deadbolt, 2x sliding windows with security bars, interior shelf/desk, LED ceiling light, single-phase electrical with 2 outlets. Floor-mounted on adjustable levelling feet. Finished in RAL 9002 grey-white. Low weight (850 kg) for easy crane placement." \
    "$CAT_OFFICE_CONTAINERS" \
    2200 2800 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"SPC d.o.o.","Type":"Guard House","Dimensions":"3000 x 2438 x 2600 mm","Wall Insulation":"50mm sandwich panel","Door":"1x with deadbolt","Windows":"2x sliding with security bars","Electrical":"Single-phase, 2 outlets","Weight":"850 kg","Condition":"New"}' \
    "portable_cabin_02.jpg"

# =====================================================================
# LOT 4: Portable Cabin — Mobile Office on Wheels
# =====================================================================
create_lot_with_images \
    "Mobilna pisarna na kolesih — Portable Office on Wheels" \
    "Mobile office cabin on integrated chassis with rubber wheels for easy relocation. Dimensions 6058 x 2438 x 2800 mm (including chassis). No crane required — tow with standard vehicle or forklift. Insulated walls and roof, 2x windows, entry door with steps, full electrical installation. Interior includes desk area and storage shelf. Ideal for construction companies needing frequently relocated site offices. Used condition, recently refurbished interior, new vinyl flooring installed." \
    "$CAT_OFFICE_CONTAINERS" \
    3800 4800 "Celje" "SI" "spc" \
    '{"Type":"Mobile Office on Wheels","Dimensions":"6058 x 2438 x 2800 mm","Chassis":"Integrated with rubber wheels","Transport":"Towable, no crane needed","Windows":"2x PVC","Insulation":"Sandwich panel 50mm","Flooring":"New vinyl","Condition":"Used — Refurbished","Year":"2021"}' \
    "portable_cabin_01.jpeg"

# =====================================================================
# LOT 5: Portable Cabin — Compact 3m Entry Level
# =====================================================================
create_lot_with_images \
    "Kontejner pisarna 3m — Compact Office Container 10ft" \
    "Compact 10ft (3m) portable office container, ideal for small construction sites, temporary ticket booths, or storage with workspace. Dimensions 2991 x 2438 x 2591 mm. Single entry door, 1x PVC window. Basic electrical: ceiling LED light, 2x outlets, external CEE inlet. Flat-pack folding design allows efficient transport — 4 units stack on a single flatbed. Galvanized steel base frame. White exterior finish." \
    "$CAT_OFFICE_CONTAINERS" \
    1800 2400 "Koper" "SI" "spc" \
    '{"Type":"Compact Office Container","Dimensions":"2991 x 2438 x 2591 mm","Design":"Flat-pack folding","Transport":"4 units per flatbed","Door":"1x entry","Window":"1x PVC","Electrical":"LED + 2 outlets","Weight":"1,200 kg","Condition":"New"}' \
    "portable_cabin_04.jpg"

# =====================================================================
# LOT 6: Portable Cabin — Folding Expandable Container
# =====================================================================
create_lot_with_images \
    "Zložljiv kontejner — Folding Expandable Container Office" \
    "Innovative folding container that expands from transport size to full 20ft office in under 10 minutes. Patented hydraulic folding mechanism — no crane required for setup. Transport dimensions 6058 x 2438 x 580 mm (collapsed). Expanded dimensions 6058 x 2438 x 2600 mm. Sandwich panel walls with 50mm EPS insulation, PVC vinyl floor, 2x windows, 1x entry door. Pre-wired electrical system activates automatically during unfolding. Revolutionary logistics savings — transport 6 units where normally only 1 fits." \
    "$CAT_OFFICE_CONTAINERS" \
    5500 6800 "Ljubljana" "SI" "spc" \
    '{"Type":"Folding Expandable Container","Collapsed":"6058 x 2438 x 580 mm","Expanded":"6058 x 2438 x 2600 mm","Setup Time":"< 10 minutes","Crane Required":"No","Insulation":"50mm EPS sandwich","Transport Ratio":"6:1 vs standard","Condition":"New","Year":"2025"}' \
    "portable_cabin_03.jpg"

# =====================================================================
# LOT 7: Shipping Container 20ft HC — Used (ACTIVE auction, 48h)
# =====================================================================
create_lot_with_images \
    "Ladijski zabojnik 20ft HC — 20ft High Cube Shipping Container" \
    "Used 20ft High Cube ISO shipping container in good structural condition, CSC-certified. External dimensions 6058 x 2438 x 2896 mm. Internal volume 38.5 m³. Double-wing cargo doors with original locking hardware. 28mm bamboo-laminated marine plywood floor rated for forklift entry. Corner castings for standard container handling. Minor surface corrosion patched and treated with marine-grade primer. Wind and water tight certified (WWT). Ideal for on-site storage, workshop conversion, or continued shipping use. Located at Ljubljana IOC Rudnik — delivery within Slovenia available." \
    "$CAT_SHIPPING_CONTAINERS" \
    2800 3500 "Ljubljana" "SI" "spc" \
    '{"Type":"20ft High Cube","ISO Code":"22G1","External":"6058 x 2438 x 2896 mm","Internal Volume":"38.5 m3","Doors":"Double-wing cargo","Floor":"28mm bamboo marine plywood","Max Payload":"28,200 kg","Tare Weight":"2,300 kg","Certification":"CSC + WWT","Condition":"Used — Good","Year":"2018"}' \
    "shipping_container_03.jpg" "shipping_container_01.jpg"

# =====================================================================
# LOT 8: Shipping Container 20ft — Converted with Windows
# =====================================================================
create_lot_with_images \
    "Predelan zabojnik 20ft z okni — Converted Container with Windows" \
    "20ft shipping container professionally converted with window cutouts and reinforced steel lintels. Features 3x double-glazed windows (800 x 600 mm) with security shutters, personnel entry door with 3-point locking, and original cargo doors retained at rear. Painted RAL 7024 graphite grey exterior. Basic electrical pre-wiring (conduit only, no fixtures). Suitable as workshop, hobby space, or base for further conversion. Structurally certified after modification." \
    "$CAT_SHIPPING_CONTAINERS" \
    4200 5200 "Zagreb" "HR" "spc" \
    '{"Type":"20ft Standard — Converted","External":"6058 x 2438 x 2591 mm","Modifications":"3x windows, 1x personnel door, electrical conduit","Windows":"3x 800x600 double-glazed with shutters","Door":"Personnel 3-point lock + original cargo doors","Finish":"RAL 7024 Graphite Grey","Certification":"Post-modification structural cert","Condition":"Used — Modified","Year":"2019"}' \
    "shipping_container_02.jpg"

# =====================================================================
# LOT 9: Shipping Container 40ft HC — Standard
# =====================================================================
create_lot_with_images \
    "Ladijski zabojnik 40ft HC — 40ft High Cube Container" \
    "Standard 40ft High Cube shipping container in cargo-worthy condition. External dimensions 12192 x 2438 x 2896 mm, internal volume 76.3 m³. Double-wing cargo doors with 4x cross-bar locking mechanism. Hardwood floor, 14 tie-down points. Recently de-registered from active shipping — clean interior, no chemical residue. Minor dents on side panels (cosmetic only, structurally sound). One-trip paint refresh available on request. Bulk pricing available for 5+ units." \
    "$CAT_SHIPPING_CONTAINERS" \
    3500 4500 "Koper" "SI" "spc" \
    '{"Type":"40ft High Cube","ISO Code":"45G1","External":"12192 x 2438 x 2896 mm","Internal Volume":"76.3 m3","Doors":"Double-wing 4x cross-bar","Floor":"Hardwood","Max Payload":"26,580 kg","Tare Weight":"3,900 kg","Tie-downs":"14 points","Condition":"Cargo-Worthy","Year":"2020"}' \
    "shipping_container_03.jpg"

# =====================================================================
# LOT 10: Sanitary Container — SPC SAN C01 (ACTIVE auction, ending soon)
# =====================================================================
create_lot_with_images \
    "Sanitarni kontejner SPC SAN C01 — Sanitary Container with Shower" \
    "Complete sanitary container SPC SAN C01 manufactured in-house by SPC Ljubljana. Features 2x WC cabins with ceramic toilets, 2x ceramic wash basins with mixer taps, 1x urinal, and dedicated shower cabin with thermostatic mixer. Dimensions 3130 x 2400 x 2715 mm. 4 separate entry doors, 2x opaque sanitary windows for ventilation. Full mechanical installation: cold/hot water supply, drainage, 5L Gorenje electric water heater. Full electrical: LED lighting, extractor fan, GFCI-protected outlets. Hot-dip galvanized steel frame with 50mm insulated sandwich panels. Forklift pockets and crane lifting eyes. Suitable for construction sites, outdoor events, sports facilities." \
    "$CAT_SANITARY_CONTAINERS" \
    7200 8500 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"SPC d.o.o.","Model":"SPC SAN C01","Dimensions":"3130 x 2400 x 2715 mm","WC Cabins":"2x ceramic","Wash Basins":"2x with mixer taps","Urinals":"1","Shower":"1x thermostatic","Water Heater":"Gorenje 5L electric","Doors":"4 separate entries","Ventilation":"2x windows + extractor fan","Frame":"Hot-dip galvanized steel","Condition":"New","Year":"2025"}' \
    "sanitary_container_01.jpg" "sanitary_container_03.webp"

# =====================================================================
# LOT 11: Sanitary Container — Twin WC Unit (green, open doors)
# =====================================================================
create_lot_with_images \
    "Dvojni WC kontejner 8ft — Twin Toilet Container Unit" \
    "Compact 8ft twin toilet container with 2 independent WC cabins. Each cabin equipped with ceramic toilet, small wash basin, mirror, and coat hook. Shared water supply and waste connection at rear panel. Dimensions 2438 x 2200 x 2591 mm. Steel construction powder-coated RAL 6005 (moss green). Ventilation louvers in each door. Floor-level entry (wheelchair accessible with ramp). Low water consumption dual-flush cisterns. Previously used on residential construction project — cleaned, sanitized, and inspected. Minor cosmetic wear on exterior." \
    "$CAT_SANITARY_CONTAINERS" \
    2800 3600 "Graz" "AT" "spc" \
    '{"Type":"Twin WC Container","Dimensions":"2438 x 2200 x 2591 mm","WC Cabins":"2 independent","Toilets":"Ceramic dual-flush","Basins":"2x small","Finish":"RAL 6005 Moss Green","Water Connection":"Rear panel","Accessibility":"Floor-level entry","Condition":"Used — Good","Year":"2022"}' \
    "portable_toilet_03.jpg" "sanitary_container_02.jpg"

# =====================================================================
# LOT 12: Sanitary Container — 4-Stall Portable Toilet Block
# =====================================================================
create_lot_with_images \
    "Sanitarni blok 4 kabine — 4-Stall Portable Toilet Block" \
    "Large-capacity 4-stall portable toilet block for high-traffic construction sites or events. Each stall with ceramic toilet, individual ventilation, and privacy lock indicator. Central wash area with 2x basins and soap dispenser. Dimensions 6058 x 2438 x 2600 mm. Blue/white steel frame construction. Includes 50L hot water boiler, LED lighting in all compartments, external maintenance access panel. Water inlet/outlet at ground level. Can serve 40-60 workers per shift. Brand new, manufactured Q1 2025." \
    "$CAT_SANITARY_CONTAINERS" \
    8500 10500 "Ljubljana" "SI" "spc" \
    '{"Type":"4-Stall Toilet Block","Dimensions":"6058 x 2438 x 2600 mm","Stalls":"4 independent","Wash Basins":"2x central","Hot Water":"50L boiler","Lighting":"LED all compartments","Capacity":"40-60 workers/shift","Water Connection":"Ground level","Condition":"New","Year":"2025"}' \
    "portable_toilet_01.jpg" "portable_toilet_02.jpg"

# =====================================================================
# LOT 13: Sanitary Container — WC + Shower Combo
# =====================================================================
create_lot_with_images \
    "WC + tuš kontejner 8ft — Toilet & Shower Combo Container" \
    "Combined 8ft toilet and shower container. Left cabin: WC with ceramic toilet, wash basin, and mirror. Right cabin: full shower with thermostatic mixer, drain, and curtain rail. Shared 15L electric water heater mounted internally. Dimensions 2438 x 2200 x 2591 mm. Blue powder-coated steel exterior. Transparent glass entry doors for natural lighting. Anti-slip flooring in both compartments. Ideal for small construction teams (4-8 workers), temporary accommodation support, or camping facilities." \
    "$CAT_SANITARY_CONTAINERS" \
    3200 4200 "Ljubljana" "SI" "spc" \
    '{"Type":"WC + Shower Combo","Dimensions":"2438 x 2200 x 2591 mm","WC":"1x ceramic with basin","Shower":"1x thermostatic","Water Heater":"15L electric","Flooring":"Anti-slip","Entry":"Glass doors","Capacity":"4-8 workers","Condition":"Used — Excellent","Year":"2023"}' \
    "sanitary_container_04.jpg"

# =====================================================================
# LOT 14: Industrial Dehumidifier — Large Unit (ACTIVE auction, has bids)
# =====================================================================
create_lot_with_images \
    "Industrijski razvlaževalec 960L — Large Industrial Dehumidifier" \
    "High-capacity industrial condensation dehumidifier rated 960 litres per day at standard conditions (30°C, 80% RH). Designed for large-area drying in warehouses, production halls, and post-flood restoration. Air flow 6,500 m³/h via dual centrifugal fans. Operating temperature range 5-38°C. Automatic defrost function. Built-in digital hygrostat with programmable target humidity (30-90% RH). Continuous drainage via 32mm outlet + optional condensate pump. Heavy-duty steel cabinet on 4x swivel casters with brakes. 3-phase 400V power supply, 4.2 kW consumption. Control panel with LED display, operating hours counter, and fault diagnostics." \
    "$CAT_CLIMATE_CONTROL" \
    4200 5500 "Ljubljana" "SI" "spc" \
    '{"Type":"Industrial Condensation Dehumidifier","Capacity":"960 L/24h","Air Flow":"6,500 m3/h","Operating Range":"5-38°C","Power":"3-phase 400V, 4.2 kW","Humidity Control":"30-90% RH digital","Drainage":"32mm continuous + pump option","Weight":"145 kg","Dimensions":"1200 x 800 x 1600 mm","Condition":"Used — Excellent","Hours":"~2,000 h"}' \
    "industrial_dehumidifier_03.webp" "industrial_dehumidifier_01.jpg"

# =====================================================================
# LOT 15: Industrial Dehumidifier — Commercial 63L Costway
# =====================================================================
create_lot_with_images \
    "Komercialni razvlaževalec 63L — Commercial Dehumidifier with Pump" \
    "Commercial-grade portable dehumidifier with 63 litres/day extraction capacity at 35°C/90% RH. Built-in condensate pump with 6m vertical lift for flexible drainage routing. 5.5L collection tank with auto-shutoff as backup. Digital control panel with humidity display, 24h timer, and continuous mode. Ergonomic handle and 4x caster wheels for easy repositioning. Coverage area up to 200 m². Ideal for basements, warehouses, container storage, and construction drying. Energy-efficient compressor with R410a refrigerant. Orange/black industrial design." \
    "$CAT_CLIMATE_CONTROL" \
    450 650 "Maribor" "SI" "spc" \
    '{"Type":"Commercial Dehumidifier","Capacity":"63 L/24h","Air Flow":"265 CFM","Coverage":"200 m2","Tank":"5.5L with auto-shutoff","Pump":"Built-in, 6m lift","Refrigerant":"R410a","Power":"230V, 860W","Weight":"42 kg","Condition":"Used — Good","Year":"2023"}' \
    "industrial_dehumidifier_04.webp"

# =====================================================================
# LOT 16: Industrial Dehumidifier — Heavy-Duty Floor Standing
# =====================================================================
create_lot_with_images \
    "Profesionalni razvlaževalec — Professional Floor-Standing Dehumidifier" \
    "Professional floor-standing industrial dehumidifier for demanding environments. Stainless steel cabinet construction resistant to chemical exposure and high-humidity corrosion. Dehumidification capacity rated for spaces up to 350 m². Features automatic humidistat, hot-gas defrost for low-temperature operation down to 1°C, and dual drainage ports. Three-speed fan with quiet night mode (48 dB). Front-mounted gauges for refrigerant pressure monitoring. Service-friendly design with removable side panels for compressor access. Mounted on reinforced rubber wheels for warehouse floor protection." \
    "$CAT_CLIMATE_CONTROL" \
    1800 2400 "Wien" "AT" "spc" \
    '{"Type":"Professional Floor-Standing","Coverage":"350 m2","Min Operating Temp":"1°C","Noise Level":"48 dB (quiet mode)","Cabinet":"Stainless steel","Defrost":"Hot-gas automatic","Drainage":"Dual ports","Fan Speeds":"3 + night mode","Power":"230V, 1,650W","Condition":"Used — Serviced","Weight":"68 kg"}' \
    "industrial_dehumidifier_02.jpg" "industrial_dehumidifier_05.png"

# =====================================================================
# LOT 17: Diesel Heater — SIP Fireball Direct-Fired
# =====================================================================
create_lot_with_images \
    "SIP Fireball dieselski grelec — Direct-Fired Diesel/Paraffin Heater" \
    "SIP Fireball XD215 direct-fired diesel/paraffin space heater, 63 kW (215,000 BTU) output. Massive air flow for rapid heating of large open spaces, workshops, and partially enclosed construction areas. Thermostat-controlled with adjustable flame. Safety features: tip-over switch, overheat protection, flame failure device. Stainless steel combustion chamber for long life. Large 56-litre fuel tank provides 13+ hours continuous operation. Pneumatic wheels and lifting handle for one-person mobility. Note: direct-fired heaters produce combustion gases — use in ventilated spaces only." \
    "$CAT_CLIMATE_CONTROL" \
    650 850 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"SIP Industrial","Model":"Fireball XD215","Heat Output":"63 kW (215,000 BTU)","Fuel":"Diesel / Kerosene","Tank Capacity":"56 L","Burn Time":"13+ hours","Thermostat":"Yes, adjustable","Safety":"Tip-over + overheat + flame failure","Type":"Direct-fired","Ventilation":"Required","Condition":"Used — Good","Weight":"36 kg"}' \
    "diesel_heater_02.jpg"

# =====================================================================
# LOT 18: Diesel Heater — Allmand Maxi-Heat 1M BTU Indirect
# =====================================================================
create_lot_with_images \
    "Allmand Maxi-Heat 1M BTU — Indirect-Fired Mobile Diesel Heater" \
    "Allmand Maxi-Heat 1,000,000 BTU (293 kW) indirect-fired mobile diesel heater on trailer chassis. Heat exchanger design produces clean, fume-free hot air safe for enclosed occupied spaces. Dual 12-inch (305mm) discharge outlets for flexible duct routing. Diesel-powered with 400L integrated fuel tank — runs 48+ hours unattended. Electric start with 12V battery. Digital thermostat with remote sensor option. DOT-compliant road-towable trailer with highway lights. Ideal for large commercial construction projects, concrete curing, event tents, emergency heating. Recently serviced — new ignition module and fuel filters installed." \
    "$CAT_CLIMATE_CONTROL" \
    8500 11000 "Zagreb" "HR" "spc" \
    '{"Manufacturer":"Allmand","Model":"Maxi-Heat MH1000","Heat Output":"1,000,000 BTU (293 kW)","Type":"Indirect-fired (fume-free)","Fuel":"Diesel","Tank":"400L integrated","Run Time":"48+ hours","Outlets":"2x 12-inch (305mm)","Trailer":"DOT road-towable","Start":"Electric 12V","Condition":"Used — Serviced","Year":"2020","Hours":"~3,200 h"}' \
    "diesel_heater_03.jpg"

# =====================================================================
# LOT 19: Space Heater — HPL Herman Nelson Trailer-Mounted
# =====================================================================
create_lot_with_images \
    "HPL Herman Nelson 600D grelec — Trailer-Mounted Space Heater" \
    "HPL Herman Nelson 600 D7 all-in-one indirect-fired space heater with integrated LED light tower. 600,000 BTU (176 kW) clean heat output via heat exchanger — safe for occupied enclosed spaces. Trailer-mounted for easy deployment. Features: 300L diesel tank, electric start, digital thermostat, 2x 10-inch duct outlets, integrated 4x LED floodlight tower (retracting mast). Dual function: heating + site illumination in one unit. Commonly used on Canadian/Nordic construction sites for winter operations. Low hours, well-maintained unit from rental fleet." \
    "$CAT_CLIMATE_CONTROL" \
    12000 15000 "München" "DE" "spc" \
    '{"Manufacturer":"HPL / Herman Nelson","Model":"600 D7","Heat Output":"600,000 BTU (176 kW)","Type":"Indirect-fired + LED tower","Fuel":"Diesel","Tank":"300L","Outlets":"2x 10-inch","Light Tower":"4x LED floodlight, retractable mast","Trailer":"Integrated, towable","Condition":"Used — Fleet maintained","Year":"2021","Hours":"~2,500 h"}' \
    "space_heater_industrial_01.webp" "space_heater_industrial_02.webp"

# =====================================================================
# LOT 20: Space Heater — Thermobile Indirect ITA-75
# =====================================================================
create_lot_with_images \
    "Thermobile ITA-75 indirektni grelec — Indirect Space Heater" \
    "Thermobile ITA-75 indirect-fired oil/diesel space heater. 75 kW heat output through stainless steel heat exchanger — exhaust gases vented externally via chimney flue, making it safe for enclosed occupied spaces. High-volume radial fan delivers 3,500 m³/h heated air. Built-in room thermostat and safety pressure switch. Dual-fuel capable (diesel or heating oil). Robust red powder-coated steel chassis on pneumatic wheels for site mobility. Easy-access service panels for burner maintenance. CE certified. Widely used in European construction, agriculture (polytunnels), and event/marquee heating." \
    "$CAT_CLIMATE_CONTROL" \
    1800 2400 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"Thermobile","Model":"ITA-75","Heat Output":"75 kW","Type":"Indirect-fired","Air Flow":"3,500 m3/h","Fuel":"Diesel / Heating Oil","Flue":"External chimney","Thermostat":"Built-in room","Safety":"Pressure switch","Weight":"95 kg","Condition":"Used — Good","Year":"2020"}' \
    "space_heater_industrial_03.jpg" "space_heater_industrial_04.jpg"

# =====================================================================
# LOT 21: Space Heater — Wacker Neuson HI 400 Indirect
# =====================================================================
create_lot_with_images \
    "Wacker Neuson HI 400 — Indirect Diesel Heater" \
    "Wacker Neuson HI 400 HD indirect-fired diesel heater with 110 kW heat output. Designed for demanding construction site heating — concrete curing, thawing frozen ground, heating temporary structures. Clean hot air output via heat exchanger with chimney exhaust. 12-inch (305mm) discharge duct connection. 200L integrated fuel tank for 20+ hours continuous operation. Electric start, built-in thermostat, flame sensor, and overheat protection. Heavy-duty frame on pneumatic wheels. Previously in rental fleet — regularly maintained per manufacturer schedule. Some cosmetic wear on exterior (dents, paint chips) but mechanically sound. Starts and runs perfectly." \
    "$CAT_CLIMATE_CONTROL" \
    3200 4200 "Linz" "AT" "spc" \
    '{"Manufacturer":"Wacker Neuson","Model":"HI 400 HD","Heat Output":"110 kW (375,000 BTU)","Type":"Indirect-fired","Fuel":"Diesel","Tank":"200L","Run Time":"20+ hours","Duct":"305mm (12-inch)","Start":"Electric","Safety":"Thermostat + flame sensor + overheat","Condition":"Used — Rental fleet maintained","Year":"2019","Hours":"~4,500 h"}' \
    "diesel_heater_04.jpg" "diesel_heater_01.jpg"

# =====================================================================
# LOT 22: Temporary Fence — 30m Construction Fence Set
# =====================================================================
create_lot_with_images \
    "Gradbena ograja ECONOMICO 30m — Temporary Construction Fence Set" \
    "Complete 30-meter temporary construction fence set ECONOMICO, ready to deploy. Kit includes: 15x welded mesh fence panels (2000 x 1100 mm, wire diameter 3.5mm, mesh 60 x 150 mm), 16x concrete feet bases (each 25 kg, designed for panel overlap), 30x anti-lift clamps for panel-to-base security. All components hot-dip galvanized to EN ISO 1461 standard — fully weather resistant, no maintenance required. Tool-free assembly: panels slide into base slots, clamps hand-tighten. One person can erect 30m in approximately 30 minutes. Ideal for construction site perimeters, event barriers, property security during renovation. Can be extended with additional panel sets. Available for purchase or short/long-term rental." \
    "$CAT_FENCING" \
    1200 1600 "Ljubljana" "SI" "spc" \
    '{"Type":"ECONOMICO Welded Mesh Fence","Total Length":"30 m","Panel Size":"2000 x 1100 mm","Wire Diameter":"3.5 mm","Mesh Size":"60 x 150 mm","Panels":"15","Concrete Bases":"16 (25 kg each)","Anti-lift Clamps":"30","Material":"Hot-dip galvanized EN ISO 1461","Assembly":"Tool-free, ~30 min","Condition":"New"}' \
    "temporary_fence_construction_01.webp"

# =====================================================================
# LOT 23: Modular Office — 2-Story Glass-Front Building
# =====================================================================
create_lot_with_images \
    "Dvonadstropna modularna pisarna — 2-Story Modular Office Building" \
    "Impressive 2-story modular office building constructed from 8x 20ft container modules (4 per floor). Total usable office area approximately 112 m². Ground floor: open-plan office space with full glass front facade, reception area, 2x meeting rooms. First floor: 4x individual offices with balcony terrace access. Features: aluminum-framed triple-glazed glass curtain wall, internal staircase, disabled access ramp, full HVAC system (heating + cooling), LED lighting throughout, cat6 network cabling, fire alarm system. External finish: RAL 7016 anthracite with anodized aluminum window frames. Currently assembled and operational — can be disassembled and relocated. Ideal for construction project HQ, temporary corporate offices, or school/clinic expansion." \
    "$CAT_MODULAR_STRUCTURES" \
    85000 110000 "Ljubljana" "SI" "spc" \
    '{"Type":"2-Story Modular Office","Modules":"8x 20ft (4 per floor)","Total Area":"~112 m2","Ground Floor":"Open-plan + reception + 2x meeting rooms","First Floor":"4x offices + balcony terrace","Facade":"Triple-glazed aluminum curtain wall","HVAC":"Full heating + cooling","Network":"Cat6 cabled","Fire Safety":"Alarm system","Finish":"RAL 7016 + anodized aluminum","Condition":"Used — Operational","Year":"2022"}' \
    "modular_office_building_01.jpg"

# =====================================================================
# LOT 24: Modular Office — Modern Prefab Commercial Building
# =====================================================================
create_lot_with_images \
    "Sodobna modularna pisarna — Modern Prefab Commercial Office" \
    "Architecturally designed modern prefab commercial office building. Single-story configuration with floor-to-ceiling glazing on two sides for maximum natural light. Total area approximately 45 m². Open-plan interior with kitchenette corner and separate WC. Premium finishes: engineered oak flooring, suspended acoustic ceiling, custom LED lighting design. HVAC via concealed ceiling cassette. External: composite cladding panels in dark grey/wood combination. Fully insulated to current building regulations. Delivered on a single wide-load transport and craned into position. Foundation requirements: level concrete pad or screw piles. Previously used as sales office — immaculate condition." \
    "$CAT_MODULAR_STRUCTURES" \
    35000 45000 "Maribor" "SI" "spc" \
    '{"Type":"Modern Prefab Office","Area":"~45 m2","Glazing":"Floor-to-ceiling, 2 sides","Flooring":"Engineered oak","Ceiling":"Suspended acoustic","HVAC":"Ceiling cassette concealed","Cladding":"Composite dark grey + wood","WC":"Included","Kitchenette":"Included","Foundation":"Concrete pad or screw piles","Condition":"Used — Immaculate","Year":"2023"}' \
    "portable_cabin_05.jpg"

# =====================================================================
# LOT 25: Modular Office — Red-Framed 2-Story Warehouse Office
# =====================================================================
create_lot_with_images \
    "Industrijska modularna pisarna — Industrial 2-Story Modular Office" \
    "Industrial-grade 2-story modular office system designed for integration within warehouse and factory environments. Red powder-coated steel frame with large glass panels for supervisory oversight of production/warehouse floor. Configuration: 4x modules (2 per floor) creating approximately 56 m² total office space. Internal steel staircase with anti-slip treads. Each module independently climate-controlled. Acoustic insulation rated for industrial noise environments (Rw 42 dB). Cable management channels integrated into frame for power, data, and compressed air. Bolt-together assembly — can be reconfigured or relocated as facility needs change. Includes fire-rated internal partition walls." \
    "$CAT_MODULAR_STRUCTURES" \
    42000 55000 "Celje" "SI" "spc" \
    '{"Type":"Industrial Warehouse Office","Configuration":"2-story, 4 modules","Area":"~56 m2","Frame":"Red powder-coated steel","Glazing":"Large panels, supervisory view","Staircase":"Internal steel, anti-slip","Acoustic":"Rw 42 dB industrial rated","Assembly":"Bolt-together, reconfigurable","Fire Rating":"Fire-rated partitions","Cable Management":"Integrated channels","Condition":"Used — Good","Year":"2021"}' \
    "modular_office_building_04.webp"

# =====================================================================
# LOT 26: Mini Excavator — CAT 302 (1.5t)
# =====================================================================
create_lot_with_images \
    "CAT 302 mini bager 1.5t — Caterpillar Mini Excavator" \
    "Caterpillar 302 CR mini excavator, 2020 model with 2,100 operating hours. Operating weight 1,700 kg, rated engine power 14.3 kW (Yanmar diesel). Maximum digging depth 2,340 mm, maximum reach 3,980 mm. Rubber tracks (new set installed at 1,800h), expandable undercarriage from 980 to 1,280 mm for stability in confined spaces. ROPS/FOPS certified cab with sliding door. Equipped with: 300mm digging bucket, quick coupler ready, dozer blade 1,080 mm. Full Caterpillar service history — all maintenance performed at authorized dealer on schedule. Excellent condition for age — no leaks, strong hydraulics, clean cab. Ideal for landscaping, utility trenching, indoor demolition, or tight-access foundation work." \
    "$CAT_CONSTRUCTION" \
    22000 28000 "Zagreb" "HR" "spc" \
    '{"Manufacturer":"Caterpillar","Model":"302 CR","Year":"2020","Operating Hours":"2,100 h","Operating Weight":"1,700 kg","Engine":"Yanmar 14.3 kW diesel","Max Dig Depth":"2,340 mm","Max Reach":"3,980 mm","Tracks":"Rubber (new at 1,800h)","Undercarriage":"Expandable 980-1,280 mm","Cab":"ROPS/FOPS sliding door","Bucket":"300 mm","Dozer Blade":"1,080 mm","Quick Coupler":"Ready","Condition":"Excellent"}' \
    "mini_excavator_01.jpg" "cat_excavator_02.jpeg"

# =====================================================================
# LOT 27: Mini Excavator — CAT 303.5 (3.5t)
# =====================================================================
create_lot_with_images \
    "CAT 303.5 mini bager 3.5t — Caterpillar Compact Excavator" \
    "Caterpillar 303.5E2 CR compact excavator, 2019 model with 3,400 operating hours. Operating weight 3,590 kg, Cat C1.7 diesel engine 18.4 kW. Maximum digging depth 3,230 mm. Fixed undercarriage with rubber tracks (60% remaining), cab with heat and air conditioning. Equipped with: 450mm GP bucket, hydraulic thumb, pattern changer (ISO/SAE switchable), 2-way auxiliary hydraulics for attachments. Standard dozer blade. Recent service: new hydraulic oil and filters at 3,200h, swing bearing greased, all cylinders inspected — no drift. Some scratches on cab glass (visibility OK) and paint wear on boom. Mechanically very strong machine. Delivery available across Slovenia, Croatia, and Austria." \
    "$CAT_CONSTRUCTION" \
    32000 40000 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"Caterpillar","Model":"303.5E2 CR","Year":"2019","Operating Hours":"3,400 h","Operating Weight":"3,590 kg","Engine":"Cat C1.7, 18.4 kW","Max Dig Depth":"3,230 mm","Tracks":"Rubber (60% remaining)","Cab":"Heat + AC","Bucket":"450mm GP","Attachments":"Hydraulic thumb, pattern changer, 2-way aux","Dozer Blade":"Standard","Condition":"Good — Mechanically strong"}' \
    "cat_excavator_01.jpeg"

# =====================================================================
# LOT 28: Portable AC — Americool 23,000 BTU
# =====================================================================
create_lot_with_images \
    "Americool WPC-23000 industrijska klima — Industrial Portable AC" \
    "Americool WPC-23000 industrial portable air conditioner with 23,000 BTU (6.7 kW) cooling capacity. Three independent cooling outlets (top-mounted nozzles) for flexible airflow direction — cool multiple workstations or zones simultaneously. Compact footprint on heavy-duty casters. Self-contained system — no external condenser unit needed, just power and a vent path for hot air exhaust. Digital thermostat with auto-restart after power failure. Coverage area: 45-60 m². Suitable for server rooms, container offices, industrial workstations, temporary medical facilities. Uses R-410A refrigerant. Low operating hours, excellent condition." \
    "$CAT_CLIMATE_CONTROL" \
    2200 2900 "Ljubljana" "SI" "spc" \
    '{"Manufacturer":"Americool","Model":"WPC-23000","Cooling Capacity":"23,000 BTU (6.7 kW)","Outlets":"3x independent nozzles","Coverage":"45-60 m2","Type":"Self-contained portable","Refrigerant":"R-410A","Power":"230V, 2.8 kW","Thermostat":"Digital with auto-restart","Weight":"95 kg","Condition":"Used — Excellent","Year":"2022"}' \
    "portable_air_conditioner_industrial_02.webp"

# =====================================================================
# LOT 29: Portable AC — Industrial Multi-Duct Unit
# =====================================================================
create_lot_with_images \
    "Industrijska prenosna klima 5kW — Portable Industrial Air Conditioner" \
    "Heavy-duty portable industrial air conditioner with 5 kW (17,000 BTU) cooling capacity. Two-duct system: 2x flexible cold air outlets (150mm diameter) for spot cooling of equipment or personnel. Rear-mounted hot air exhaust duct (200mm). Steel cabinet construction on 4x swivel casters with brakes — designed for factory floor and workshop use. Features: washable air filter, condensate collection tray with float switch, adjustable thermostat, and 3-speed fan control. Operates on standard 230V single-phase power. Coverage approximately 35-50 m² depending on heat load. Three units available — priced per unit, discount for bulk purchase." \
    "$CAT_CLIMATE_CONTROL" \
    1100 1500 "Ljubljana" "SI" "spc" \
    '{"Type":"Portable Industrial AC","Cooling Capacity":"5 kW (17,000 BTU)","Cold Air Outlets":"2x 150mm flexible","Exhaust":"1x 200mm","Fan Speeds":"3","Power":"230V single-phase, 2.1 kW","Coverage":"35-50 m2","Cabinet":"Steel on casters","Filter":"Washable","Units Available":"3 (priced per unit)","Condition":"Used — Good","Year":"2021"}' \
    "portable_air_conditioner_industrial_04.jpg" "portable_air_conditioner_industrial_05.jpg"

# =====================================================================
# LOT 30: Container House — Premium Conversion (pending approval)
# =====================================================================
create_lot_with_images \
    "Luksuzna kontejnerska hiša — Premium Container House" \
    "Stunning 2-story luxury container house built from 4x 40ft high-cube containers. Total living area approximately 120 m². Ground floor: open-plan living room with floor-to-ceiling glazing opening to wooden deck terrace, fully equipped kitchen with island, guest WC. Upper floor: master bedroom with en-suite bathroom, 2x additional bedrooms, family bathroom, and rooftop terrace with glass balustrade. Premium finishes throughout: engineered hardwood flooring, custom LED lighting, designer bathroom fixtures. Fully insulated to passive house standards. Underfloor heating with heat pump. Triple-glazed windows. External cladding: dark timber composite panels. All utilities connected (water, electricity, sewage, fiber internet preparation). Turnkey — move-in ready. Building permit documentation available for Slovenian sites. An architectural statement piece that proves container living can be luxurious." \
    "$CAT_MODULAR_STRUCTURES" \
    145000 180000 "Ljubljana" "SI" "spc" \
    '{"Type":"Luxury Container House","Base":"4x 40ft HC containers","Floors":"2 stories","Living Area":"~120 m2","Bedrooms":"3 (1 master en-suite)","Bathrooms":"2 + guest WC","Kitchen":"Full with island","Terraces":"Ground deck + rooftop","Insulation":"Passive house standard","Heating":"Underfloor + heat pump","Windows":"Triple-glazed","Cladding":"Dark timber composite","Condition":"New — Turnkey","Building Permit":"Documentation available"}' \
    "container_house_01.webp" "container_home_01.webp" "container_house_02.jpg"

echo ""
echo "============================================="
log_info "Created ${#LOT_IDS[@]} lots with images"
echo "============================================="
echo ""

# ---------------------------------------------------------------------------
# Step 4: Submit all lots for review
# ---------------------------------------------------------------------------
log_info "Submitting lots for review..."

for lot_id in "${LOT_IDS[@]}"; do
    api_call POST "/lots/$lot_id/submit" "$SELLER_TOKEN" > /dev/null 2>&1 || true
done
log_ok "All ${#LOT_IDS[@]} lots submitted for review"

# Give search indexing a moment
sleep 2

# ---------------------------------------------------------------------------
# Step 5: Approve lots (keep last 3 pending for admin demo)
# ---------------------------------------------------------------------------
TOTAL=${#LOT_IDS[@]}
PENDING_COUNT=3
APPROVE_LIMIT=$((TOTAL > PENDING_COUNT ? TOTAL - PENDING_COUNT : TOTAL))

log_info "Approving $APPROVE_LIMIT lots (keeping $PENDING_COUNT pending for admin demo)..."

APPROVED_COUNT=0
for i in $(seq 0 $((APPROVE_LIMIT - 1))); do
    lot_id="${LOT_IDS[$i]}"
    api_call POST "/lots/$lot_id/approve" "$ADMIN_TOKEN" > /dev/null 2>&1 || true
    APPROVED_COUNT=$((APPROVED_COUNT + 1))
done

log_ok "Approved $APPROVED_COUNT lots, $((TOTAL - APPROVED_COUNT)) pending for admin demo"

# ---------------------------------------------------------------------------
# Step 6: Create auctions for selected lots
# ---------------------------------------------------------------------------
log_info "Creating auctions..."

NOW_EPOCH=$(date +%s)
START_PAST=$((NOW_EPOCH - 7200))           # 2 hours ago
END_SOON=$((NOW_EPOCH + 180))              # 3 minutes (anti-sniping demo)
END_6H=$((NOW_EPOCH + 21600))              # 6 hours
END_24H=$((NOW_EPOCH + 86400))             # 24 hours
END_48H=$((NOW_EPOCH + 172800))            # 48 hours
END_72H=$((NOW_EPOCH + 259200))            # 72 hours
END_5D=$((NOW_EPOCH + 432000))             # 5 days

format_iso() {
    date -u -d "@$1" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || \
    date -u -r "$1" +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || \
    echo "2026-03-20T12:00:00Z"
}

START_TIME=$(format_iso $START_PAST)
END_TIME_SOON=$(format_iso $END_SOON)
END_TIME_6H=$(format_iso $END_6H)
END_TIME_24H=$(format_iso $END_24H)
END_TIME_48H=$(format_iso $END_48H)
END_TIME_72H=$(format_iso $END_72H)
END_TIME_5D=$(format_iso $END_5D)

declare -a AUCTION_IDS=()
declare -a AUCTION_LOT_INDICES=()

create_auction() {
    local lot_index="$1"
    local start_time="$2"
    local end_time="$3"
    local starting_bid="$4"

    if [ "$lot_index" -ge "${#LOT_IDS[@]}" ]; then
        log_warn "  Lot index $lot_index out of range, skipping auction"
        return
    fi

    local lot_id="${LOT_IDS[$lot_index]}"
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
        AUCTION_LOT_INDICES+=("$lot_index")
        log_ok "  Auction: $auction_id (lot #$((lot_index+1)): ${LOT_TITLES[$lot_index]:-?})"
    else
        log_warn "  Auction creation may have failed for lot #$((lot_index+1))"
    fi
}

# Create auctions for diverse lot mix:
# Lot 0:  Office Container SPC K6          — 24h, active
# Lot 1:  Office Container Premium         — 48h, active
# Lot 6:  Shipping Container 20ft HC       — 48h, active
# Lot 7:  Shipping Container Converted     — 72h, active
# Lot 8:  Shipping Container 40ft HC       — 5 days, active
# Lot 9:  Sanitary SPC SAN C01             — ending soon (anti-sniping demo!)
# Lot 10: Twin Toilet Container            — 24h, active
# Lot 11: 4-Stall Toilet Block             — 48h, active
# Lot 13: Industrial Dehumidifier 960L     — 24h, active
# Lot 16: Diesel Heater SIP Fireball       — 6h, active
# Lot 17: Allmand Maxi-Heat                — 72h, active
# Lot 18: HPL Herman Nelson                — 5 days, active
# Lot 21: Temporary Fence                  — 48h, active
# Lot 22: 2-Story Modular Office           — 5 days, active
# Lot 25: CAT 302 Mini Excavator           — 72h, active
# Lot 26: CAT 303.5 Compact Excavator      — 5 days, active

create_auction 0  "$START_TIME" "$END_TIME_24H"  4500    # Office Container K6
create_auction 1  "$START_TIME" "$END_TIME_48H"  6200    # Office Premium
create_auction 6  "$START_TIME" "$END_TIME_48H"  2800    # Shipping 20ft HC
create_auction 7  "$START_TIME" "$END_TIME_72H"  4200    # Shipping Converted
create_auction 8  "$START_TIME" "$END_TIME_5D"   3500    # Shipping 40ft HC
create_auction 9  "$START_TIME" "$END_TIME_SOON" 7200    # Sanitary SAN C01 (ending soon!)
create_auction 10 "$START_TIME" "$END_TIME_24H"  2800    # Twin Toilet
create_auction 11 "$START_TIME" "$END_TIME_48H"  8500    # 4-Stall Toilet
create_auction 13 "$START_TIME" "$END_TIME_24H"  4200    # Dehumidifier 960L
create_auction 16 "$START_TIME" "$END_TIME_6H"   650     # SIP Fireball
create_auction 17 "$START_TIME" "$END_TIME_72H"  8500    # Allmand Maxi-Heat
create_auction 18 "$START_TIME" "$END_TIME_5D"   12000   # HPL Herman Nelson
create_auction 21 "$START_TIME" "$END_TIME_48H"  1200    # Temp Fence
create_auction 22 "$START_TIME" "$END_TIME_5D"   85000   # 2-Story Office
create_auction 25 "$START_TIME" "$END_TIME_72H"  22000   # CAT 302
create_auction 26 "$START_TIME" "$END_TIME_5D"   32000   # CAT 303.5

echo ""
log_info "Created ${#AUCTION_IDS[@]} auctions"

# ---------------------------------------------------------------------------
# Step 7: Place bids to create realistic bid history
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
        log_ok "  Bid EUR $amount ($label)"
    else
        log_warn "  Bid may have failed: EUR $amount ($label)"
    fi
}

# Bid on Office Container K6 (auction 0): competitive bidding
if [ ${#AUCTION_IDS[@]} -ge 1 ]; then
    place_bid "${AUCTION_IDS[0]}" 4600 "$BUYER_TOKEN"  "Office K6 — Buyer 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[0]}" 4700 "$BUYER2_TOKEN" "Office K6 — Buyer 2"
    sleep 0.3
    place_bid "${AUCTION_IDS[0]}" 4900 "$BUYER_TOKEN"  "Office K6 — Buyer 1 counter"
    sleep 0.3
    place_bid "${AUCTION_IDS[0]}" 5100 "$BUYER2_TOKEN" "Office K6 — Buyer 2 counter"
fi

# Bid on Shipping Container 20ft (auction 2)
if [ ${#AUCTION_IDS[@]} -ge 3 ]; then
    place_bid "${AUCTION_IDS[2]}" 2900 "$BUYER_TOKEN"  "Shipping 20ft — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[2]}" 3000 "$BUYER2_TOKEN" "Shipping 20ft — Bid 2"
fi

# Bid on Sanitary Container — ending soon (auction 5)
if [ ${#AUCTION_IDS[@]} -ge 6 ]; then
    place_bid "${AUCTION_IDS[5]}" 7300 "$BUYER_TOKEN"  "Sanitary SAN C01 — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[5]}" 7500 "$BUYER2_TOKEN" "Sanitary SAN C01 — Bid 2"
    sleep 0.3
    place_bid "${AUCTION_IDS[5]}" 7700 "$BUYER_TOKEN"  "Sanitary SAN C01 — Bid 3"
fi

# Bid on Industrial Dehumidifier 960L (auction 8)
if [ ${#AUCTION_IDS[@]} -ge 9 ]; then
    place_bid "${AUCTION_IDS[8]}" 4300 "$BUYER_TOKEN"  "Dehumidifier 960L — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[8]}" 4500 "$BUYER2_TOKEN" "Dehumidifier 960L — Bid 2"
fi

# Bid on Temp Fence (auction 12)
if [ ${#AUCTION_IDS[@]} -ge 13 ]; then
    place_bid "${AUCTION_IDS[12]}" 1300 "$BUYER_TOKEN" "Fence Set — Bid 1"
fi

# Bid on CAT 302 (auction 14)
if [ ${#AUCTION_IDS[@]} -ge 15 ]; then
    place_bid "${AUCTION_IDS[14]}" 22500 "$BUYER_TOKEN"  "CAT 302 — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[14]}" 23000 "$BUYER2_TOKEN" "CAT 302 — Bid 2"
    sleep 0.3
    place_bid "${AUCTION_IDS[14]}" 23500 "$BUYER_TOKEN"  "CAT 302 — Bid 3"
fi

# Bid on 2-Story Modular Office (auction 13)
if [ ${#AUCTION_IDS[@]} -ge 14 ]; then
    place_bid "${AUCTION_IDS[13]}" 87000 "$BUYER2_TOKEN" "2-Story Office — Bid 1"
fi

# ---------------------------------------------------------------------------
# Step 8: Feature some auctions for homepage carousel
# ---------------------------------------------------------------------------
log_info "Featuring auctions for homepage..."

# Feature: Office K6, Shipping 20ft, Sanitary SAN C01, CAT 302, 2-Story Office
for idx in 0 2 5 14 13; do
    if [ "$idx" -lt "${#AUCTION_IDS[@]}" ]; then
        api_call POST "/auctions/${AUCTION_IDS[$idx]}/feature" "$ADMIN_TOKEN" > /dev/null 2>&1 || true
    fi
done
log_ok "Featured 5 auctions for homepage carousel"

# ---------------------------------------------------------------------------
# Step 9: Add watchlist items for buyer
# ---------------------------------------------------------------------------
log_info "Adding watchlist items for buyer..."

# Watchlist: diverse mix of lot types
for idx in 0 6 9 13 22 25 29; do
    if [ "$idx" -lt "${#LOT_IDS[@]}" ]; then
        api_call POST "/users/me/watchlist/${LOT_IDS[$idx]}" "$BUYER_TOKEN" > /dev/null 2>&1 || true
    fi
done
log_ok "Added items to buyer watchlist"

# ---------------------------------------------------------------------------
# Done!
# ---------------------------------------------------------------------------
echo ""
echo "============================================="
echo -e "${GREEN}  SPC demo data seeded successfully!${NC}"
echo "============================================="
echo ""
echo "Summary:"
echo "  - ${#LOT_IDS[@]} lots created with product images"
echo "  - $APPROVED_COUNT approved, $((TOTAL - APPROVED_COUNT)) pending admin review"
echo "  - ${#AUCTION_IDS[@]} active auctions (1 ending soon for anti-sniping demo)"
echo "  - Bids placed across multiple auctions"
echo "  - 5 featured auctions for homepage carousel"
echo "  - Buyer watchlist populated"
echo ""
echo "Categories seeded:"
echo "  - Office Containers & Portable Cabins (6 lots)"
echo "  - Shipping Containers (3 lots)"
echo "  - Sanitary & Toilet Containers (4 lots)"
echo "  - Industrial Dehumidifiers (3 lots)"
echo "  - Diesel Heaters & Space Heaters (5 lots)"
echo "  - Temporary Construction Fencing (1 lot)"
echo "  - Modular Office Buildings (3 lots)"
echo "  - Mini & Compact Excavators (2 lots)"
echo "  - Portable Air Conditioners (2 lots)"
echo "  - Container House (1 lot)"
echo ""
echo "Demo accounts:"
echo "  Buyer 1: buyer@test.com   / password123"
echo "  Buyer 2: buyer2@test.com  / password123"
echo "  Seller:  seller@test.com  / password123"
echo "  Admin:   admin@test.com   / password123"
echo ""
echo "Frontends:"
echo "  Buyer:  http://localhost:3000"
echo "  Seller: http://localhost:5174"
echo "  Admin:  http://localhost:5175"
echo ""
