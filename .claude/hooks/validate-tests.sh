#!/bin/bash
# Test validation hook — runs unit tests for a module.
# Usage: ./validate-tests.sh <:module> [gradleTestTask]
#   Default task is "test" (works for :server and JVM modules).
#   For KMP modules pick a concrete target task to avoid running iOS tests off-mac:
#     ./validate-tests.sh :shared jvmTest
#     ./validate-tests.sh :composeApp testDebugUnitTest

set -euo pipefail

MODULE="$1"
TASK="${2:-test}"
PROJECT_ROOT="$(git rev-parse --show-toplevel)"

echo "TEST: $MODULE:$TASK"
cd "$PROJECT_ROOT"
./gradlew "$MODULE:$TASK" --quiet 2>&1 | tail -12
EXIT_CODE=${PIPESTATUS[0]}

if [ "$EXIT_CODE" -eq 0 ]; then echo "✓ TESTS PASS"; else echo "✗ TESTS FAIL"; exit 1; fi
