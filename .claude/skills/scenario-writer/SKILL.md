---
name: scenario-writer
description: >
  Use when writing, editing, extending, reviewing, or validating game content in
  Financial Life V2 — a ScenarioGraph, GameEvent, GameOption, Effect, Condition,
  story arc, conditional event, scam event, era event, or event pool. Triggers on
  "write a scenario", "add a character", "new ScenarioGraph", "add an event/choice",
  "balance the economy", "validate scenarios", or any change under
  shared/.../scenarios/. Enforces the engine's real data contract and runs a
  static lint + a deterministic simulation harness so AI-authored scenarios ship
  without dangling references, dead-ends, unreachable endings, or stat blowups.
license: MIT
metadata:
  author: scenario-writer skill for FinancialLifeV2
  version: "1.0.0"
---

# Scenario Writer (Financial Life V2)

Authoring + verification workflow for the `:shared` game content. The goal is
**solid scenarios**: content an AI can write or edit that compiles, never traps
the player, and always resolves to a real ending — proven by simulation, not by
eyeballing.

This skill is the **test-aligned source of truth**. Where it disagrees with
`.claude/SCENARIO_GRAPH_GUIDE.md`, this skill wins (that guide predates the
current engine contract — see "Corrections" below).

## When to use

- Creating a new `XxxScenarioGraph` (new character + era).
- Adding/editing events, options, conditionals, scam/era events, or the pool.
- Re-balancing the economy (capital, income, weights, deltas).
- Reviewing AI-generated scenario content before it merges.

## Mental model (read once)

The engine (`engine/GameEngine.kt`) is a 3-layer FSM. After any option whose
`next == MONTHLY_TICK`, it runs the economic tick, then selects the next event
through a **4-tier priority queue**:

```
1. Era global event   — fires on a real calendar date (EraDefinition.globalEvents)
2. Deferred scheduled — queued earlier via Effect.scheduleEvent
3. Conditional event  — graph.conditionalEvents, highest priority that matches
4. Weighted pool      — EventPoolSelector over graph.eventPool
   (fallback: if nothing matches, the engine navigates to "normal_life")
```

Three things follow from that and you must respect them:

- **`"normal_life"` MUST exist** in every graph. It is the engine's hard-coded
  fallback target (`GameEngine.makeChoice` → `?: "normal_life"`).
- **A pool event is single-use by default.** It only repeats if it declares
  `cooldownMonths > 0` (see `EventPoolSelector`). Don't expect a one-off pool
  event to come back.
- **Conditions are AND-ed.** Every `Condition` in a list must pass.

Full data-structure reference: **`references/data-model.md`**.
Every rule the validator enforces, with rationale: **`references/validation-rules.md`**.

## Authoring workflow

Follow these steps in order. Do not skip the validation gate.

### 1. Define the character & era
Name, age, year, starting class, the core financial conflict, and the lesson the
arc teaches. Pick the `eraId` (`kz_90s`, `kz_2005`, `kz_2015`, `kz_2024`). Set
`characterId = "{name}_{era_short}"`.

### 2. Design the initial `PlayerState`
- `capital` ≈ 3–6× monthly `expenses` (uncomfortable, not desperate).
- `income - expenses - debtPaymentMonthly` slightly **positive** (slow tension).
- `financialKnowledge` low (10–25) — the game teaches it through consequences.
- `stress` 40–70 for a crisis arc, 30–50 for stable middle class.
- `investmentReturnRate` is **annual** (`0.08` = 8%/yr; engine divides by 12).
- `currency = CurrencyCode.RUB` only if the story starts before Nov 1993 in
  `kz_90s`; otherwise omit (defaults to `KZT`).

### 3. Plan the arc, then write story events
Map chapters to months (intro → first crisis → opportunity → complication →
historical world event → moral test → `final_review`). Each event ≥ 2 options.
Use the DSL helpers — never construct `GameEvent`/`GameOption`/`Effect` data
classes by hand in scenario files:

```kotlin
event(
    id = "intro",
    message = story("Paragraph one.", "Paragraph two."),
    flavor = "🏦",
    unique = true,                          // all story events are unique
    options = listOf(
        option("save", "Отложить в резерв", "💰", next = MONTHLY_TICK,
            fx = Effect(capitalDelta = 20_000L, knowledgeDelta = 2)),
        option("ask", "Спросить совета у брата", "🧑", next = "brother_advice"),
    ),
)
```

`next` is either `MONTHLY_TICK` (advance time, let the queue pick) or a literal
event id (immediate branch). Use `Effect.scheduleEvent = ScheduledEvent(id, afterMonths = n)`
for realistic delayed consequences.

### 4. Write conditional events
Required safety/structure conditionals (priority high → low):

| id | trigger | priority | notes |
|---|---|---|---|
| `ending_bankruptcy_trigger` | `cond(CAPITAL, LTE, 0L)` | 120 | `unique`, routes to `ending_bankruptcy` |
| `debt_crisis` | `cond(DEBT, GT, …)` | 100 | `cooldownMonths = 3` |
| `burnout_{era}` | `cond(STRESS, GT, 75L)` | 60 | `cooldownMonths = 6` |

Scale thresholds to the era's money (see `StoryBalance` in `arcs/EraCharacterArcs.kt`).

### 5. Endings — **CRITICAL, this is where AI gets it wrong**
A terminal ending event has **`options = emptyList()`** and is reached *through a
separate trigger event*. It does **not** carry an "ok" closing option.

```kotlin
// Terminal ending — empty options, isEnding = true, endingType set:
event(id = "ending_freedom", isEnding = true, endingType = EndingType.FINANCIAL_FREEDOM,
      flavor = "🌅", message = story("Финансовая свобода…"), options = emptyList())

// Reached via a trigger event (a NORMAL event whose option points to it):
event(id = "ending_freedom_trigger", priority = 100, unique = true,
      conditions = listOf(Condition.HasFlag("arc.final_check"), cond(CAPITAL, GTE, 4_500_000L)),
      message = "…", options = listOf(option("claim", "Принять", "🎯", next = "ending_freedom")))
```

Cover **all 5** `EndingType`s with terminal events. `bankruptcy` is reachable via
the `ending_bankruptcy_trigger` conditional; the other four via `final_review` →
flags → `ending_*_trigger` conditionals. See `endingsArc()` + `storyConditionals()`
in `arcs/EraCharacterArcs.kt` for the canonical implementation.

### 6. Write the pool + register the graph
- Define every pool event in `events` (or reuse `ScamEventLibrary`/`EraEventLibrary`),
  then list weighted refs in `eventPool` as `PoolEntry(id, baseWeight)`.
- `normal_life` weight (10) must exceed any single scam event's weight.
- Scam events: tags `"scam"` + `"scam.{type}"`, add `Condition.NotFlag("learned.scam.{type}")`,
  and set `"learned.scam.{type}"` on the refuse option's `fx`.
- Register in **`scenarios/Scenarios.kt`** → `ScenarioGraphFactory.buildGraph()`
  (by `characterId`) and/or `forEra()` (by `eraId`). *(The old guide's
  `forCharacter()` name is wrong — the real private builders are `buildGraph`/`forEra`.)*
- Add the new `characterId to eraId` pair to **`ScenarioTestCatalog.combos`**
  (`shared/src/commonTest/.../scenarios/ScenarioTestCatalog.kt`). This one edit
  auto-enrolls the graph in every content + simulation test.

## Validation gate (do not declare done until all three pass)

**1. Static lint (fast, pre-compile):**
```bash
python3 .claude/skills/scenario-writer/scripts/validate_scenarios.py
```
Catches missing `L` suffixes on money, `scheduleEvent = Pair(...)`, uppercase
`eraId`, `object XxxScenarioGraph` (must be `class`), and direct data-class
construction in scenario files.

**2. Compile `:shared`:**
```bash
./gradlew :shared:compileKotlinCommon --no-daemon   # or :shared:build -x test
```

**3. Simulation + content tests (the real proof):**
```bash
./gradlew :shared:test --no-daemon
```
This runs:
- `ScenarioSimulationTest` — drives the engine across 40 deterministic RNG seeds
  per graph for up to 800 steps. Fails on any crash, dead-end (an event with no
  options while the game isn't over), or stat out of bounds (money < 0, or
  stress/knowledge/risk outside 0–100), and verifies every reference resolves and
  endings are reachable from `intro`.
- `ScenarioGraphContentTest` — structural checks (intro exists, all `next`/
  scheduled refs resolve, endings terminal, `normal_life` weight dominance).

If a new graph fails, read the violation detail (it names the combo, seed, step,
event, and option) and fix the content — never weaken the test to make it pass.

## Corrections to `.claude/SCENARIO_GRAPH_GUIDE.md`

That guide is a useful narrative/style reference but is **out of date** on these
points (all enforced by tests):

1. **Endings have `options = emptyList()`** — not a closing `option("ok", …)`.
   The guide's ending examples and its `bankruptcy_trigger` (`isEnding = true` +
   an option) would fail `ScenarioGraphContentTest."ending events are terminal"`.
2. The bankruptcy conditional is **`ending_bankruptcy_trigger`** (priority 120,
   routes to `ending_bankruptcy`), not `bankruptcy_trigger`.
3. Register graphs in **`buildGraph()` / `forEra()`** inside `Scenarios.kt`, not a
   public `forCharacter()` body.
4. Most production graphs compose events from **arcs** (`buildEvents()` over
   `EventArc`s) rather than one giant `buildMap` — prefer the arc pattern for new
   large graphs (see `AsanScenarioGraph`).

Use the writing-style section of the old guide (2nd person, era-specific detail,
teach-through-consequences tone) — that part is still correct.
