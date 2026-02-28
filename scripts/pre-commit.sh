#!/usr/bin/env bash
set -euo pipefail

echo "=== Running pre-commit checks ==="

# Backend lint
echo "--- Detekt ---"
./gradlew detekt --daemon 2>/dev/null || {
  echo "ERROR: Detekt found issues. Fix them before committing."
  exit 1
}

# Frontend lint (only if frontend files changed)
CHANGED_FILES=$(git diff --cached --name-only)

for app in buyer-web seller-portal admin-dashboard; do
  if echo "$CHANGED_FILES" | grep -q "frontend/$app/"; then
    echo "--- ESLint: $app ---"
    cd frontend/$app
    npx eslint --max-warnings 0 src/ 2>/dev/null || {
      echo "ERROR: ESLint found issues in $app. Fix them before committing."
      cd ../..
      exit 1
    }
    cd ../..
  fi
done

echo "=== All pre-commit checks passed ==="
