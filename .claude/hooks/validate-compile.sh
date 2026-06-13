#!/bin/bash
# Post-edit validation hook — focused compilation of the edited file's module.
# Adapted to the FinancialLifeV2 layout (:shared :composeApp :server :landing :admin).
# Usage: ./validate-compile.sh <filepath>

set -euo pipefail

FILE="$1"
PROJECT_ROOT="$(git rev-parse --show-toplevel)"
REL_PATH="${FILE#$PROJECT_ROOT/}"

MODULE=""
TASK=""
case "$REL_PATH" in
  shared/src/*)     MODULE=":shared";     TASK="compileCommonMainKotlinMetadata" ;;
  composeApp/src/*) MODULE=":composeApp"; TASK="compileCommonMainKotlinMetadata" ;;
  server/src/*)     MODULE=":server";     TASK="compileKotlin" ;;
  landing/src/*)    MODULE=":landing";    TASK="compileKotlinWasmJs" ;;
  admin/src/*)      MODULE=":admin";      TASK="compileKotlinWasmJs" ;;
  *) echo "SKIP: no module detected for $REL_PATH"; exit 0 ;;
esac

echo "COMPILE: $MODULE:$TASK"
cd "$PROJECT_ROOT"
./gradlew "$MODULE:$TASK" --quiet 2>&1 | tail -8
EXIT_CODE=${PIPESTATUS[0]}

if [ "$EXIT_CODE" -eq 0 ]; then echo "✓ PASS"; else echo "✗ FAIL"; exit 1; fi
