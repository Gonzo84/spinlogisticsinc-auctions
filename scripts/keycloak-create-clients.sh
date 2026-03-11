#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# keycloak-create-clients.sh
#
# Creates missing bearer-only OIDC service clients in the auction-platform
# Keycloak realm via the Admin REST API.
#
# Why: KC_SPI_IMPORT_REALM_FILE_STRATEGY=OVERWRITE is broken — Keycloak
#      always uses IGNORE_EXISTING, so clients added to the realm JSON
#      after initial import are never created. This script fills the gap.
#
# Usage:  ./scripts/keycloak-create-clients.sh
# Env:    KEYCLOAK_URL  (default: http://localhost:8180)
#         KEYCLOAK_ADMIN (default: admin)
#         KEYCLOAK_ADMIN_PASSWORD (default: admin)
# ---------------------------------------------------------------------------
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
REALM="auction-platform"
ADMIN_USER="${KEYCLOAK_ADMIN:-admin}"
ADMIN_PASS="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

# Bearer-only service clients to ensure exist
declare -A CLIENTS=(
  ["analytics-service"]="Analytics Service"
  ["broker-service"]="Broker Service"
  ["catalog-service"]="Catalog Service"
  ["co2-service"]="CO2 Service"
  ["compliance-service"]="Compliance Service"
  ["media-service"]="Media Service"
  ["notification-service"]="Notification Service"
  ["search-service"]="Search Service"
  ["seller-service"]="Seller Service"
  ["user-service"]="User Service"
)

echo "=== Keycloak OIDC Client Provisioning ==="
echo "Keycloak URL: ${KEYCLOAK_URL}"
echo "Realm:        ${REALM}"
echo ""

# --- Step 1: Get admin access token ---
echo "Obtaining admin token..."
TOKEN_RESPONSE=$(curl -s -X POST \
  "${KEYCLOAK_URL}/realms/master/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "username=${ADMIN_USER}" \
  -d "password=${ADMIN_PASS}" \
  -d "grant_type=password" \
  -d "client_id=admin-cli")

ACCESS_TOKEN=$(echo "${TOKEN_RESPONSE}" | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])" 2>/dev/null || true)

if [ -z "${ACCESS_TOKEN}" ]; then
  echo "ERROR: Failed to obtain admin token. Is Keycloak running at ${KEYCLOAK_URL}?"
  echo "Response: ${TOKEN_RESPONSE}"
  exit 1
fi
echo "Admin token obtained."
echo ""

# --- Step 2: Get existing clients in realm ---
echo "Fetching existing clients..."
EXISTING_CLIENTS=$(curl -s -X GET \
  "${KEYCLOAK_URL}/admin/realms/${REALM}/clients?max=100" \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json")

# --- Step 3: Create each missing client ---
CREATED=0
SKIPPED=0

for CLIENT_ID in "${!CLIENTS[@]}"; do
  CLIENT_NAME="${CLIENTS[$CLIENT_ID]}"

  # Check if client already exists
  EXISTS=$(echo "${EXISTING_CLIENTS}" | python3 -c "
import sys, json
clients = json.load(sys.stdin)
print('yes' if any(c['clientId'] == '${CLIENT_ID}' for c in clients) else 'no')
" 2>/dev/null)

  if [ "${EXISTS}" = "yes" ]; then
    echo "  SKIP  ${CLIENT_ID} (already exists)"
    SKIPPED=$((SKIPPED + 1))
    continue
  fi

  # Create bearer-only client
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
    "${KEYCLOAK_URL}/admin/realms/${REALM}/clients" \
    -H "Authorization: Bearer ${ACCESS_TOKEN}" \
    -H "Content-Type: application/json" \
    -d "{
      \"clientId\": \"${CLIENT_ID}\",
      \"name\": \"${CLIENT_NAME}\",
      \"enabled\": true,
      \"clientAuthenticatorType\": \"client-secret\",
      \"secret\": \"${CLIENT_ID}-secret\",
      \"bearerOnly\": true,
      \"publicClient\": false,
      \"protocol\": \"openid-connect\",
      \"directAccessGrantsEnabled\": false,
      \"serviceAccountsEnabled\": false
    }")

  if [ "${HTTP_CODE}" = "201" ]; then
    echo "  CREATE ${CLIENT_ID} (${CLIENT_NAME})"
    CREATED=$((CREATED + 1))
  else
    echo "  ERROR  ${CLIENT_ID} — HTTP ${HTTP_CODE}"
  fi
done

echo ""
echo "=== Done: ${CREATED} created, ${SKIPPED} skipped ==="
