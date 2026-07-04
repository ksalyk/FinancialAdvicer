#!/usr/bin/env python3
"""
Fast, dependency-free static pre-check for Financial Life V2 scenario content.

This is the FIRST gate, not the last. It catches a few mechanical mistakes
before you spend a Gradle build. The authoritative checks are the Kotlin
simulation + content tests (`./gradlew :shared:test`).

Usage:
    python3 .claude/skills/scenario-writer/scripts/validate_scenarios.py [PATH ...]

With no args it scans authored character graphs (files matching
*ScenarioGraph.kt) directly under:
    shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/

Infra files (NarrativeDsl.kt, Scenarios.kt, EraDefinition.kt,
EventPoolSelector.kt) are intentionally excluded — they legitimately construct
GameEvent/GameOption directly.

Severities:
    ERROR  -> real bug (factory won't match / type mismatch). Exit code 1.
    WARN   -> codebase convention drift. Printed, does not fail the gate.
"""
import os
import re
import sys

# Long-typed fields that the codebase convention wants suffixed with `L`.
LONG_FIELDS = (
    "capitalDelta", "incomeDelta", "expensesDelta", "debtDelta",
    "debtPaymentDelta", "investmentsDelta",
    "capital", "income", "expenses", "debt", "debtPaymentMonthly", "investments",
)

RE_LONG_ASSIGN = re.compile(
    r"\b(" + "|".join(LONG_FIELDS) + r")\s*=\s*(-?\d[\d_]*)(L?)\b"
)
RE_COND = re.compile(r"\bcond\(\s*[A-Za-z_]+\s*,\s*[A-Za-z]+\s*,\s*(-?\d[\d_]*)(L?)\s*\)")
RE_SCHEDULE_PAIR = re.compile(r"scheduleEvent\s*=\s*(Pair\b|\")")
RE_OBJECT_GRAPH = re.compile(r"\bobject\s+\w*ScenarioGraph\b")
RE_ERA_ID = re.compile(r'\beraId\s*=\s*"([^"]*)"')
RE_DIRECT_CTOR = re.compile(r"\b(GameEvent|GameOption)\s*\(")
RE_ISENDING = re.compile(r"\bisEnding\s*=")

SCENARIOS_DIR = "shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios"

# Engine/DSL infrastructure — not authored content; excluded from the default scan.
INFRA_FILES = {
    "NarrativeDsl.kt",
    "Scenarios.kt",          # factory + EmptyEraScenarioGraph shells
    "EraDefinition.kt",
    "EventPoolSelector.kt",
}


def default_files():
    """Authored graphs: scenarios/*ScenarioGraph.kt minus infra files."""
    if not os.path.isdir(SCENARIOS_DIR):
        return []
    return sorted(
        os.path.join(SCENARIOS_DIR, n)
        for n in os.listdir(SCENARIOS_DIR)
        if n.endswith("ScenarioGraph.kt") and n not in INFRA_FILES
    )

errors = []
warns = []


def add(bucket, path, lineno, msg):
    bucket.append(f"{path}:{lineno}: {msg}")


def check_file(path):
    with open(path, "r", encoding="utf-8") as fh:
        lines = fh.readlines()

    for i, line in enumerate(lines, start=1):
        stripped = line.strip()
        if stripped.startswith("//") or stripped.startswith("*"):
            continue

        # ERROR: scheduleEvent must be a ScheduledEvent(...)
        if RE_SCHEDULE_PAIR.search(line):
            add(errors, path, i, "scheduleEvent must be ScheduledEvent(eventId, afterMonths), not a Pair/String")

        # ERROR: graph must be a class, not an object
        if RE_OBJECT_GRAPH.search(line):
            add(errors, path, i, "ScenarioGraph subtype must be a `class`, not an `object` (factory instantiates it)")

        # ERROR: eraId must be lowercase (matched case-sensitively by the factory)
        m = RE_ERA_ID.search(line)
        if m and m.group(1) != m.group(1).lower():
            add(errors, path, i, f'eraId "{m.group(1)}" must be lowercase to match EraRegistry')

        # WARN: Long fields without an L suffix (convention; bare literals still compile)
        for fm in RE_LONG_ASSIGN.finditer(line):
            if fm.group(3) != "L":
                add(warns, path, i, f"{fm.group(1)} = {fm.group(2)} is missing the Long `L` suffix")
        cm = RE_COND.search(line)
        if cm and cm.group(2) != "L":
            add(warns, path, i, f"cond(...) value {cm.group(1)} is missing the Long `L` suffix")

        # WARN: prefer the event()/option() DSL over direct data-class construction
        if RE_DIRECT_CTOR.search(line):
            add(warns, path, i, "prefer event()/option() DSL over direct GameEvent(...)/GameOption(...) construction")

        # ERROR: scenario files never set isEnding directly. event() has no such
        # parameter (it hardcodes isEnding = false), so this either doesn't compile
        # or is a hand-rolled GameEvent(...) bypassing the DSL. Use ending().
        if RE_ISENDING.search(line):
            add(errors, path, i,
                "isEnding in a scenario file — use the ending() DSL helper "
                "(event() has no isEnding param; endings get options = emptyList() built in)")


def collect(paths):
    files = []
    for p in paths:
        if os.path.isdir(p):
            for root, _, names in os.walk(p):
                files.extend(os.path.join(root, n) for n in names if n.endswith(".kt"))
        elif os.path.isfile(p) and p.endswith(".kt"):
            files.append(p)
    return files


def main():
    args = sys.argv[1:]
    files = collect(args) if args else default_files()
    if not files:
        where = ", ".join(args) if args else SCENARIOS_DIR
        print("ERROR: no .kt scenario files found under:", where)
        print("(run from the repository root, or pass explicit paths)")
        return 1
    for f in files:
        check_file(f)

    if errors:
        print("ERRORS:")
        for e in errors:
            print("  " + e)
    if warns:
        print("WARNINGS:")
        for w in warns:
            print("  " + w)
    if not errors and not warns:
        print(f"OK: scanned {len(files)} file(s), no issues.")
    else:
        print(f"\nScanned {len(files)} file(s): {len(errors)} error(s), {len(warns)} warning(s).")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
