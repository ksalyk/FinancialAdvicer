#!/bin/bash
# Detekt validation hook.
# NOTE: detekt is not currently configured in FinancialLifeV2. This hook detects
# its absence and SKIPs (exit 0) instead of failing. To enable: add the detekt
# Gradle plugin, then this hook starts enforcing it automatically.
# Usage: ./validate-detekt.sh <filepath>

set -euo pipefail

FILE="$1"
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
cd "$PROJECT_ROOT"

# Fast static check for the plugin before paying for a Gradle configuration pass.
if ! grep -rqiE "detekt" build.gradle.kts ./*/build.gradle.kts gradle/libs.versions.toml 2>/dev/null; then
  echo "SKIP: detekt is not configured in this project"
  exit 0
fi

REL_PATH="${FILE#$PROJECT_ROOT/}"
MODULE=""
case "$REL_PATH" in
  shared/src/*)     MODULE=":shared" ;;
  composeApp/src/*) MODULE=":composeApp" ;;
  server/src/*)     MODULE=":server" ;;
  landing/src/*)    MODULE=":landing" ;;
  admin/src/*)      MODULE=":admin" ;;
  *) echo "SKIP: no module detected for $REL_PATH"; exit 0 ;;
esac

echo "DETEKT: $MODULE:detekt"
./gradlew "$MODULE:detekt" --quiet 2>&1 | tail -10
EXIT_CODE=${PIPESTATUS[0]}

if [ "$EXIT_CODE" -eq 0 ]; then echo "✓ DETEKT PASS"; else echo "✗ DETEKT FAIL"; exit 1; fi
