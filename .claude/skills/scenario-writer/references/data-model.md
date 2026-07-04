# Scenario Data Model — reference

Authoritative summary of the types in `:shared` (`model/Models.kt`,
`scenarios/NarrativeDsl.kt`, `scenarios/Scenarios.kt`). All snippets compile
against the current engine. Money fields are `Long` (use the `L` suffix); soft
stats are `Int` clamped to `0..100` by the engine.

## DSL helpers (use these in scenario files — defined in `scenarios/NarrativeDsl.kt`)

```kotlin
story(vararg paragraphs: String): String     // joins with blank lines

cond(field: Condition.Stat.Field, op: Condition.Stat.Op, value: Long): Condition.Stat

option(id: String, text: String, emoji: String, next: String, fx: Effect = Effect()): GameOption

event(                                       // NON-terminal events only
    id: String,
    message: String,
    options: List<GameOption>,               // 3rd positional — required
    flavor: String = "💬",
    priority: Int = 0,                       // conditional ordering (higher first)
    conditions: List<Condition> = emptyList(),
    tags: Set<String> = emptySet(),
    poolWeight: Int = 10,
    unique: Boolean = false,                 // pass true for every story beat
    cooldownMonths: Int = 0,
    maxOccurrences: Int = 0,                 // per-game pool cap; ~3 for filler
    schemeExplanation: String? = null,
): GameEvent
// ⚠ event() has NO isEnding/endingType parameters — it hardcodes isEnding = false.
//   Terminal nodes use ending():

ending(                                      // terminal node; options = emptyList() built in
    id: String,
    message: String,
    endingType: EndingType,
    flavor: String,
    priority: Int = 0,                       // vs other conditionals after a tick
    conditions: List<Condition> = emptyList(),  // makes it a terminal-conditional
): GameEvent

arc(name: String, vararg events: GameEvent): EventArc
List<EventArc>.buildEvents(): Map<String, GameEvent>   // for `events`; throws on duplicate ids
List<EventArc>.flattenEvents(): List<GameEvent>        // for `conditionalEvents`
```

Imports for terse conditions:
```kotlin
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*   // CAPITAL, INCOME, EXPENSES, DEBT, STRESS, KNOWLEDGE, RISK, MONTH
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*      // GT, LT, GTE, LTE, EQ, NEQ
```

⚠ There is **no YEAR field**. You cannot condition on the calendar year —
historical beats are driven by `Effect.scheduleEvent` chains (see SKILL.md
"scheduled backbone").

## Effect (delta-based; every field defaults to 0/empty)

```kotlin
Effect(
    capitalDelta = 0L, incomeDelta = 0L, expensesDelta = 0L,
    debtDelta = 0L,            // + adds debt, − repays
    debtPaymentDelta = 0L,     // change to monthly repayment
    investmentsDelta = 0L,
    stressDelta = 0, knowledgeDelta = 0, riskDelta = 0,   // Int, NO L suffix
    setFlags = setOf("flag.name"),
    clearFlags = setOf("flag.name"),
    scheduleEvent = ScheduledEvent(eventId = "id", afterMonths = 3),  // NOT a Pair
    monetaryReform = MonetaryReform(from = RUB, to = KZT, numerator = 1, denominator = 500),
                               // ONLY for real currency switches; no-ops if
                               // PlayerState.currency != from
)
```

The engine applies deltas then clamps: money `coerceAtLeast(0)`, soft stats
`coerceIn(0,100)`. You cannot drive a stat out of range via effects — but you
*can* leave the player permanently stuck if you forget to give a path forward.

## Condition (sealed; AND-ed within a list)

```kotlin
cond(CAPITAL, LT, 10_000L)              // Condition.Stat — value is always Long
cond(STRESS, GTE, 75L)
Condition.HasFlag("kiosk.opened")
Condition.NotFlag("learned.scam.forex")
Condition.InEra("kz_90s")               // normalizes "modern_kz_2024" → "kz_2024"
Condition.ForCharacter("daniyar_90s")
```

## GameEvent semantics

| field | meaning / rule |
|---|---|
| `id` | unique snake_case; referenced by `next`, `scheduleEvent`, `PoolEntry` |
| `message` | narrative; supports `{token}` substitution (below) |
| `options` | story/pool events ≥ 2 (1 acceptable for forced beats); **endings = `emptyList()` via `ending()`** |
| `conditions` | required for conditional & gated pool events; empty for fixed story events |
| `priority` | conditional-tier ordering; higher checked first |
| `tags` | scene image + scam logic keys — see mapping below |
| `poolWeight`/`PoolEntry.baseWeight` | relative draw weight (`PoolEntry` wins for pool draws) |
| `unique` | fires once per session; **true for every story beat** |
| `cooldownMonths` | pool event repeats ONLY if `> 0`; otherwise single-use |
| `maxOccurrences` | hard per-game pool cap; `0` = legacy (single-use unless cooldown) |
| `isEnding`+`endingType` | terminal node (set via `ending()`); engine sets `gameOver` on navigation |
| `schemeExplanation` | post-choice teaching card for scams; supports `{token}`s |

### Pool repeat semantics (`EventPoolSelector`)

| declaration | behaviour |
|---|---|
| default (`unique=false, cooldownMonths=0`) | single use per game |
| `unique = true` | single use per game (explicit) |
| `cooldownMonths = n` | repeatable, at most once per n months |
| `cooldownMonths = n, maxOccurrences = m` | repeatable on cooldown, retires after m draws |

Weight modifiers applied at draw time: learned `"learned.scam.{type}"` flag →
that subtype ×0.15; `financialKnowledge > 50` → all `"scam"`-tagged ×0.6;
`"lost_money_to_scam"` flag → all scam subtypes ×0.3.

### Tags → scene image (UI: `GameEngine.primarySceneTag`, priority order)
`mortgage` → `scam*` → `crisis` → `windfall` → `career` → `investment` →
`family` → `world`/`reflection`. Untagged/routine events render without a scene
image (intentional — don't tag filler).

### Message tokens (substituted live from PlayerState)
`{income} {expenses} {capital} {debt} {debtPayment} {investments} {passiveIncome}`
`{netFlow} {income3x} {knowledge} {stress} {name} {eraLabel}`

## PlayerState (initial snapshot)

```kotlin
PlayerState(
    capital = 3_000L, income = 6_000L, expenses = 5_500L,      // Daniyar's 1994 numbers
    debt = 0L, debtPaymentMonthly = 0L,
    investments = 0L, investmentReturnRate = 0.05,             // annual
    stress = 45, financialKnowledge = 5, riskLevel = 25,
    month = 3, year = 1994,                                    // pick month so scheduled
                                                               // beats land on real dates
    characterId = "daniyar_90s", eraId = "kz_90s",
    currency = CurrencyCode.KZT,          // RUB only pre-Nov-1993 starts
)
```

Derived (read-only): `netMonthlyFlow`, `monthlyInvestmentReturn`, `netWorth`,
`absoluteMonth`.

## ScenarioGraph (the unit you author)

```kotlin
class ZarinaScenarioGraph : ScenarioGraph() {          // class — NOT object
    override val initialPlayerState = PlayerState(/* … */)
    override val events: Map<String, GameEvent> =
        listOf(openingArc(), midgameArc(), regularLifeArc()).buildEvents()
    override val conditionalEvents: List<GameEvent> =
        listOf(conditionalsArc(), endingsArc()).flattenEvents()
    override val eventPool: List<PoolEntry> =
        listOf(PoolEntry("normal_life", 10), /* … */)
}
```

**`findEvent(id)` resolves `events[id]` then `conditionalEvents` — nothing
else.** There are no shared event libraries; every id referenced by `next`,
`scheduleEvent`, or `PoolEntry` must be defined in this same graph.

## Era definitions (`scenarios/EraDefinition.kt`)

`EraRegistry` holds `kz_2024`, `kz_90s`, `kz_2005`, `kz_2015`. **All have empty
`globalEvents` and empty `poolWeightModifiers`** — priority-tier 1 and era
weight modifiers are engine capabilities that no content uses yet. Do not
invent library event ids (`era_ussr_collapse` etc. do not exist). Historical
crises belong in your graph as scheduled-backbone beats.

## Registration & test enrollment

1. `scenarios/Scenarios.kt` → add a `"{characterId}" -> XxxScenarioGraph()`
   branch in `ScenarioGraphFactory.buildGraph()`. (`forCharacter()` is the
   public cached entry; `forEra()` serves empty era shells for unknown ids.)
2. `shared/src/commonTest/.../scenarios/ScenarioTestCatalog.kt` → add
   `"characterId" to "eraId"` to **`authoredCombos`** (not `emptyShellCombos`).
   Picked up automatically by `ScenarioGraphContentTest`,
   `ScenarioSimulationTest`, and the regression suites.
