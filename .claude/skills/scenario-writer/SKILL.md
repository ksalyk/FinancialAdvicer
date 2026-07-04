---
name: scenario-writer
description: >
  Use when writing, editing, extending, reviewing, or validating game content in
  Financial Life V2 — a ScenarioGraph, GameEvent, GameOption, Effect, Condition,
  story arc, conditional event, scam event, ending, or event pool. Triggers on
  "write a scenario", "add a character", "new ScenarioGraph", "add an event/choice",
  "balance the economy", "validate scenarios", or any change under
  shared/.../scenarios/. Enforces the engine's real data contract and runs a
  static lint + a deterministic simulation harness so AI-authored scenarios ship
  without dangling references, dead-ends, unreachable endings, or stat blowups.
license: MIT
metadata:
  author: scenario-writer skill for FinancialLifeV2
  version: "2.0.0"
---

# Scenario Writer (Financial Life V2)

Authoring + verification workflow for the `:shared` game content. The goal is
**solid scenarios**: content an AI can write or edit that compiles, never traps
the player, and always resolves to a real ending — proven by simulation, not by
eyeballing.

**Ground truth hierarchy** (when documents disagree, higher wins):

1. The code: `scenarios/NarrativeDsl.kt`, `scenarios/Scenarios.kt`,
   `engine/GameEngine.kt`, `model/Models.kt`, `scenarios/EventPoolSelector.kt`.
2. The reference graph: **`scenarios/DaniyarScenarioGraph.kt`** — the canonical
   authored character. Copy its structure, not examples from older docs.
3. This skill (`references/data-model.md`, `references/validation-rules.md`).
4. `.claude/CHARACTER_STORY_BIBLE.md` — the *creative* layer (who the characters
   are, their lesson, voice, arc). It does not define mechanics.
   `.claude/SCENARIO_REFERENCE.md` — real scam/mistake research to draw from.

## When to use

- Creating a new `XxxScenarioGraph` (new character + era).
- Adding/editing events, options, conditionals, scam events, endings, or the pool.
- Re-balancing the economy (capital, income, weights, deltas).
- Reviewing AI-generated scenario content before it merges.

## Mental model (read once — three facts differ from what you'd assume)

The engine (`engine/GameEngine.kt`) is a 3-layer FSM. After any option whose
`next == MONTHLY_TICK`, it runs the economic tick, then selects the next event
through a **4-tier priority queue**:

```
1. Era global event   — EraDefinition.globalEvents … ⚠ EMPTY for every era (see below)
2. Deferred scheduled — queued earlier via Effect.scheduleEvent
3. Conditional event  — graph.conditionalEvents, highest priority that matches
4. Weighted pool      — EventPoolSelector over graph.eventPool
   (fallback: if nothing matches, the engine navigates to "normal_life")
```

Facts you must respect (all verified against the current engine):

- **Tier 1 never fires today.** Every `EraRegistry` era has empty `globalEvents`
  and empty `poolWeightModifiers`, and `Condition` has **no YEAR field** (only
  `MONTH`). Therefore historical calendar beats (МММ '94, devaluation '99, …)
  are authored as a **scheduled backbone**: a scripted chain where each beat
  queues the next via `Effect.scheduleEvent = ScheduledEvent(id, afterMonths = n)`.
  This is how `DaniyarScenarioGraph` spans 1994–1996.
- **Fast-forward depends on the backbone.** The engine skips uneventful months
  *only while a future scheduled event (or dated era global) exists*. If your
  backbone runs dry before an ending is reachable, the player grinds one filler
  month per click. Keep a scheduled beat pending until the endgame.
- **`"normal_life"` MUST exist** in `events`, non-terminal, with options — it is
  the engine's hard-coded fallback (`ScenarioGraphContentTest` enforces this).
- **Pool repeat semantics** (`EventPoolSelector`):
  `unique = true` or `cooldownMonths == 0` → single use;
  `cooldownMonths > 0` → repeatable on cooldown;
  `maxOccurrences = n` → hard per-game cap (use ~3 for filler so it retires).
- **Conditions are AND-ed.** Every `Condition` in a list must pass.
- **`findEvent` looks in this graph only** (`events` then `conditionalEvents`).
  There are no shared event libraries — every id you reference must be defined
  in the same graph.

Full data-structure reference: **`references/data-model.md`**.
Every rule the validator enforces, with rationale: **`references/validation-rules.md`**.

## Authoring workflow

Follow these steps in order. Do not skip the validation gate.

### 1. Define the character & era
Read the character's chapter in `.claude/CHARACTER_STORY_BIBLE.md` first. Pick
the `eraId` (`kz_90s`, `kz_2005`, `kz_2015`, `kz_2024` — lowercase). Set
`characterId = "{name}_{era_short}"` (e.g. `daniyar_90s`).

### 2. Design the initial `PlayerState`
Defaults for a stable-middle-class arc; hardship arcs (like Daniyar: capital
3 000 ₸ ≈ half a month of expenses, net flow +500 ₸) may go far tighter — the
simulation harness is the referee, not these ranges.

- `capital` ≈ 3–6× monthly `expenses` (uncomfortable, not desperate).
- `income - expenses - debtPaymentMonthly` slightly **positive** (slow tension).
- `financialKnowledge` low (5–25) — the game teaches it through consequences.
- `stress` 40–70 for a crisis arc, 30–50 for stable middle class.
- `investmentReturnRate` is **annual** (`0.08` = 8%/yr; engine divides by 12).
- `currency = CurrencyCode.RUB` only if the story starts before Nov 1993 in
  `kz_90s`; otherwise omit (defaults to `KZT`). Use `Effect.monetaryReform` for
  the RUB→KZT switch if you start pre-reform.

### 3. Plan the arc, then write story events
Structure the graph as named arcs flattened into the `events` map — this is the
`DaniyarScenarioGraph` shape:

```kotlin
class ZarinaScenarioGraph : ScenarioGraph() {          // class, NOT object
    override val initialPlayerState = PlayerState(/* … */)
    override val events = listOf(
        openingArc(), midgameArc(), endgameArc(), regularLifeArc(),
    ).buildEvents()                                    // fails loudly on duplicate ids
    override val conditionalEvents = listOf(
        conditionalsArc(), endingsArc(),
    ).flattenEvents()
    override val eventPool = listOf(
        PoolEntry("normal_life", 10), /* … */
    )
}
```

Write events with the DSL — never construct `GameEvent`/`GameOption` by hand:

```kotlin
private fun openingArc(): EventArc = arc(
    "opening",
    event(
        id = "intro",                       // engine starts every game at "intro"
        flavor = "🌅",
        unique = true,                      // story events are unique
        tags = setOf("family"),
        message = story("Paragraph one.", "Paragraph two."),
        options = listOf(
            option("save", "Отложить в резерв", "💰", MONTHLY_TICK,
                Effect(capitalDelta = 2_000L, knowledgeDelta = 2)),
            option("ask", "Спросить совета", "🧑", "brother_advice"),
        ),
    ),
)
```

`next` is either `MONTHLY_TICK` (advance time, let the queue pick) or a literal
event id (immediate branch — no month passes). A scripted opening chain of
direct-`next` events is safe and standard for Act I. From then on, drive the
historical backbone with `fx = Effect(scheduleEvent = ScheduledEvent("next_beat", afterMonths = 3))`.

### 4. Write conditional events
Safety conditionals every graph needs (thresholds scaled to the era's money —
Daniyar's numbers are for a 6 000 ₸/mo world):

| id | trigger | priority | notes |
|---|---|---|---|
| `debt_crisis` | `cond(DEBT, GT, …)` | 100 | `cooldownMonths = 3` |
| `burnout` | `cond(STRESS, GT, 75L)` | 90 | `cooldownMonths = 6` |

Bankruptcy is NOT a conditional-plus-trigger pair — it is a terminal conditional
ending (next section).

### 5. Endings — **CRITICAL, this is where AI gets it wrong**
Use the **`ending()` DSL helper** — `event()` deliberately has no
`isEnding`/`endingType` parameters, so the old `event(isEnding = true, …)`
pattern **does not compile**.

A terminal-conditional ending both *triggers* and *resolves* in one node: put it
in `conditionalEvents` (via `endingsArc()`), give it `conditions` + `priority`,
and the engine navigates to it after a tick — `options = emptyList()` is built in.

```kotlin
private fun endingsArc(): EventArc = arc(
    "endings",
    ending(
        id = "ending_bankruptcy", endingType = EndingType.BANKRUPTCY,
        flavor = "💔", priority = 200,
        conditions = listOf(cond(CAPITAL, LTE, 0L)),        // the safety net
        message = story("…"),
    ),
    ending(
        id = "ending_wealth", endingType = EndingType.WEALTH,
        flavor = "🤑", priority = 150,
        conditions = listOf(
            Condition.HasFlag("arc.final_check"),           // gate on the endgame beat
            cond(CAPITAL, GTE, 35_000L), cond(DEBT, LTE, 3_000L),
            Condition.HasFlag("business.open"),
        ),
        message = story("…"),
    ),
    // + ending_freedom, ending_stability, ending_paycheck …
)
```

Rules (test-enforced):
- Cover **all 5 `EndingType`s** (`ScenarioGraphContentTest` fails otherwise).
- Order by `priority`: bankruptcy (200) > wealth (150) > freedom > stability >
  paycheck-to-paycheck (lowest, near-always-true once the endgame flag is set) —
  so the *best achieved* outcome wins.
- Gate the four non-bankruptcy endings on an endgame flag (e.g.
  `arc.final_check`) set by the last backbone beat, so the game can't end before
  the story does.
- An ending may also be the direct `next` of an option (e.g. "give up" → an
  ending id). Both routes are valid; the terminal node itself never has options.

### 6. Write the pool + register the graph
- Define every pool event in `events` (there are **no shared scam/era
  libraries** — author scams locally, sourcing schemes from
  `.claude/SCENARIO_REFERENCE.md`), then list weighted refs in `eventPool`.
- `normal_life` should carry the top weight (10, per the Bible: routine
  dominates); scams stay rare (weight 2–3). ~12–20 pool entries is the shipped
  range (Daniyar has 15).
- Scam events: tags `"scam"` + `"scam.{type}"`, condition
  `Condition.NotFlag("learned.scam.{type}")`, and the refuse/learn option sets
  `"learned.scam.{type}"`. `EventPoolSelector` then crushes that subtype's
  weight (0.15×) once learned; knowledge > 50 halves all scam weights. Add a
  `schemeExplanation` for headline scams (post-choice teaching moment).
- Filler pool events that should repeat: give them `cooldownMonths` (repeatable)
  **and** `maxOccurrences ≈ 3` (so they retire before they get stale).
- Register in **`scenarios/Scenarios.kt` → `ScenarioGraphFactory.buildGraph()`**:
  add a `"{characterId}" -> XxxScenarioGraph()` branch (the public entry point
  `forCharacter()` caches per locale+character+era and falls back to `forEra()`
  era shells for unknown ids — don't touch those).
- Add the combo to **`ScenarioTestCatalog.authoredCombos`**
  (`shared/src/commonTest/.../scenarios/ScenarioTestCatalog.kt`) — NOT
  `emptyShellCombos`. This one line auto-enrolls the graph in every content +
  simulation test.

## Validation gate (do not declare done until all three pass)

**1. Static lint (fast, pre-compile):**
```bash
python3 .claude/skills/scenario-writer/scripts/validate_scenarios.py
```
Scans `scenarios/*ScenarioGraph.kt`. Catches missing `L` suffixes on money,
`scheduleEvent = Pair(...)`, uppercase `eraId`, `object XxxScenarioGraph`
(must be `class`), direct data-class construction, and `isEnding =` in scenario
files (use the `ending()` helper).

**2. Compile:**
```bash
./gradlew :shared:build -x test --no-daemon
```

**3. Simulation + content tests (the real proof):**
```bash
./gradlew :shared:test --no-daemon
```
This runs:
- `ScenarioSimulationTest` — drives the real engine across 40 deterministic RNG
  seeds per graph, up to 800 steps each. Fails on any crash, dead-end (an event
  with no options while the game isn't over), stat out of bounds, unresolved
  reference, or unreachable ending.
- `ScenarioGraphContentTest` — structural checks (intro exists, all `next`/
  scheduled refs resolve, endings terminal, all 5 ending types covered,
  playable `normal_life`).
- Regression suites (`RandomEventCooldownRegressionTest`,
  `FillerEventCapRegressionTest`, `EngineReliabilityRegressionTest`) and
  per-character balance tests (e.g. `DaniyarScenarioBalanceTest` — add one for
  a new character if its arc makes specific promises, e.g. "WEALTH reachable
  only through the business path").

If a graph fails, read the violation detail (it names the combo, seed, step,
event, and option) and fix the content — never weaken the test to make it pass.

## Style (unchanged, still enforced in review)

2nd person, era-specific concrete detail (prices, brands, street names), short
chat-like paragraphs via `story(...)`, options ≤ ~15 words starting with a verb,
teach-through-consequences — never lecture inside the scene. Russian prose for
character graphs is the shipped convention; the tri-lingual `Strings` layer
covers UI, not scenario prose (localizing graphs is a separate, future effort).
