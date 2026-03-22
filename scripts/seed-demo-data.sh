#!/usr/bin/env bash
# =============================================================================
# seed-demo-data.sh — Populate the platform with 30 US-market demo lots + images
#
# Creates lots with real product images, auctions, and bids tailored for the
# US industrial equipment market: containers, construction equipment, trucks,
# agriculture, material handling, and more.
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
# Category IDs (US-market taxonomy)
# ---------------------------------------------------------------------------
CAT_CONTAINERS_MODULAR="40000000-0000-0000-0000-000000000001"
CAT_CONSTRUCTION_EQUIPMENT="40000000-0000-0000-0000-000000000002"
CAT_CRANES_LIFTING="40000000-0000-0000-0000-000000000003"
CAT_AERIAL_PLATFORMS="40000000-0000-0000-0000-000000000004"
CAT_MATERIAL_HANDLING="40000000-0000-0000-0000-000000000005"
CAT_TRUCKS="40000000-0000-0000-0000-000000000006"
CAT_TRAILERS="40000000-0000-0000-0000-000000000007"
CAT_AGRICULTURE="40000000-0000-0000-0000-000000000008"
CAT_FORESTRY_LOGGING="40000000-0000-0000-0000-000000000009"
CAT_MINING_QUARRY="40000000-0000-0000-0000-000000000010"
CAT_OIL_GAS="40000000-0000-0000-0000-000000000011"
CAT_POWER_CLIMATE="40000000-0000-0000-0000-000000000012"
CAT_METALWORKING="40000000-0000-0000-0000-000000000013"
CAT_WOODWORKING_PLASTICS="40000000-0000-0000-0000-000000000014"
CAT_FOOD_PROCESSING="40000000-0000-0000-0000-000000000015"
CAT_MEDICAL_LAB="40000000-0000-0000-0000-000000000016"
CAT_VEHICLES_FLEET="40000000-0000-0000-0000-000000000017"
CAT_ATTACHMENTS_PARTS="40000000-0000-0000-0000-000000000018"

# =============================================================================
# MAIN
# =============================================================================
echo ""
echo "============================================="
echo "  Spin Logistics — US Market Demo Data Seeder (30 lots)"
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
# Step 3: Create 30 US-market demo lots with images
# ---------------------------------------------------------------------------
log_info "Creating 30 US-market demo lots with product images..."
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
    local brand="${8:-spin-logistics}"
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
# LOT 1: Office Container 20ft — Standard (ACTIVE auction, has bids)
# =====================================================================
create_lot_with_images \
    "20ft Standard Office Container — Insulated with HVAC Ready" \
    "Fully insulated 20ft standard office container with galvanized steel frame, powder-coated RAL 7016 (anthracite grey). Dimensions 20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2896 mm). 2-inch polyurethane wall insulation (U=0.37 W/m2K), 3-inch stone wool roof insulation. Includes aluminum entry door with cylinder lock, 2x PVC double-glazed tilt-and-turn windows (47 x 39 in / 1200 x 1000 mm), complete electrical system with distribution panel, 3-phase 30A connection, 6x 120V duplex outlets, 4x LED panel lights 40W, light switches. PVC vinyl flooring rated 50 psf. Stackable up to 3 units high. Ready for immediate delivery from Chicago warehouse." \
    "$CAT_CONTAINERS_MODULAR" \
    4500 5800 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"Spin Logistics","Model":"SL-K6","Dimensions":"20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2896 mm)","Wall Insulation":"2 in PUR (U=0.37)","Roof Insulation":"3 in Stone Wool","Floor Capacity":"50 psf (250 kg/m2)","Electrical":"3-phase 30A","Windows":"2x PVC double-glazed 47x39 in","Condition":"New","Stackable":"Up to 3 units","Year":"2025","Weight":"5,290 lbs (2,400 kg)"}' \
    "office_container_01.jpg" "office_container_02.jpg"

# =====================================================================
# LOT 2: Office Container — Premium with AC
# =====================================================================
create_lot_with_images \
    "20ft Premium Office Container with Split AC — Move-In Ready" \
    "Premium-grade 20ft office container with pre-installed split air conditioning (cooling + heating). Dimensions 20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2896 mm). Upgraded interior with laminate flooring, suspended ceiling with recessed LED lighting, and painted interior walls. Includes 9,000 BTU Mitsubishi split AC unit, electric convector heater 6,800 BTU backup, 4x double 120V duplex outlets, RJ45 network socket preparation. External finish anthracite grey. Delivered with forklift pockets and crane lifting points. Ideal for site managers, architects, or extended project offices." \
    "$CAT_CONTAINERS_MODULAR" \
    6200 7800 "Houston" "TX" "spin-logistics" \
    '{"Manufacturer":"Spin Logistics","Model":"SL-K6 Premium","Dimensions":"20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2896 mm)","Insulation":"2 in PUR walls + 3 in roof","Air Conditioning":"Mitsubishi 9,000 BTU split","Heating":"6,800 BTU electric convector","Flooring":"Laminate","Ceiling":"Suspended with recessed LED","Condition":"New","Year":"2025"}' \
    "office_container_03.jpg"

# =====================================================================
# LOT 3: Portable Cabin — Site Security Guard House
# =====================================================================
create_lot_with_images \
    "10ft Security Guard House — Checkpoint Booth" \
    "Compact security guard house suitable for construction site entry checkpoints, parking facilities, and industrial gate control. Dimensions 10 ft x 8 ft x 8.5 ft (3000 x 2438 x 2600 mm). Insulated sandwich panel walls 2 in, single entry door with deadbolt, 2x sliding windows with security bars, interior shelf/desk, LED ceiling light, single-phase electrical with 2x 120V outlets. Floor-mounted on adjustable leveling feet. Finished in grey-white. Low weight (1,870 lbs / 850 kg) for easy crane placement." \
    "$CAT_CONTAINERS_MODULAR" \
    2200 2800 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"Spin Logistics","Type":"Guard House","Dimensions":"10 ft x 8 ft x 8.5 ft (3000 x 2438 x 2600 mm)","Wall Insulation":"2 in sandwich panel","Door":"1x with deadbolt","Windows":"2x sliding with security bars","Electrical":"Single-phase, 2x 120V outlets","Weight":"1,870 lbs (850 kg)","Condition":"New"}' \
    "portable_cabin_02.jpg"

# =====================================================================
# LOT 4: Portable Cabin — Mobile Office on Wheels
# =====================================================================
create_lot_with_images \
    "20ft Mobile Office on Wheels — Towable Jobsite Trailer" \
    "Mobile office cabin on integrated chassis with rubber wheels for easy relocation. Dimensions 20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2800 mm) including chassis. No crane required — tow with standard vehicle or forklift. Insulated walls and roof, 2x windows, entry door with steps, full electrical installation. Interior includes desk area and storage shelf. Ideal for construction companies needing frequently relocated site offices. Used condition, recently refurbished interior, new vinyl flooring installed." \
    "$CAT_CONTAINERS_MODULAR" \
    3800 4800 "Los Angeles" "CA" "spin-logistics" \
    '{"Type":"Mobile Office on Wheels","Dimensions":"20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2800 mm)","Chassis":"Integrated with rubber wheels","Transport":"Towable, no crane needed","Windows":"2x PVC","Insulation":"Sandwich panel 2 in","Flooring":"New vinyl","Condition":"Used — Refurbished","Year":"2021"}' \
    "portable_cabin_01.jpeg"

# =====================================================================
# LOT 5: Portable Cabin — Compact 10ft Entry Level
# =====================================================================
create_lot_with_images \
    "10ft Compact Office Container — Flat-Pack Design" \
    "Compact 10ft portable office container, ideal for small construction sites, temporary ticket booths, or storage with workspace. Dimensions 10 ft x 8 ft x 8.5 ft (2991 x 2438 x 2591 mm). Single entry door, 1x PVC window. Basic electrical: ceiling LED light, 2x 120V outlets, external inlet. Flat-pack folding design allows efficient transport — 4 units stack on a single flatbed. Galvanized steel base frame. White exterior finish." \
    "$CAT_CONTAINERS_MODULAR" \
    1800 2400 "Miami" "FL" "spin-logistics" \
    '{"Type":"Compact Office Container","Dimensions":"10 ft x 8 ft x 8.5 ft (2991 x 2438 x 2591 mm)","Design":"Flat-pack folding","Transport":"4 units per flatbed","Door":"1x entry","Window":"1x PVC","Electrical":"LED + 2x 120V outlets","Weight":"2,645 lbs (1,200 kg)","Condition":"New"}' \
    "portable_cabin_04.jpg"

# =====================================================================
# LOT 6: Portable Cabin — Folding Expandable Container
# =====================================================================
create_lot_with_images \
    "20ft Folding Expandable Container Office — Hydraulic Deploy" \
    "Innovative folding container that expands from transport size to full 20ft office in under 10 minutes. Patented hydraulic folding mechanism — no crane required for setup. Transport dimensions 20 ft x 8 ft x 23 in (6058 x 2438 x 580 mm) collapsed. Expanded dimensions 20 ft x 8 ft x 8.5 ft (6058 x 2438 x 2600 mm). Sandwich panel walls with 2-inch EPS insulation, PVC vinyl floor, 2x windows, 1x entry door. Pre-wired electrical system activates automatically during unfolding. Revolutionary logistics savings — transport 6 units where normally only 1 fits." \
    "$CAT_CONTAINERS_MODULAR" \
    5500 6800 "Chicago" "IL" "spin-logistics" \
    '{"Type":"Folding Expandable Container","Collapsed":"20 ft x 8 ft x 23 in (6058 x 2438 x 580 mm)","Expanded":"20 ft x 8 ft x 8.5 ft (6058 x 2438 x 2600 mm)","Setup Time":"< 10 minutes","Crane Required":"No","Insulation":"2 in EPS sandwich","Transport Ratio":"6:1 vs standard","Condition":"New","Year":"2025"}' \
    "portable_cabin_03.jpg"

# =====================================================================
# LOT 7: Shipping Container 20ft HC — Used (ACTIVE auction, 48h)
# =====================================================================
create_lot_with_images \
    "20ft High Cube Shipping Container — CSC Certified, Wind & Watertight" \
    "Used 20ft High Cube ISO shipping container in good structural condition, CSC-certified. External dimensions 20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2896 mm). Internal volume 1,360 cu ft (38.5 m3). Double-wing cargo doors with original locking hardware. 1.1-inch bamboo-laminated marine plywood floor rated for forklift entry. Corner castings for standard container handling. Minor surface corrosion patched and treated with marine-grade primer. Wind and water tight certified (WWT). Ideal for on-site storage, workshop conversion, or continued shipping use. Located at Chicago intermodal yard — delivery within continental US available." \
    "$CAT_CONTAINERS_MODULAR" \
    2800 3500 "Chicago" "IL" "spin-logistics" \
    '{"Type":"20ft High Cube","ISO Code":"22G1","External":"20 ft x 8 ft x 9.5 ft (6058 x 2438 x 2896 mm)","Internal Volume":"1,360 cu ft (38.5 m3)","Doors":"Double-wing cargo","Floor":"1.1 in bamboo marine plywood","Max Payload":"62,170 lbs (28,200 kg)","Tare Weight":"5,070 lbs (2,300 kg)","Certification":"CSC + WWT","Condition":"Used — Good","Year":"2018"}' \
    "shipping_container_03.jpg" "shipping_container_01.jpg"

# =====================================================================
# LOT 8: Shipping Container 20ft — Converted with Windows
# =====================================================================
create_lot_with_images \
    "20ft Converted Container with Windows — Workshop Ready" \
    "20ft shipping container professionally converted with window cutouts and reinforced steel lintels. Features 3x double-glazed windows (31 x 24 in / 800 x 600 mm) with security shutters, personnel entry door with 3-point locking, and original cargo doors retained at rear. Painted graphite grey exterior. Basic electrical pre-wiring (conduit only, no fixtures). Suitable as workshop, hobby space, or base for further conversion. Structurally certified after modification." \
    "$CAT_CONTAINERS_MODULAR" \
    4200 5200 "Houston" "TX" "spin-logistics" \
    '{"Type":"20ft Standard — Converted","External":"20 ft x 8 ft x 8.5 ft (6058 x 2438 x 2591 mm)","Modifications":"3x windows, 1x personnel door, electrical conduit","Windows":"3x 31x24 in double-glazed with shutters","Door":"Personnel 3-point lock + original cargo doors","Finish":"Graphite Grey","Certification":"Post-modification structural cert","Condition":"Used — Modified","Year":"2019"}' \
    "shipping_container_02.jpg"

# =====================================================================
# LOT 9: Shipping Container 40ft HC — Standard
# =====================================================================
create_lot_with_images \
    "40ft High Cube Shipping Container — Cargo Worthy" \
    "Standard 40ft High Cube shipping container in cargo-worthy condition. External dimensions 40 ft x 8 ft x 9.5 ft (12192 x 2438 x 2896 mm), internal volume 2,694 cu ft (76.3 m3). Double-wing cargo doors with 4x cross-bar locking mechanism. Hardwood floor, 14 tie-down points. Recently de-registered from active shipping — clean interior, no chemical residue. Minor dents on side panels (cosmetic only, structurally sound). One-trip paint refresh available on request. Bulk pricing available for 5+ units." \
    "$CAT_CONTAINERS_MODULAR" \
    3500 4500 "Miami" "FL" "spin-logistics" \
    '{"Type":"40ft High Cube","ISO Code":"45G1","External":"40 ft x 8 ft x 9.5 ft (12192 x 2438 x 2896 mm)","Internal Volume":"2,694 cu ft (76.3 m3)","Doors":"Double-wing 4x cross-bar","Floor":"Hardwood","Max Payload":"58,598 lbs (26,580 kg)","Tare Weight":"8,598 lbs (3,900 kg)","Tie-downs":"14 points","Condition":"Cargo-Worthy","Year":"2020"}' \
    "shipping_container_03.jpg"

# =====================================================================
# LOT 10: Sanitary Container — Restroom Unit with Shower (ACTIVE auction, ending soon)
# =====================================================================
create_lot_with_images \
    "Portable Restroom Container with Shower — Jobsite Ready" \
    "Complete sanitary container manufactured for US jobsite use. Features 2x restroom stalls with porcelain toilets, 2x porcelain wash basins with mixer faucets, 1x urinal, and dedicated shower stall with thermostatic mixer. Dimensions 10 ft x 8 ft x 9 ft (3130 x 2400 x 2715 mm). 4 separate entry doors, 2x opaque windows for ventilation. Full plumbing: cold/hot water supply, drainage, 1.3 gal electric water heater. Full electrical: LED lighting, exhaust fan, GFCI-protected outlets. Hot-dip galvanized steel frame with 2-inch insulated sandwich panels. Forklift pockets and crane lifting eyes. Suitable for construction sites, outdoor events, sports facilities. ADA-compliant entry available." \
    "$CAT_CONTAINERS_MODULAR" \
    7200 8500 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"Spin Logistics","Model":"SL-SAN C01","Dimensions":"10 ft x 8 ft x 9 ft (3130 x 2400 x 2715 mm)","Restroom Stalls":"2x porcelain","Wash Basins":"2x with mixer faucets","Urinals":"1","Shower":"1x thermostatic","Water Heater":"1.3 gal electric","Doors":"4 separate entries","Ventilation":"2x windows + exhaust fan","Frame":"Hot-dip galvanized steel","Condition":"New","Year":"2025"}' \
    "sanitary_container_01.jpg" "sanitary_container_03.webp"

# =====================================================================
# LOT 11: Sanitary Container — Twin Restroom Unit
# =====================================================================
create_lot_with_images \
    "8ft Twin Restroom Container Unit — Dual Stall" \
    "Compact 8ft twin restroom container with 2 independent stalls. Each stall equipped with porcelain toilet, small wash basin, mirror, and coat hook. Shared water supply and waste connection at rear panel. Dimensions 8 ft x 7.2 ft x 8.5 ft (2438 x 2200 x 2591 mm). Steel construction powder-coated moss green. Ventilation louvers in each door. Floor-level entry (ADA accessible with ramp). Low water consumption dual-flush cisterns. Previously used on residential construction project — cleaned, sanitized, and inspected. Minor cosmetic wear on exterior." \
    "$CAT_CONTAINERS_MODULAR" \
    2800 3600 "Los Angeles" "CA" "spin-logistics" \
    '{"Type":"Twin Restroom Container","Dimensions":"8 ft x 7.2 ft x 8.5 ft (2438 x 2200 x 2591 mm)","Restroom Stalls":"2 independent","Toilets":"Porcelain dual-flush","Basins":"2x small","Finish":"Moss Green powder coat","Water Connection":"Rear panel","Accessibility":"Floor-level entry (ADA with ramp)","Condition":"Used — Good","Year":"2022"}' \
    "portable_toilet_03.jpg" "sanitary_container_02.jpg"

# =====================================================================
# LOT 12: Sanitary Container — 4-Stall Portable Restroom Block
# =====================================================================
create_lot_with_images \
    "20ft 4-Stall Portable Restroom Block — High Capacity" \
    "Large-capacity 4-stall portable restroom block for high-traffic construction sites or events. Each stall with porcelain toilet, individual ventilation, and privacy lock indicator. Central wash area with 2x basins and soap dispenser. Dimensions 20 ft x 8 ft x 8.5 ft (6058 x 2438 x 2600 mm). Blue/white steel frame construction. Includes 13 gal hot water boiler, LED lighting in all compartments, external maintenance access panel. Water inlet/outlet at ground level. Can serve 40-60 workers per shift. Brand new, manufactured Q1 2025." \
    "$CAT_CONTAINERS_MODULAR" \
    8500 10500 "Chicago" "IL" "spin-logistics" \
    '{"Type":"4-Stall Restroom Block","Dimensions":"20 ft x 8 ft x 8.5 ft (6058 x 2438 x 2600 mm)","Stalls":"4 independent","Wash Basins":"2x central","Hot Water":"13 gal boiler","Lighting":"LED all compartments","Capacity":"40-60 workers/shift","Water Connection":"Ground level","Condition":"New","Year":"2025"}' \
    "portable_toilet_01.jpg" "portable_toilet_02.jpg"

# =====================================================================
# LOT 13: Sanitary Container — Restroom + Shower Combo
# =====================================================================
create_lot_with_images \
    "8ft Restroom and Shower Combo Container — Compact Unit" \
    "Combined 8ft restroom and shower container. Left cabin: restroom with porcelain toilet, wash basin, and mirror. Right cabin: full shower with thermostatic mixer, drain, and curtain rail. Shared 4 gal electric water heater mounted internally. Dimensions 8 ft x 7.2 ft x 8.5 ft (2438 x 2200 x 2591 mm). Blue powder-coated steel exterior. Transparent glass entry doors for natural lighting. Anti-slip flooring in both compartments. Ideal for small construction teams (4-8 workers), temporary accommodation support, or camping facilities." \
    "$CAT_CONTAINERS_MODULAR" \
    3200 4200 "Chicago" "IL" "spin-logistics" \
    '{"Type":"Restroom + Shower Combo","Dimensions":"8 ft x 7.2 ft x 8.5 ft (2438 x 2200 x 2591 mm)","Restroom":"1x porcelain with basin","Shower":"1x thermostatic","Water Heater":"4 gal electric","Flooring":"Anti-slip","Entry":"Glass doors","Capacity":"4-8 workers","Condition":"Used — Excellent","Year":"2023"}' \
    "sanitary_container_04.jpg"

# =====================================================================
# LOT 14: Industrial Dehumidifier — Large Unit (ACTIVE auction, has bids)
# =====================================================================
create_lot_with_images \
    "Industrial Dehumidifier 253 GPD — High-Capacity Warehouse Unit" \
    "High-capacity industrial condensation dehumidifier rated 253 gallons per day (960 litres/day) at standard conditions (86F, 80% RH). Designed for large-area drying in warehouses, production halls, and post-flood restoration. Air flow 3,825 CFM via dual centrifugal fans. Operating temperature range 41-100F (5-38C). Automatic defrost function. Built-in digital hygrostat with programmable target humidity (30-90% RH). Continuous drainage via 1.25-inch outlet + optional condensate pump. Heavy-duty steel cabinet on 4x swivel casters with brakes. 3-phase 480V power supply, 4.2 kW consumption. Control panel with LED display, operating hours counter, and fault diagnostics." \
    "$CAT_POWER_CLIMATE" \
    4200 5500 "Chicago" "IL" "spin-logistics" \
    '{"Type":"Industrial Condensation Dehumidifier","Capacity":"253 GPD (960 L/24h)","Air Flow":"3,825 CFM (6,500 m3/h)","Operating Range":"41-100F (5-38C)","Power":"3-phase 480V, 4.2 kW","Humidity Control":"30-90% RH digital","Drainage":"1.25 in continuous + pump option","Weight":"320 lbs (145 kg)","Dimensions":"47 x 31 x 63 in (1200 x 800 x 1600 mm)","Condition":"Used — Excellent","Hours":"~2,000 h"}' \
    "industrial_dehumidifier_03.webp" "industrial_dehumidifier_01.jpg"

# =====================================================================
# LOT 15: Industrial Dehumidifier — Commercial 63L
# =====================================================================
create_lot_with_images \
    "Commercial Dehumidifier 16.6 GPD with Pump — Portable Unit" \
    "Commercial-grade portable dehumidifier with 16.6 gallons/day (63 litres/day) extraction capacity at 95F/90% RH. Built-in condensate pump with 20 ft vertical lift for flexible drainage routing. 1.45 gal collection tank with auto-shutoff as backup. Digital control panel with humidity display, 24h timer, and continuous mode. Ergonomic handle and 4x caster wheels for easy repositioning. Coverage area up to 2,150 sq ft (200 m2). Ideal for basements, warehouses, container storage, and construction drying. Energy-efficient compressor with R410a refrigerant. Orange/black industrial design." \
    "$CAT_POWER_CLIMATE" \
    450 650 "Houston" "TX" "spin-logistics" \
    '{"Type":"Commercial Dehumidifier","Capacity":"16.6 GPD (63 L/24h)","Air Flow":"265 CFM","Coverage":"2,150 sq ft (200 m2)","Tank":"1.45 gal with auto-shutoff","Pump":"Built-in, 20 ft lift","Refrigerant":"R410a","Power":"120V, 860W","Weight":"93 lbs (42 kg)","Condition":"Used — Good","Year":"2023"}' \
    "industrial_dehumidifier_04.webp"

# =====================================================================
# LOT 16: Industrial Dehumidifier — Heavy-Duty Floor Standing
# =====================================================================
create_lot_with_images \
    "Professional Floor-Standing Industrial Dehumidifier — Stainless Steel" \
    "Professional floor-standing industrial dehumidifier for demanding environments. Stainless steel cabinet construction resistant to chemical exposure and high-humidity corrosion. Dehumidification capacity rated for spaces up to 3,770 sq ft (350 m2). Features automatic humidistat, hot-gas defrost for low-temperature operation down to 34F (1C), and dual drainage ports. Three-speed fan with quiet night mode (48 dB). Front-mounted gauges for refrigerant pressure monitoring. Service-friendly design with removable side panels for compressor access. Mounted on reinforced rubber wheels for warehouse floor protection." \
    "$CAT_POWER_CLIMATE" \
    1800 2400 "New York" "NY" "spin-logistics" \
    '{"Type":"Professional Floor-Standing","Coverage":"3,770 sq ft (350 m2)","Min Operating Temp":"34F (1C)","Noise Level":"48 dB (quiet mode)","Cabinet":"Stainless steel","Defrost":"Hot-gas automatic","Drainage":"Dual ports","Fan Speeds":"3 + night mode","Power":"120V, 1,650W","Condition":"Used — Serviced","Weight":"150 lbs (68 kg)"}' \
    "industrial_dehumidifier_02.jpg" "industrial_dehumidifier_05.png"

# =====================================================================
# LOT 17: Diesel Heater — SIP Fireball Direct-Fired
# =====================================================================
create_lot_with_images \
    "SIP Fireball XD215 Direct-Fired Diesel Heater — 215,000 BTU" \
    "SIP Fireball XD215 direct-fired diesel/kerosene space heater, 215,000 BTU (63 kW) output. Massive air flow for rapid heating of large open spaces, workshops, and partially enclosed construction areas. Thermostat-controlled with adjustable flame. Safety features: tip-over switch, overheat protection, flame failure device. Stainless steel combustion chamber for long life. Large 14.8-gallon fuel tank provides 13+ hours continuous operation. Pneumatic wheels and lifting handle for one-person mobility. Note: direct-fired heaters produce combustion gases — use in ventilated spaces only." \
    "$CAT_POWER_CLIMATE" \
    650 850 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"SIP Industrial","Model":"Fireball XD215","Heat Output":"215,000 BTU (63 kW)","Fuel":"Diesel / Kerosene","Tank Capacity":"14.8 gal (56 L)","Burn Time":"13+ hours","Thermostat":"Yes, adjustable","Safety":"Tip-over + overheat + flame failure","Type":"Direct-fired","Ventilation":"Required","Condition":"Used — Good","Weight":"79 lbs (36 kg)"}' \
    "diesel_heater_02.jpg"

# =====================================================================
# LOT 18: Diesel Heater — Allmand Maxi-Heat 1M BTU Indirect
# =====================================================================
create_lot_with_images \
    "Allmand Maxi-Heat 1,000,000 BTU Indirect-Fired Mobile Heater" \
    "Allmand Maxi-Heat 1,000,000 BTU (293 kW) indirect-fired mobile diesel heater on trailer chassis. Heat exchanger design produces clean, fume-free hot air safe for enclosed occupied spaces. Dual 12-inch discharge outlets for flexible duct routing. Diesel-powered with 105-gallon integrated fuel tank — runs 48+ hours unattended. Electric start with 12V battery. Digital thermostat with remote sensor option. DOT-compliant road-towable trailer with highway lights. Ideal for large commercial construction projects, concrete curing, event tents, emergency heating. Recently serviced — new ignition module and fuel filters installed." \
    "$CAT_POWER_CLIMATE" \
    8500 11000 "Houston" "TX" "spin-logistics" \
    '{"Manufacturer":"Allmand","Model":"Maxi-Heat MH1000","Heat Output":"1,000,000 BTU (293 kW)","Type":"Indirect-fired (fume-free)","Fuel":"Diesel","Tank":"105 gal (400 L) integrated","Run Time":"48+ hours","Outlets":"2x 12-inch","Trailer":"DOT road-towable","Start":"Electric 12V","Condition":"Used — Serviced","Year":"2020","Hours":"~3,200 h"}' \
    "diesel_heater_03.jpg"

# =====================================================================
# LOT 19: Space Heater — HPL Herman Nelson Trailer-Mounted
# =====================================================================
create_lot_with_images \
    "HPL Herman Nelson 600D Trailer-Mounted Heater with LED Light Tower" \
    "HPL Herman Nelson 600 D7 all-in-one indirect-fired space heater with integrated LED light tower. 600,000 BTU (176 kW) clean heat output via heat exchanger — safe for occupied enclosed spaces. Trailer-mounted for easy deployment. Features: 79-gallon diesel tank, electric start, digital thermostat, 2x 10-inch duct outlets, integrated 4x LED floodlight tower (retractable mast). Dual function: heating + site illumination in one unit. Commonly used on US and Canadian construction sites for winter operations. Low hours, well-maintained unit from rental fleet." \
    "$CAT_POWER_CLIMATE" \
    12000 15000 "New York" "NY" "spin-logistics" \
    '{"Manufacturer":"HPL / Herman Nelson","Model":"600 D7","Heat Output":"600,000 BTU (176 kW)","Type":"Indirect-fired + LED tower","Fuel":"Diesel","Tank":"79 gal (300 L)","Outlets":"2x 10-inch","Light Tower":"4x LED floodlight, retractable mast","Trailer":"Integrated, towable","Condition":"Used — Fleet maintained","Year":"2021","Hours":"~2,500 h"}' \
    "space_heater_industrial_01.webp" "space_heater_industrial_02.webp"

# =====================================================================
# LOT 20: Space Heater — Thermobile Indirect ITA-75
# =====================================================================
create_lot_with_images \
    "Thermobile ITA-75 Indirect Space Heater — 256,000 BTU" \
    "Thermobile ITA-75 indirect-fired oil/diesel space heater. 256,000 BTU (75 kW) heat output through stainless steel heat exchanger — exhaust gases vented externally via chimney flue, making it safe for enclosed occupied spaces. High-volume radial fan delivers 2,060 CFM (3,500 m3/h) heated air. Built-in room thermostat and safety pressure switch. Dual-fuel capable (diesel or heating oil). Robust red powder-coated steel chassis on pneumatic wheels for site mobility. Easy-access service panels for burner maintenance. Widely used for construction, agriculture (polytunnels), and event/marquee heating." \
    "$CAT_POWER_CLIMATE" \
    1800 2400 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"Thermobile","Model":"ITA-75","Heat Output":"256,000 BTU (75 kW)","Type":"Indirect-fired","Air Flow":"2,060 CFM (3,500 m3/h)","Fuel":"Diesel / Heating Oil","Flue":"External chimney","Thermostat":"Built-in room","Safety":"Pressure switch","Weight":"209 lbs (95 kg)","Condition":"Used — Good","Year":"2020"}' \
    "space_heater_industrial_03.jpg" "space_heater_industrial_04.jpg"

# =====================================================================
# LOT 21: Space Heater — Wacker Neuson HI 400 Indirect
# =====================================================================
create_lot_with_images \
    "Wacker Neuson HI 400 Indirect Diesel Heater — 375,000 BTU" \
    "Wacker Neuson HI 400 HD indirect-fired diesel heater with 375,000 BTU (110 kW) heat output. Designed for demanding construction site heating — concrete curing, thawing frozen ground, heating temporary structures. Clean hot air output via heat exchanger with chimney exhaust. 12-inch discharge duct connection. 53-gallon integrated fuel tank for 20+ hours continuous operation. Electric start, built-in thermostat, flame sensor, and overheat protection. Heavy-duty frame on pneumatic wheels. Previously in rental fleet — regularly maintained per manufacturer schedule. Some cosmetic wear on exterior (dents, paint chips) but mechanically sound. Starts and runs perfectly." \
    "$CAT_POWER_CLIMATE" \
    3200 4200 "Los Angeles" "CA" "spin-logistics" \
    '{"Manufacturer":"Wacker Neuson","Model":"HI 400 HD","Heat Output":"375,000 BTU (110 kW)","Type":"Indirect-fired","Fuel":"Diesel","Tank":"53 gal (200 L)","Run Time":"20+ hours","Duct":"12 in (305 mm)","Start":"Electric","Safety":"Thermostat + flame sensor + overheat","Condition":"Used — Rental fleet maintained","Year":"2019","Hours":"~4,500 h"}' \
    "diesel_heater_04.jpg" "diesel_heater_01.jpg"

# =====================================================================
# LOT 22: CAT 320 Hydraulic Excavator — 20 ton
# =====================================================================
create_lot_with_images \
    "CAT 320 Hydraulic Excavator — 2021, Low Hours" \
    "Caterpillar 320 Next Gen hydraulic excavator, 2021 model with 3,800 operating hours. Operating weight 46,300 lbs (21,000 kg), Cat C4.4 ACERT Tier 4 Final diesel engine 162 hp (121 kW). Maximum digging depth 22 ft 4 in (6,810 mm), maximum reach 32 ft 5 in (9,880 mm). Steel tracks with triple grouser shoes. Enclosed ROPS/FOPS cab with AC, heated seat, 8-inch touchscreen monitor with grade assist. Equipped with: 42-inch GP bucket, hydraulic quick coupler, auxiliary hydraulics for hammer/shear. Full dealer service history. Excellent condition, ready to work." \
    "$CAT_CONSTRUCTION_EQUIPMENT" \
    95000 120000 "Houston" "TX" "spin-logistics" \
    '{"Manufacturer":"Caterpillar","Model":"320 Next Gen","Year":"2021","Operating Hours":"3,800 h","Operating Weight":"46,300 lbs (21,000 kg)","Engine":"Cat C4.4 ACERT Tier 4 Final, 162 hp","Max Dig Depth":"22 ft 4 in (6,810 mm)","Max Reach":"32 ft 5 in (9,880 mm)","Tracks":"Steel triple grouser","Cab":"ROPS/FOPS, AC, heated seat","Bucket":"42 in GP","Quick Coupler":"Hydraulic","Condition":"Excellent"}' \
    "cat_excavator_01.jpeg" "cat_excavator_02.jpeg"

# =====================================================================
# LOT 23: John Deere 6130R Tractor
# =====================================================================
create_lot_with_images \
    "John Deere 6130R Utility Tractor — 130 HP, MFWD" \
    "John Deere 6130R utility tractor, 2020 model with 2,200 hours. 130 hp (97 kW) PowerTech PVX 4.5L Tier 4 Final diesel engine. Mechanical front-wheel drive (MFWD), AutoPowr continuously variable transmission (CVT). PTO: 540/1000 RPM. 3-point hitch Cat II/IIIN, 7,720 lbs lift capacity. Premium cab with CommandCenter 4200 display, AC, suspension seat, integrated GPS-ready. Equipped with: front loader bracket (no bucket), 2x rear SCVs, 540/65R28 front and 650/65R38 rear tires (70% tread remaining). Clean unit from dairy operation, all scheduled maintenance performed at dealer. Located Dallas, TX." \
    "$CAT_AGRICULTURE" \
    65000 82000 "Dallas" "TX" "spin-logistics" \
    '{"Manufacturer":"John Deere","Model":"6130R","Year":"2020","Hours":"2,200 h","Engine":"PowerTech PVX 4.5L Tier 4 Final, 130 hp","Transmission":"AutoPowr CVT","Drive":"MFWD","PTO":"540/1000 RPM","3-Point Hitch":"Cat II/IIIN, 7,720 lbs lift","Cab":"Premium with CommandCenter 4200","Tires Front":"540/65R28 (70%)","Tires Rear":"650/65R38 (70%)","Condition":"Excellent"}' \
    "modular_office_building_01.jpg"

# =====================================================================
# LOT 24: Peterbilt 579 Day Cab Truck
# =====================================================================
create_lot_with_images \
    "2022 Peterbilt 579 Day Cab — PACCAR MX-13, 500 HP" \
    "2022 Peterbilt 579 day cab semi truck with PACCAR MX-13 engine rated at 500 hp and 1,850 lb-ft torque. Eaton Fuller 18-speed manual transmission. 12,000 lb front axle, tandem 40,000 lb rear axles. Air ride suspension with cab air ride. 295/75R22.5 tires (80% tread). Aluminum wheels, dual 100-gallon fuel tanks. Full air brakes with ABS. Power windows, power mirrors, AC, air seat. Odometer: 185,000 miles. Fleet-maintained with complete service records. DOT inspection current. Clean title, no accidents. Ideal for regional haul, construction, or heavy towing." \
    "$CAT_TRUCKS" \
    55000 70000 "Dallas" "TX" "spin-logistics" \
    '{"Manufacturer":"Peterbilt","Model":"579 Day Cab","Year":"2022","Mileage":"185,000 miles","Engine":"PACCAR MX-13, 500 hp, 1,850 lb-ft","Transmission":"Eaton Fuller 18-speed manual","Front Axle":"12,000 lbs","Rear Axles":"Tandem 40,000 lbs","Suspension":"Air ride","Tires":"295/75R22.5 (80%)","Fuel Tanks":"Dual 100 gal","Brakes":"Full air with ABS","Condition":"Good — Fleet maintained"}' \
    "portable_cabin_05.jpg"

# =====================================================================
# LOT 25: Modular Office — 2-Story Glass-Front Building
# =====================================================================
create_lot_with_images \
    "2-Story Modular Office Building — Glass Front, 1,205 sq ft" \
    "Impressive 2-story modular office building constructed from 8x 20ft container modules (4 per floor). Total usable office area approximately 1,205 sq ft (112 m2). Ground floor: open-plan office space with full glass front facade, reception area, 2x meeting rooms. Second floor: 4x individual offices with balcony terrace access. Features: aluminum-framed triple-glazed glass curtain wall, internal staircase, ADA access ramp, full HVAC system (heating + cooling), LED lighting throughout, Cat6 network cabling, fire alarm system. External finish: anthracite with anodized aluminum window frames. Currently assembled and operational — can be disassembled and relocated. Ideal for construction project HQ, temporary corporate offices, or school/clinic expansion." \
    "$CAT_CONTAINERS_MODULAR" \
    85000 110000 "Chicago" "IL" "spin-logistics" \
    '{"Type":"2-Story Modular Office","Modules":"8x 20ft (4 per floor)","Total Area":"~1,205 sq ft (112 m2)","Ground Floor":"Open-plan + reception + 2x meeting rooms","Second Floor":"4x offices + balcony terrace","Facade":"Triple-glazed aluminum curtain wall","HVAC":"Full heating + cooling","Network":"Cat6 cabled","Fire Safety":"Alarm system","Finish":"Anthracite + anodized aluminum","Condition":"Used — Operational","Year":"2022"}' \
    "modular_office_building_01.jpg"

# =====================================================================
# LOT 26: Mini Excavator — CAT 302 (1.5t)
# =====================================================================
create_lot_with_images \
    "CAT 302 CR Mini Excavator 1.5 Ton — Low Hours, Full Service History" \
    "Caterpillar 302 CR mini excavator, 2020 model with 2,100 operating hours. Operating weight 3,748 lbs (1,700 kg), rated engine power 19.2 hp (14.3 kW) Yanmar diesel. Maximum digging depth 7 ft 8 in (2,340 mm), maximum reach 13 ft 1 in (3,980 mm). Rubber tracks (new set installed at 1,800h), expandable undercarriage from 38.6 to 50.4 in (980 to 1,280 mm) for stability in confined spaces. ROPS/FOPS certified cab with sliding door. Equipped with: 12-inch digging bucket, quick coupler ready, dozer blade 42.5 in (1,080 mm). Full Caterpillar service history — all maintenance performed at authorized dealer on schedule. Excellent condition for age — no leaks, strong hydraulics, clean cab. Ideal for landscaping, utility trenching, indoor demolition, or tight-access foundation work." \
    "$CAT_CONSTRUCTION_EQUIPMENT" \
    22000 28000 "Houston" "TX" "spin-logistics" \
    '{"Manufacturer":"Caterpillar","Model":"302 CR","Year":"2020","Operating Hours":"2,100 h","Operating Weight":"3,748 lbs (1,700 kg)","Engine":"Yanmar 19.2 hp (14.3 kW) diesel","Max Dig Depth":"7 ft 8 in (2,340 mm)","Max Reach":"13 ft 1 in (3,980 mm)","Tracks":"Rubber (new at 1,800h)","Undercarriage":"Expandable 38.6-50.4 in","Cab":"ROPS/FOPS sliding door","Bucket":"12 in (300 mm)","Dozer Blade":"42.5 in (1,080 mm)","Quick Coupler":"Ready","Condition":"Excellent"}' \
    "mini_excavator_01.jpg" "cat_excavator_02.jpeg"

# =====================================================================
# LOT 27: Mini Excavator — CAT 303.5 (3.5t)
# =====================================================================
create_lot_with_images \
    "CAT 303.5E2 CR Compact Excavator 3.5 Ton — AC Cab, Thumb" \
    "Caterpillar 303.5E2 CR compact excavator, 2019 model with 3,400 operating hours. Operating weight 7,914 lbs (3,590 kg), Cat C1.7 diesel engine 24.7 hp (18.4 kW). Maximum digging depth 10 ft 7 in (3,230 mm). Fixed undercarriage with rubber tracks (60% remaining), cab with heat and air conditioning. Equipped with: 18-inch GP bucket, hydraulic thumb, pattern changer (ISO/SAE switchable), 2-way auxiliary hydraulics for attachments. Standard dozer blade. Recent service: new hydraulic oil and filters at 3,200h, swing bearing greased, all cylinders inspected — no drift. Some scratches on cab glass (visibility OK) and paint wear on boom. Mechanically very strong machine. Delivery available across continental US." \
    "$CAT_CONSTRUCTION_EQUIPMENT" \
    32000 40000 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"Caterpillar","Model":"303.5E2 CR","Year":"2019","Operating Hours":"3,400 h","Operating Weight":"7,914 lbs (3,590 kg)","Engine":"Cat C1.7, 24.7 hp (18.4 kW)","Max Dig Depth":"10 ft 7 in (3,230 mm)","Tracks":"Rubber (60% remaining)","Cab":"Heat + AC","Bucket":"18 in GP","Attachments":"Hydraulic thumb, pattern changer, 2-way aux","Dozer Blade":"Standard","Condition":"Good — Mechanically strong"}' \
    "cat_excavator_01.jpeg"

# =====================================================================
# LOT 28: Portable AC — Americool 23,000 BTU
# =====================================================================
create_lot_with_images \
    "Americool WPC-23000 Industrial Portable AC — 23,000 BTU" \
    "Americool WPC-23000 industrial portable air conditioner with 23,000 BTU (6.7 kW) cooling capacity. Three independent cooling outlets (top-mounted nozzles) for flexible airflow direction — cool multiple workstations or zones simultaneously. Compact footprint on heavy-duty casters. Self-contained system — no external condenser unit needed, just power and a vent path for hot air exhaust. Digital thermostat with auto-restart after power failure. Coverage area: 485-645 sq ft (45-60 m2). Suitable for server rooms, container offices, industrial workstations, temporary medical facilities. Uses R-410A refrigerant. Low operating hours, excellent condition." \
    "$CAT_POWER_CLIMATE" \
    2200 2900 "Chicago" "IL" "spin-logistics" \
    '{"Manufacturer":"Americool","Model":"WPC-23000","Cooling Capacity":"23,000 BTU (6.7 kW)","Outlets":"3x independent nozzles","Coverage":"485-645 sq ft (45-60 m2)","Type":"Self-contained portable","Refrigerant":"R-410A","Power":"120V, 2.8 kW","Thermostat":"Digital with auto-restart","Weight":"209 lbs (95 kg)","Condition":"Used — Excellent","Year":"2022"}' \
    "portable_air_conditioner_industrial_02.webp"

# =====================================================================
# LOT 29: Portable AC — Industrial Multi-Duct Unit
# =====================================================================
create_lot_with_images \
    "5 kW Portable Industrial Air Conditioner — Dual-Duct Spot Cooling" \
    "Heavy-duty portable industrial air conditioner with 17,000 BTU (5 kW) cooling capacity. Two-duct system: 2x flexible cold air outlets (6-inch diameter) for spot cooling of equipment or personnel. Rear-mounted hot air exhaust duct (8-inch). Steel cabinet construction on 4x swivel casters with brakes — designed for factory floor and workshop use. Features: washable air filter, condensate collection tray with float switch, adjustable thermostat, and 3-speed fan control. Operates on standard 120V single-phase power. Coverage approximately 375-540 sq ft (35-50 m2) depending on heat load. Three units available — priced per unit, discount for bulk purchase." \
    "$CAT_POWER_CLIMATE" \
    1100 1500 "Phoenix" "AZ" "spin-logistics" \
    '{"Type":"Portable Industrial AC","Cooling Capacity":"17,000 BTU (5 kW)","Cold Air Outlets":"2x 6 in flexible","Exhaust":"1x 8 in","Fan Speeds":"3","Power":"120V single-phase, 2.1 kW","Coverage":"375-540 sq ft (35-50 m2)","Cabinet":"Steel on casters","Filter":"Washable","Units Available":"3 (priced per unit)","Condition":"Used — Good","Year":"2021"}' \
    "portable_air_conditioner_industrial_04.jpg" "portable_air_conditioner_industrial_05.jpg"

# =====================================================================
# LOT 30: Container House — Premium Conversion (pending approval)
# =====================================================================
create_lot_with_images \
    "Luxury 2-Story Container Home — 1,290 sq ft, Turnkey" \
    "Stunning 2-story luxury container house built from 4x 40ft high-cube containers. Total living area approximately 1,290 sq ft (120 m2). Ground floor: open-plan living room with floor-to-ceiling glazing opening to wooden deck, fully equipped kitchen with island, guest half-bath. Upper floor: master bedroom with en-suite bathroom, 2x additional bedrooms, family bathroom, and rooftop terrace with glass balustrade. Premium finishes throughout: engineered hardwood flooring, custom LED lighting, designer bathroom fixtures. Fully insulated to passive house standards. Radiant floor heating with heat pump. Triple-glazed windows. External cladding: dark timber composite panels. All utilities connected (water, electricity, sewer, fiber internet preparation). Turnkey — move-in ready. Architectural plans and engineering stamps available. An architectural statement piece that proves container living can be luxurious." \
    "$CAT_CONTAINERS_MODULAR" \
    145000 180000 "Atlanta" "GA" "spin-logistics" \
    '{"Type":"Luxury Container House","Base":"4x 40ft HC containers","Floors":"2 stories","Living Area":"~1,290 sq ft (120 m2)","Bedrooms":"3 (1 master en-suite)","Bathrooms":"2 + guest half-bath","Kitchen":"Full with island","Terraces":"Ground deck + rooftop","Insulation":"Passive house standard","Heating":"Radiant floor + heat pump","Windows":"Triple-glazed","Cladding":"Dark timber composite","Condition":"New — Turnkey","Engineering":"Plans and stamps available"}' \
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
    "brand": "spin-logistics",
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
# Lot 0:  20ft Office Container             — 24h, active
# Lot 1:  Premium Office Container           — 48h, active
# Lot 6:  Shipping Container 20ft HC        — 48h, active
# Lot 7:  Shipping Container Converted      — 72h, active
# Lot 8:  Shipping Container 40ft HC        — 5 days, active
# Lot 9:  Restroom Container with Shower    — ending soon (anti-sniping demo!)
# Lot 10: Twin Restroom Container           — 24h, active
# Lot 11: 4-Stall Restroom Block            — 48h, active
# Lot 13: Industrial Dehumidifier 253 GPD   — 24h, active
# Lot 16: Diesel Heater SIP Fireball        — 6h, active
# Lot 17: Allmand Maxi-Heat                  — 72h, active
# Lot 18: HPL Herman Nelson                  — 5 days, active
# Lot 21: CAT 320 Excavator                 — 48h, active
# Lot 22: John Deere 6130R Tractor          — 5 days, active
# Lot 25: CAT 302 Mini Excavator            — 72h, active
# Lot 26: CAT 303.5 Compact Excavator       — 5 days, active

create_auction 0  "$START_TIME" "$END_TIME_24H"  4500    # Office Container
create_auction 1  "$START_TIME" "$END_TIME_48H"  6200    # Office Premium
create_auction 6  "$START_TIME" "$END_TIME_48H"  2800    # Shipping 20ft HC
create_auction 7  "$START_TIME" "$END_TIME_72H"  4200    # Shipping Converted
create_auction 8  "$START_TIME" "$END_TIME_5D"   3500    # Shipping 40ft HC
create_auction 9  "$START_TIME" "$END_TIME_SOON" 7200    # Restroom Container (ending soon!)
create_auction 10 "$START_TIME" "$END_TIME_24H"  2800    # Twin Restroom
create_auction 11 "$START_TIME" "$END_TIME_48H"  8500    # 4-Stall Restroom
create_auction 13 "$START_TIME" "$END_TIME_24H"  4200    # Dehumidifier 253 GPD
create_auction 16 "$START_TIME" "$END_TIME_6H"   650     # SIP Fireball
create_auction 17 "$START_TIME" "$END_TIME_72H"  8500    # Allmand Maxi-Heat
create_auction 18 "$START_TIME" "$END_TIME_5D"   12000   # HPL Herman Nelson
create_auction 21 "$START_TIME" "$END_TIME_48H"  95000   # CAT 320 Excavator
create_auction 22 "$START_TIME" "$END_TIME_5D"   65000   # John Deere 6130R
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
        log_ok "  Bid USD $amount ($label)"
    else
        log_warn "  Bid may have failed: USD $amount ($label)"
    fi
}

# Bid on Office Container (auction 0): competitive bidding
if [ ${#AUCTION_IDS[@]} -ge 1 ]; then
    place_bid "${AUCTION_IDS[0]}" 4600 "$BUYER_TOKEN"  "Office Container — Buyer 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[0]}" 4700 "$BUYER2_TOKEN" "Office Container — Buyer 2"
    sleep 0.3
    place_bid "${AUCTION_IDS[0]}" 4900 "$BUYER_TOKEN"  "Office Container — Buyer 1 counter"
    sleep 0.3
    place_bid "${AUCTION_IDS[0]}" 5100 "$BUYER2_TOKEN" "Office Container — Buyer 2 counter"
fi

# Bid on Shipping Container 20ft (auction 2)
if [ ${#AUCTION_IDS[@]} -ge 3 ]; then
    place_bid "${AUCTION_IDS[2]}" 2900 "$BUYER_TOKEN"  "Shipping 20ft — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[2]}" 3000 "$BUYER2_TOKEN" "Shipping 20ft — Bid 2"
fi

# Bid on Restroom Container — ending soon (auction 5)
if [ ${#AUCTION_IDS[@]} -ge 6 ]; then
    place_bid "${AUCTION_IDS[5]}" 7300 "$BUYER_TOKEN"  "Restroom Container — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[5]}" 7500 "$BUYER2_TOKEN" "Restroom Container — Bid 2"
    sleep 0.3
    place_bid "${AUCTION_IDS[5]}" 7700 "$BUYER_TOKEN"  "Restroom Container — Bid 3"
fi

# Bid on Industrial Dehumidifier 253 GPD (auction 8)
if [ ${#AUCTION_IDS[@]} -ge 9 ]; then
    place_bid "${AUCTION_IDS[8]}" 4300 "$BUYER_TOKEN"  "Dehumidifier 253 GPD — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[8]}" 4500 "$BUYER2_TOKEN" "Dehumidifier 253 GPD — Bid 2"
fi

# Bid on CAT 320 Excavator (auction 12)
if [ ${#AUCTION_IDS[@]} -ge 13 ]; then
    place_bid "${AUCTION_IDS[12]}" 96000 "$BUYER_TOKEN" "CAT 320 — Bid 1"
fi

# Bid on CAT 302 (auction 14)
if [ ${#AUCTION_IDS[@]} -ge 15 ]; then
    place_bid "${AUCTION_IDS[14]}" 22500 "$BUYER_TOKEN"  "CAT 302 — Bid 1"
    sleep 0.3
    place_bid "${AUCTION_IDS[14]}" 23000 "$BUYER2_TOKEN" "CAT 302 — Bid 2"
    sleep 0.3
    place_bid "${AUCTION_IDS[14]}" 23500 "$BUYER_TOKEN"  "CAT 302 — Bid 3"
fi

# Bid on John Deere 6130R (auction 13)
if [ ${#AUCTION_IDS[@]} -ge 14 ]; then
    place_bid "${AUCTION_IDS[13]}" 67000 "$BUYER2_TOKEN" "John Deere 6130R — Bid 1"
fi

# ---------------------------------------------------------------------------
# Step 8: Feature some auctions for homepage carousel
# ---------------------------------------------------------------------------
log_info "Featuring auctions for homepage..."

# Feature: Office Container, Shipping 20ft, Restroom Container, CAT 302, John Deere
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
echo -e "${GREEN}  Spin Logistics US market demo data seeded successfully!${NC}"
echo "============================================="
echo ""
echo "Summary:"
echo "  - ${#LOT_IDS[@]} lots created with product images"
echo "  - $APPROVED_COUNT approved, $((TOTAL - APPROVED_COUNT)) pending admin review"
echo "  - ${#AUCTION_IDS[@]} active auctions (1 ending soon for anti-sniping demo)"
echo "  - Bids placed across multiple auctions (USD)"
echo "  - 5 featured auctions for homepage carousel"
echo "  - Buyer watchlist populated"
echo ""
echo "Categories seeded:"
echo "  - Containers & Modular (office, shipping, restroom, expandable — 14 lots)"
echo "  - Power & Climate (dehumidifiers, heaters, AC — 9 lots)"
echo "  - Construction Equipment (CAT excavators — 3 lots)"
echo "  - Agriculture (John Deere tractor — 1 lot)"
echo "  - Trucks (Peterbilt 579 — 1 lot)"
echo "  - Modular Office Buildings (1 lot)"
echo "  - Container House (1 lot)"
echo ""
echo "US Locations:"
echo "  Chicago IL, Houston TX, Los Angeles CA, New York NY,"
echo "  Miami FL, Phoenix AZ, Dallas TX, Atlanta GA"
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
