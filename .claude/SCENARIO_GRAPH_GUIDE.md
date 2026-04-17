# ScenarioGraph AI Writing Guide

This document teaches an AI how to write a complete `ScenarioGraph` for a new character + era combination in **Financial Life V2**.

---

## What Is a ScenarioGraph?

A `ScenarioGraph` is an abstract class that defines a character's full game scenario:

1. **`initialPlayerState`** — starting financial snapshot for the character
2. **`events`** — `Map<String, GameEvent>` with all story and pool event definitions
3. **`conditionalEvents`** — `List<GameEvent>` triggered by player state (debt crisis, burnout, etc.)
4. **`eventPool`** — `List<PoolEntry>` — weighted references into `events` for random selection

The engine selects the **next event** using a 4-tier priority queue every time a choice leads to a monthly tick:

```
Priority 1: Era-scheduled global events (world crises by real calendar date)
Priority 2: Deferred consequences (scheduled by a prior choice via ScheduledEvent)
Priority 3: Conditional events (state-triggered, checked highest priority first)
Priority 4: Weighted pool events (filtered, weight-adjusted, random)
```

---

## File Location and Naming

```
shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/characters/
└── {Character}{Era}ScenarioGraph.kt
```

Example: `Aidar90sScenarioGraph.kt`, `ZarinaScenarioGraph.kt`

After creating, register the new class in `ScenarioGraphFactory.forCharacter()` in `Scenarios.kt`:

```kotlin
"zarina_2015" -> ZarinaScenarioGraph()
```

---

## DSL Helpers (use these — do NOT construct data classes directly)

All helpers are defined in `Scenarios.kt` and `NarrativeDsl.kt`:

```kotlin
// Create a GameEvent
event(
    id            = "event_id",
    message       = story("Paragraph 1.", "Paragraph 2."),  // or plain string
    flavor        = "📼",        // emoji shown as event icon
    priority      = 0,           // higher = checked sooner (conditional events)
    conditions    = listOf(...), // for conditional/pool events
    tags          = setOf("career", "family"),
    poolWeight    = 10,          // relative weight in pool (1=rare, 10=common)
    unique        = false,       // true = fires only once per session
    cooldownMonths= 0,           // months before can fire again
    isEnding      = false,
    endingType    = null,
    options       = listOf(...)
)

// Create a GameOption
option(
    id   = "option_a",
    text = "Вложить все деньги",
    emoji = "💸",               // required emoji per option
    next = MONTHLY_TICK,        // or "next_event_id"
    fx   = Effect(...)           // optional, defaults to Effect()
)

// Create a Condition.Stat
cond(CAPITAL, LT, 10_000L)      // capital < 10_000
cond(STRESS, GT, 75L)           // stress > 75
cond(MONTH, GTE, 12L)           // month >= 12

// story() joins multiple paragraphs with blank lines
story("First paragraph.", "Second paragraph.")
// → "First paragraph.\n\nSecond paragraph."
```

**`next` values for `option()`:**
- `MONTHLY_TICK` — triggers monthly economic simulation, then picks next event from queue
- `"event_id"` — jumps directly to that story event (no monthly tick)

---

## Data Structures

### PlayerState (Initial State)

```kotlin
PlayerState(
    capital              = 50_000L,   // liquid savings in active currency
    income               = 15_000L,   // monthly gross income
    expenses             = 12_000L,   // fixed monthly expenses
    debt                 = 0L,        // total outstanding debt
    debtPaymentMonthly   = 0L,        // monthly debt repayment
    investments          = 0L,        // money in market
    investmentReturnRate = 0.05,      // ANNUAL return rate (0.08 = 8%/year, computed as /12)
    stress               = 60,        // 0-100 (higher = worse)
    financialKnowledge   = 15,        // 0-100 (higher = better)
    riskLevel            = 40,        // 0-100 (higher = more risk-tolerant)
    month                = 1,
    year                 = 1993,
    characterId          = "aidar_90s",    // unique: "{character}_{era_short}"
    eraId                = "kz_90s",       // lowercase — matches EraRegistry id field
    currency             = CurrencyCode.RUB, // starting currency (default KZT)
    flags                = setOf(),
)
```

**Design rules for initial state:**
- `stress`: poor/crisis context = 60-80; stable middle class = 30-50
- `financialKnowledge`: start low (10-25) — the game teaches it through consequences
- `income - expenses`: small positive net flow (1000-5000) to create slow tension
- `capital`: ~3-6× monthly expenses (not comfortable, not desperate)
- `investmentReturnRate`: **annual** rate, e.g. `0.05` = 5%/year; engine divides by 12 internally
- `currency`: omit (defaults to `CurrencyCode.KZT`) unless the character starts before Nov 1993 in `kz_90s` era → use `CurrencyCode.RUB`. Set monetary values in rubles accordingly (pre-reform scale). The `era_tenge_introduced` event will convert them automatically.

**Era IDs (lowercase strings, not enum names):**

| EraRegistry variable | `eraId` string to use |
|---|---|
| `EraRegistry.KZ_90S` | `"kz_90s"` |
| `EraRegistry.KZ_2015_DEVALUATION` | `"kz_2015"` |
| `EraRegistry.MODERN_KZ_2024` | `"modern_kz_2024"` |

---

### Effect (What a Choice Does)

```kotlin
Effect(
    capitalDelta       = 50_000L,    // immediate cash change (Long)
    incomeDelta        = 5_000L,     // recurring income change
    expensesDelta      = 2_000L,     // recurring expense change
    debtDelta          = 100_000L,   // adds debt (positive = more debt; negative = repay)
    debtPaymentDelta   = 5_000L,     // adds/removes monthly debt payment
    investmentsDelta   = 30_000L,    // moves cash to/from investments
    stressDelta        = -10,        // stress change (Int, clamped 0-100)
    knowledgeDelta     = 5,          // knowledge gain (Int, clamped 0-100)
    riskDelta          = 10,         // risk level change (Int, clamped 0-100)
    setFlags           = setOf("flag.name"),
    clearFlags         = setOf("flag.name"),
    scheduleEvent      = ScheduledEvent(eventId = "event_id", afterMonths = 3),
    monetaryReform     = MonetaryReform(          // ONLY for real currency reform events
        from        = CurrencyCode.RUB,
        to          = CurrencyCode.KZT,
        numerator   = 1L,
        denominator = 500L                        // 500 RUB → 1 KZT
    ),
)
```

**Key points:**
- All monetary fields are `Long` — always use `L` suffix: `50_000L`, not `50_000`
- `scheduleEvent` takes a `ScheduledEvent` object — **NOT** a `Pair`
- `debtDelta` negative = repays debt; positive = adds debt
- `capitalDelta` is immediate cash, not income
- `monetaryReform` — scales **all** monetary fields in `PlayerState` by `numerator/denominator` and switches the active currency. Use **only** in historical currency reform events (e.g., `era_tenge_introduced`). Do NOT use for regular devaluation events — those use `capitalDelta` instead.

### MonetaryReform and CurrencyCode

```kotlin
// Available currency codes:
CurrencyCode.RUB   // Soviet/Russian ruble (pre-1993 KZ)
CurrencyCode.KZT   // Kazakhstani tenge (post Nov 1993)
CurrencyCode.USD   // US dollar

// MonetaryReform applies ratio: new_amount = old_amount * numerator / denominator
MonetaryReform(from = CurrencyCode.RUB, to = CurrencyCode.KZT, numerator = 1L, denominator = 500L)
// → every 500 rubles becomes 1 tenge; capital, income, expenses, debt all rescaled
```

**When to use:**
- `era_tenge_introduced` (1993): `RUB → KZT, 1/500` — already defined in `EraEventLibrary`, no action needed
- A future hypothetical redenomination event

**When NOT to use:**
- Regular devaluation (2015, 2022) — the currency stays KZT, purchasing power drops → use `capitalDelta` or `incomeDelta`

---

### Condition (When to Show a Conditional or Pool Event)

Use the DSL helper `cond()` for `Condition.Stat`, or construct directly:

```kotlin
// Via DSL helper (preferred):
cond(CAPITAL, LT, 10_000L)
cond(STRESS, GT, 75L)
cond(KNOWLEDGE, GTE, 30L)
cond(MONTH, GTE, 12L)
cond(DEBT, GT, 0L)

// Available Field enums: CAPITAL, INCOME, EXPENSES, DEBT, STRESS, KNOWLEDGE, RISK, MONTH
// Available Op enums:    GT, LT, GTE, LTE, EQ, NEQ

// Flag checks (always use class directly, no DSL):
Condition.HasFlag("kiosk.opened")
Condition.NotFlag("parents.warned")

// Era and character targeting:
Condition.InEra("kz_90s")
Condition.ForCharacter("aidar_90s")
```

All conditions in a list must pass simultaneously (AND logic).

---

### GameEvent field reference

```
id            — unique string ID (snake_case)
message       — narrative text; use story() for multi-paragraph
flavor        — emoji icon string: "📼", "💬", "💀", etc.
options       — list of GameOption (via option() DSL)
conditions    — for conditional/pool events; empty for story events
priority      — higher = checked sooner among conditional events
tags          — set of strings: "career", "scam", "scam.pyramid", "family", "crisis"
poolWeight    — relative weight in pool selection (default 10)
unique        — true = fires only once per session (required for all story events)
cooldownMonths— min months before re-triggering (0 = no cooldown)
isEnding      — true = game over node
endingType    — EndingType enum or null
```

---

## ScenarioGraph Class Structure

```kotlin
class ZarinaScenarioGraph : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        characterId = "zarina_2015",
        eraId = "kz_2015",
        // ... other fields
    )

    override val events: Map<String, GameEvent> = buildMap {
        // All story events AND pool events go here, keyed by id
        put("intro", event(id = "intro", ...))
        put("normal_life", event(id = "normal_life", ...))
        // ...
    }

    override val conditionalEvents: List<GameEvent> = listOf(
        event(id = "debt_crisis", conditions = listOf(cond(DEBT, GT, 500_000L)), priority = 100, ...),
        // ...
    )

    // PoolEntry references event IDs defined in events{}
    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", baseWeight = 10),
        PoolEntry("forex_scam",  baseWeight = 3),
        PoolEntry("job_offer",   baseWeight = 5),
        // Can also reference ScamEventLibrary events by id:
        // PoolEntry("scam_crypto_pump", baseWeight = 4),
    )
}
```

**Critical:** `eventPool` contains only `PoolEntry(eventId, baseWeight)` references — the actual event definitions live in `events` map or `ScamEventLibrary`/`EraEventLibrary`.

---

## How to Write a Complete ScenarioGraph

### Step 1: Define the Character

Answer these questions before writing:
- **Name & Age** — who is this person in what year?
- **Starting class** — poor worker, middle class, student, entrepreneur?
- **Era** — which era? (`"kz_90s"`, `"kz_2015"`, `"modern_kz_2024"`)
- **Core conflict** — what financial problem defines their arc?
- **Theme** — what financial lesson does this character teach?

### Step 2: Design the Initial State

`capital = 3-6 × monthly expenses`. Net flow slightly positive (1000-5000). Knowledge = 10-25. Stress = 40-70.

### Step 3: Plan the Narrative Arc

Structure the main story in chapters mapped to months:

```
Month 1-3:   Introduction + first crisis (sets up the world)
Month 4-6:   First major opportunity OR historical event
Month 7-12:  Entrepreneurship / career decision
Month 13-18: Complication (scam, debt, relationship)
Month 19-24: Historical world event (era-specific)
Month 25-36: Late-game consequences + moral test
Month 37+:   Final choice → multiple endings
```

Each chapter = 2-5 story events. Each event must offer at least 2 choices.

### Step 4: Write Story Events (in `events` map)

Rules:
- `unique = true` for all story events
- `message`: use `story("Para 1.", "Para 2.")` for multi-paragraph text
- Use `next = MONTHLY_TICK` for choices that advance time; `next = "event_id"` for immediate branching
- Use `scheduleEvent = ScheduledEvent("consequence_id", afterMonths = 2)` for realistic delays

### Step 5: Write Conditional Events (8-12 events)

Required conditionals:

| Event ID | Trigger | Priority |
|---|---|---|
| `bankruptcy_trigger` | `cond(CAPITAL, LTE, 0L)` + `cond(STRESS, GTE, 90L)` | 200 |
| `debt_crisis` | `cond(DEBT, GT, 100_000L)` | 100 |
| `burnout_{era}` | `cond(STRESS, GT, 75L)` | 60, `cooldownMonths = 6` |
| `trap_warning` | low capital + low knowledge + `cond(MONTH, GTE, 6L)` | 50 |
| `bonus_received` | high capital + `cond(KNOWLEDGE, GTE, 30L)` | 30, `cooldownMonths = 12` |

### Step 6: Write Pool Events (30-50 events) + PoolEntry list

All pool events are defined in `events` map, then referenced in `eventPool`:

| Category | Tags | baseWeight | Notes |
|---|---|---|---|
| Normal life hub | `"normal_life"` | 10 | Monthly catch-all, 4 minor choices |
| Career | `"career"` | 5-6 | Job offers, promotions, layoffs |
| Scam | `"scam"`, `"scam.pyramid"` | 3-4 | Era-modulated via poolWeightModifiers |
| Family | `"family"` | 4-5 | Celebrations, emergencies |
| Investment | `"investment"` | 3-4 | Opportunities with risk |
| Crisis | `"crisis"` | 3 | Medical, theft, legal |
| Windfall | `"windfall"` | 2 | Rare luck events |

**Scam events must:**
- Have tags `"scam"` and `"scam.{type}"` (e.g., `"scam.pyramid"`, `"scam.forex"`)
- Add `Condition.NotFlag("learned.scam.{type}")` in conditions
- Set `"learned.scam.{type}"` flag on the "refuse" option's `fx`

### Step 7: Design Endings (5 types, in `events` map)

```kotlin
put("ending_bankruptcy", event(
    id = "ending_bankruptcy",
    isEnding = true,
    endingType = EndingType.BANKRUPTCY,
    message = "Долги поглотили всё...",
    flavor = "💀",
    unique = true,
    options = listOf(
        option("ok", "Принять", "😔", next = MONTHLY_TICK)
    )
))
```

| EndingType | Connect from |
|---|---|
| `BANKRUPTCY` | `bankruptcy_trigger` conditional or `final_choice` |
| `PAYCHECK_TO_PAYCHECK` | `final_choice` |
| `FINANCIAL_STABILITY` | `final_choice` |
| `FINANCIAL_FREEDOM` | `final_choice` |
| `WEALTH` | `final_choice` |

---

## Flag Naming Conventions

```
"learned.scam.{type}"   →  "learned.scam.pyramid", "learned.scam.forex"
"lost_money_to_scam"    →  universal "burned by scam" flag
"{domain}.{state}"      →  "kiosk.opened", "parents.warned", "parents.lost_money"
"chose.{option}"        →  "chose.emigrate", "chose.build_empire"
```

---

## Era Events (Global Historical Crises)

Defined in `EraEventLibrary` — referenced automatically via `EraRegistry` when `eraId` matches. You do NOT define these or put them in `eraEventIds`. They fire when the in-game calendar reaches their date.

| Event ID | Calendar date | Era |
|---|---|---|
| `era_ussr_collapse` | Dec 1991 | `"kz_90s"` |
| `era_tenge_introduced` | Nov 1993 | `"kz_90s"` |
| `era_mmm_wave_90s` | Jun 1994 (p=0.9) | `"kz_90s"` |
| `chechen_war_broadcast` | Dec 1994 | `"kz_90s"` |
| `nuclear_disarmament_reaction` | Apr 1995 | `"kz_90s"` |
| `capital_move_debate` | Dec 1997 | `"kz_90s"` |
| `era_devaluation_2015` | Aug 2015 | `"kz_2015"` |
| `era_covid_shock_2020` | Mar 2020 | `"modern_kz_2024"` |
| `era_kz_devaluation_2022` | Mar 2022 (p=0.85) | `"modern_kz_2024"` |

If the character's story starts after some of these dates, those earlier events simply won't fire.

---

## Complete Minimal Example

```kotlin
package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*
import kz.fearsom.financiallifev2.scenarios.*

class ZarinaScenarioGraph : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital              = 300_000L,
        income               = 80_000L,
        expenses             = 65_000L,
        debt                 = 0L,
        debtPaymentMonthly   = 0L,
        investments          = 0L,
        investmentReturnRate = 0.07,    // 7% annual
        stress               = 45,
        financialKnowledge   = 20,
        riskLevel            = 30,
        month                = 3,
        year                 = 2014,
        characterId          = "zarina_2015",
        eraId                = "kz_2015",
    )

    override val events: Map<String, GameEvent> = buildMap {

        // ── STORY EVENTS ─────────────────────────────────────────────

        put("intro", event(
            id      = "intro",
            message = story(
                "Алматы, март 2014. Ты — Зарина, 28 лет, менеджер в банке.",
                "Коллеги шепчутся о девальвации. Слухи или реальность?"
            ),
            flavor  = "🏦",
            unique  = true,
            options = listOf(
                option("ignore",      "Не обращать внимание",         "🤷", next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)),
                option("investigate", "Поспрашивать знакомых из НБК", "🔍", next = "insider_info",
                    fx = Effect(knowledgeDelta = 10)),
            )
        ))

        put("insider_info", event(
            id      = "insider_info",
            message = story(
                "Бывший коллега шёпотом: «Скупай доллары».",
                "Это инсайдерская информация. Рискнёшь?"
            ),
            flavor  = "🤫",
            unique  = true,
            options = listOf(
                option("buy_usd", "Перевести 200,000 в доллары", "💵", next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -200_000L, setFlags = setOf("hedged.usd"))),
                option("refuse",  "Слишком рискованно",          "🛑", next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)),
            )
        ))

        put("final_choice", event(
            id      = "final_choice",
            message = "2019 год. Ты пережила девальвацию. Что теперь?",
            flavor  = "🔮",
            unique  = true,
            options = listOf(
                option("invest",  "Открыть инвестиционный счёт", "📈", next = "ending_freedom",
                    fx = Effect(setFlags = setOf("chose.invest"))),
                option("safe",    "Оставить всё в депозите",     "🏦", next = "ending_stability"),
                option("emigrate","Уехать за рубеж",             "✈️", next = "ending_paycheck"),
            )
        ))

        // ── ENDINGS ──────────────────────────────────────────────────

        put("ending_freedom", event(
            id = "ending_freedom", isEnding = true, endingType = EndingType.FINANCIAL_FREEDOM,
            message = "Пассивный доход покрывает расходы. Ты свободна.", flavor = "🌅", unique = true,
            options = listOf(option("ok", "Конец истории", "🏆", next = MONTHLY_TICK))
        ))
        put("ending_stability", event(
            id = "ending_stability", isEnding = true, endingType = EndingType.FINANCIAL_STABILITY,
            message = "Надёжный консервативный путь. Не богатство, но стабильность.", flavor = "⚖️", unique = true,
            options = listOf(option("ok", "Конец истории", "👍", next = MONTHLY_TICK))
        ))
        put("ending_paycheck", event(
            id = "ending_paycheck", isEnding = true, endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            message = "Зарплата уходит сразу на жизнь. Всё по-старому.", flavor = "😔", unique = true,
            options = listOf(option("ok", "Конец истории", "😔", next = MONTHLY_TICK))
        ))
        put("ending_bankruptcy", event(
            id = "ending_bankruptcy", isEnding = true, endingType = EndingType.BANKRUPTCY,
            message = "Долги поглотили всё.", flavor = "💀", unique = true,
            options = listOf(option("ok", "Конец истории", "😭", next = MONTHLY_TICK))
        ))
        put("ending_wealth", event(
            id = "ending_wealth", isEnding = true, endingType = EndingType.WEALTH,
            message = "Ты построила настоящее состояние.", flavor = "👑", unique = true,
            options = listOf(option("ok", "Конец истории", "🎉", next = MONTHLY_TICK))
        ))

        // ── POOL EVENTS ───────────────────────────────────────────────

        put("normal_life", event(
            id = "normal_life",
            message = "Обычный месяц. Небольшие решения.",
            flavor = "📅",
            tags = setOf("normal_life"),
            options = listOf(
                option("save",        "Отложить лишнее в сбережения",  "💰", next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 5_000L)),
                option("spend",       "Позволить себе отдых",          "🌴", next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -8_000L, stressDelta = -10)),
                option("invest_small","Попробовать ПИФ",               "📈", next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, investmentsDelta = 10_000L, knowledgeDelta = 2)),
                option("learn",       "Прочитать книгу по финансам",   "📚", next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)),
            )
        ))

        put("forex_scam", event(
            id         = "forex_scam",
            message    = "Незнакомец обещает 30% в месяц на форекс. Очень убедительно.",
            flavor     = "🎰",
            tags       = setOf("scam", "scam.forex"),
            conditions = listOf(Condition.NotFlag("learned.scam.forex")),
            cooldownMonths = 12,
            options = listOf(
                option("invest_scam", "Вложить 50,000",                    "💸", next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, setFlags = setOf("lost_money_to_scam"))),
                option("refuse_scam", "Отказаться — слишком хорошо звучит","🛑", next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 8, setFlags = setOf("learned.scam.forex"))),
            )
        ))
    }

    override val conditionalEvents: List<GameEvent> = listOf(

        event(
            id         = "bankruptcy_trigger",
            message    = "Капитал обнулён. Нервы на пределе. Это конец.",
            flavor     = "💀",
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            priority   = 200,
            isEnding   = true,
            endingType = EndingType.BANKRUPTCY,
            unique     = true,
            options    = listOf(option("accept", "Принять поражение", "😭", next = MONTHLY_TICK))
        ),

        event(
            id         = "debt_crisis",
            message    = story(
                "Выплаты по кредиту съедают весь доход. Нужно действовать срочно.",
            ),
            flavor     = "💳",
            conditions = listOf(cond(DEBT, GT, 500_000L)),
            priority   = 100,
            cooldownMonths = 3,
            options = listOf(
                option("restructure", "Запросить реструктуризацию в банке", "🏦", next = MONTHLY_TICK,
                    fx = Effect(debtPaymentDelta = -10_000L, stressDelta = 10)),
                option("sell_asset",  "Продать машину — закрыть часть долга","🚗", next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 200_000L, debtDelta = -200_000L)),
            )
        ),

        event(
            id             = "burnout_2015",
            message        = "Хроническое напряжение подкашивает. Надо сделать паузу.",
            flavor         = "😮‍💨",
            conditions     = listOf(cond(STRESS, GT, 75L)),
            priority       = 60,
            cooldownMonths = 6,
            options = listOf(
                option("rest",    "Взять отпуск за свой счёт", "🌿", next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = -20_000L, stressDelta = -25)),
                option("push_on", "Терпеть — деньги нужны",   "💪", next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = 3)),
            )
        ),
    )

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", baseWeight = 10),
        PoolEntry("forex_scam",  baseWeight = 3),
        // Add more PoolEntry(...) references here
        // Can also reference ScamEventLibrary events by id
    )
}
```

---

## Quality Checklist

Before submitting a ScenarioGraph, verify:

- [ ] Class declaration: `class XxxScenarioGraph : ScenarioGraph()` (not `object`, not `fun build()`)
- [ ] Registered in `ScenarioGraphFactory.forCharacter()` in `Scenarios.kt`
- [ ] `characterId` is unique (`"{name}_{era_short}"`)
- [ ] `eraId` is lowercase and matches the `id` field of an `EraRegistry` entry
- [ ] Starting `capital` = roughly 3-6× monthly expenses
- [ ] Net monthly flow is slightly positive (`income - expenses > 0`)
- [ ] `investmentReturnRate` is annual (e.g., `0.07` = 7%/year)
- [ ] `currency` is set to `CurrencyCode.RUB` if character starts before Nov 1993 in `kz_90s`; otherwise omit (defaults to KZT)
- [ ] All monetary `Effect` fields use `Long` suffix: `50_000L`, not `50_000`
- [ ] `scheduleEvent` uses `ScheduledEvent(eventId, afterMonths)` — not `Pair`
- [ ] `Condition.Stat` uses enum fields: `cond(CAPITAL, LT, 10_000L)` — not strings
- [ ] All story events have `unique = true`
- [ ] All events have at least 1 option (endings should have 1 closing option)
- [ ] Every `option()` has an `emoji` argument
- [ ] Scam events have `"learned.scam.X"` flag on the "refuse" option's `fx`
- [ ] `bankruptcy_trigger` conditional event is present (priority 200)
- [ ] `debt_crisis` conditional event is present (priority 100)
- [ ] `burnout` conditional event is present (stress > 75, cooldown 6m, priority 60)
- [ ] All 5 `EndingType` values are covered in `events` map
- [ ] `final_choice` event connects to endings
- [ ] `eventPool` contains `PoolEntry(id, weight)` — NOT `GameEvent` directly
- [ ] All IDs in `eventPool` exist in `events`, `conditionalEvents`, `ScamEventLibrary`, or `EraEventLibrary`
- [ ] `normal_life` event has `poolWeight`/`baseWeight = 10` and 4 options
- [ ] At least 30 pool entries total

---

## Common Mistakes

| Mistake | Fix |
|---|---|
| `object FooGraph` instead of `class FooGraph : ScenarioGraph()` | Must be a class extending `ScenarioGraph` |
| `ScenarioData(...)` — this type doesn't exist | Use the abstract class structure |
| `GameOption(id, text, effect, nextEventId)` | Use DSL: `option(id, text, emoji, next, fx)` |
| `scheduleEvent = Pair("id", 3)` | Use `ScheduledEvent(eventId = "id", afterMonths = 3)` |
| `Condition.Stat("capital", "LT", 10_000)` | Use `cond(CAPITAL, LT, 10_000L)` or enum form |
| `title = "..."` in `event()` | No `title` field — use `message` + `flavor` (emoji) |
| `eraId = "KZ_90S"` | Must be lowercase: `"kz_90s"` |
| `Effect(capitalDelta = 50_000)` without L | Must be Long: `50_000L` |
| `investmentReturnRate = 0.07 // monthly` | It's **annual** — `0.07` = 7%/year |
| Pool event defined only in `eventPool` list | Event must be in `events` map; `eventPool` holds refs |
| Forgetting to register in `ScenarioGraphFactory` | Add `"char_id" -> MyGraph()` in `Scenarios.kt` |
| `era_constitution_1995` in era events table | That ID doesn't exist in `EraEventLibrary` |
| `era_russia_crisis_1998` in era events table | That ID doesn't exist in `EraEventLibrary` |
| `monetaryReform` on a devaluation choice | `MonetaryReform` is only for currency switches (RUB→KZT); devaluations use `capitalDelta` |
| `currency = CurrencyCode.KZT` for a 1991 character | Characters starting before Nov 1993 should use `CurrencyCode.RUB` |

---

## Writing Style Guide

**`message` / `story()` text:**
- 2nd person singular: "Ты", "Тебе", "Твой"
- Present tense or vivid past: "Ты стоишь у прилавка..."
- Emotionally grounded: show human stakes, not just numbers
- Historically specific: real prices, goods, places of the era
- Use `story("Para 1.", "Para 2.")` for multi-paragraph structure

**`option()` text:**
- Start with an infinitive verb: "Вложить", "Отказаться", "Спросить совета"
- Be concrete: "Взять кредит 200,000 тенге" not "Взять кредит"
- Imply risk without spoiling: "Рискнуть — всё или ничего"
- Max ~15 words

**Tone:** Grounded, slightly tense, never preachy. The game teaches through consequences, not lectures.
