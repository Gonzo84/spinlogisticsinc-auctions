#!/usr/bin/env bash
set -euo pipefail

echo "=== Backend dependency check ==="
./gradlew dependencyCheckAnalyze || echo "WARNING: Backend dependency check failed"

echo ""
echo "=== Frontend security audit ==="
for app in buyer-web seller-portal admin-dashboard; do
  echo "--- $app ---"
  cd frontend/$app
  npm audit --audit-level=high || echo "WARNING: $app has high severity vulnerabilities"
  cd ../..
done
