# Scenario Data Model — reference

Authoritative summary of the types in `:shared` (`model/Models.kt`,
`model/GameModels.kt`, `scenarios/`). All snippets compile against the current
engine. Money fields are `Long` (use the `L` suffix); soft stats are `Int`
clamped to `0..100` by the engine.

## DSL helpers (use these in scenario files)

Defined in `scenarios/Scenarios.kt` and `scenarios/NarrativeDsl.kt`:

```kotlin
event(
    id: String,
    message: String,                 // use story(...) for multi-paragraph
    flavor: String = "💬",           // emoji icon
    priority: Int = 0,               // conditional ordering (higher first)
    conditions: List<Condition> = emptyList(),
    tags: Set<String> = emptySet(),
    poolWeight: Int = 10,
    unique: Boolean = false,
    cooldownMonths: Int = 0,
    schemeExplanation: String? = null,
    isEnding: Boolean = false,
    endingType: EndingType? = null,
    options: List<GameOption>,       // LAST, required
): GameEvent

option(id: String, text: String, emoji: String, next: String, fx: Effect = Effect()): GameOption

cond(field: Condition.Stat.Field, op: Condition.Stat.Op, value: Long): Condition.Stat

story(vararg paragraphs: String): String     // joins with blank lines
```

Import the enums for terse conditions:
```kotlin
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*   // CAPITAL, INCOME, EXPENSES, DEBT, STRESS, KNOWLEDGE, RISK, MONTH
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*      // GT, LT, GTE, LTE, EQ, NEQ
```

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
    monetaryReform = null,     // ONLY for real currency switches (RUB→KZT)
)
```

The engine applies deltas then clamps: money `coerceAtLeast(0)`, soft stats
`coerceIn(0,100)`. So you cannot drive a stat out of range via effects — but you
*can* leave the player permanently stuck if you forget to give a path forward.

## Condition (sealed; AND-ed within a list)

```kotlin
cond(CAPITAL, LT, 10_000L)              // Condition.Stat
cond(STRESS, GTE, 75L)
Condition.HasFlag("kiosk.opened")
Condition.NotFlag("learned.scam.forex")
Condition.InEra("kz_90s")
Condition.ForCharacter("aidar_90s")
```

## GameEvent semantics

| field | meaning / rule |
|---|---|
| `id` | unique snake_case; referenced by `next`, `scheduleEvent`, `PoolEntry`, era globals |
| `message` | narrative; supports `{token}` substitution (see below) |
| `options` | story/pool events ≥ 2; **endings = `emptyList()`** |
| `conditions` | required for conditional & pool events; empty for fixed story events |
| `priority` | conditional tier ordering; higher checked first |
| `tags` | pool filtering + scene image + era weight keys (`"scam"`, `"scam.pyramid"`, `"career"`, `"crisis"`, `"mortgage"`, `"family"`, `"investment"`, `"windfall"`, `"world"`) |
| `poolWeight`/`PoolEntry.baseWeight` | relative draw weight |
| `unique` | fires once per session; **true for every story event** |
| `cooldownMonths` | a pool event repeats ONLY if `> 0`; otherwise single-use |
| `isEnding`+`endingType` | terminal node; engine sets `gameOver` when navigated to |

### Message tokens (substituted live from PlayerState)
`{income} {expenses} {capital} {debt} {debtPayment} {investments} {passiveIncome}`
`{netFlow} {income3x} {knowledge} {stress} {name} {eraLabel}`

## PlayerState (initial snapshot)

```kotlin
PlayerState(
    capital = 260_000L, income = 520_000L, expenses = 255_000L,
    debt = 180_000L, debtPaymentMonthly = 30_000L,
    investments = 0L, investmentReturnRate = 0.10,   // annual
    stress = 38, financialKnowledge = 24, riskLevel = 22,
    month = 1, year = 2024,
    characterId = "asan", eraId = "kz_2024",
    currency = CurrencyCode.KZT,                       // RUB only for pre-1993 kz_90s
    flags = emptySet(),
)
```

Derived (read-only): `netMonthlyFlow`, `monthlyInvestmentReturn`, `netWorth`,
`absoluteMonth`.

## ScenarioGraph (the unit you author)

```kotlin
class ZarinaScenarioGraph : ScenarioGraph() {          // class, extends ScenarioGraph — NOT object
    override val initialPlayerState = PlayerState(/* … */)
    override val events: Map<String, GameEvent> = /* buildMap{} OR arcs.buildEvents() */
    override val conditionalEvents: List<GameEvent> = /* storyConditionals(StoryBalance(...)) */
    override val eventPool: List<PoolEntry> = /* storyEventPool(eraId) OR listOf(PoolEntry(...)) */
}
```

`findEvent(id)` resolves in order: `events` → `conditionalEvents` →
`ScamEventLibrary` → `EraEventLibrary`. So a `PoolEntry` or `next` may legally
reference a shared library event you did not define locally.

### Arc composition (preferred for large graphs)
```kotlin
override val events = listOf(
    myMainStoryArc(eraId),
    regularLifeArc(eraId),
    endingsArc(),
).buildEvents()                       // EventArc + List<EventArc>.buildEvents()
```

## Registration & test enrollment

1. `scenarios/Scenarios.kt` → add a branch in `ScenarioGraphFactory.buildGraph()`
   (matched by `characterId`) and/or `forEra()` (matched by `eraId`).
2. `shared/src/commonTest/.../scenarios/ScenarioTestCatalog.kt` → add
   `"characterId" to "eraId"` to `combos`. Picked up automatically by
   `ScenarioGraphContentTest` and `ScenarioSimulationTest`.

## Era events & libraries (do not redefine)

`EraEventLibrary` holds global crises (`era_ussr_collapse`, `era_tenge_introduced`,
`era_mmm_wave_90s`, `era_mortgage_freeze_2008`, `era_devaluation_2015`,
`era_covid_shock_2020`, `era_kz_devaluation_2022`, `era_online_credit_rules_2024`,
…). `EraRegistry` wires them to eras by calendar date + sets `poolWeightModifiers`.
You reference these by id; you do not author them inside a character graph.
`ScamEventLibrary` holds reusable scam events referenced via `PoolEntry`.
