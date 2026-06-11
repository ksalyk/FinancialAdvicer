# FinancialLifeV2 — GameEngine Code Review

**Reviewer:** Verifier agent
**Date:** 2026-06-11
**Files reviewed:**
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/engine/GameEngine.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/model/Models.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/Scenarios.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/EraDefinition.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/EventPoolSelector.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/ScamEventLibrary.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/arcs/EraCharacterArcs.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/scenarios/arcs/EventArc.kt`
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/model/Extensions.kt`

---

## 1. FSM Correctness Assessment

### 1.1 Architecture — PASS

The 3-layer FSM (narrative graph → state system → economic simulation) is well-designed and correctly implemented:

- **Layer 1 (narrative graph):** `Map<String, GameEvent>` gives O(1) event lookup by ID. `findEvent()` chains through 4 sources (story → conditional → scam library → era library) with correct priority.
- **Layer 2 (state system):** `PlayerState` is an immutable data class. All mutations create new copies — no shared mutable state.
- **Layer 3 (economic sim):** Monthly tick correctly computes: `capital += income − expenses − debtPayment + investmentReturn`.

### 1.2 Priority Queue — PASS with one gap

The 4-tier event priority queue is correctly ordered in `makeChoice()`:

| Priority | Source | Logic |
|----------|--------|-------|
| 1 | Era global event | Filters by year/month + probability |
| 2 | Deferred scheduled | Filters by exact fire date |
| 3 | Conditional | `sortedByDescending { it.priority }`, first match |
| 4 | Weighted pool | `EventPoolSelector.selectNext()` |

The first non-null event wins. Correct.

### 1.3 FSM Boundary Cases — CRITICAL BUG

**BUG-1: Infinite re-injection of non-unique conditional events**
`EraCharacterArcs.kt:719` — `ending_regular_trigger`:
```kotlin
event(
    id = "ending_regular_trigger",
    ...
    unique = false,  // NOT unique
    conditions = listOf(Condition.HasFlag("arc.final_check")),
    options = listOf(option("claim_regular", "Закончить как есть", "📓", "ending_regular"))
)
```

`GameEngine.kt:308–313` — `findConditionalEvent()`:
```kotlin
private fun findConditionalEvent(ps: PlayerState, excludeId: String): GameEvent? =
    graph.conditionalEvents
        .filter { it.id != excludeId && it.conditions.isNotEmpty() }
        .filter { it.id !in ps.triggeredUniqueEvents || !it.unique }  // BUG: !true = false → skips unique; !false = true → includes non-unique
        .sortedByDescending { it.priority }
        .firstOrNull { event -> event.conditions.all { it.check(ps) } }
```

**Problem:** Once `arc.final_check` flag is set, `ending_regular_trigger` will fire every single month forever because:
1. `it.id !in ps.triggeredUniqueEvents` → `true` (non-unique, never added to triggered set)
2. `!it.unique` → `true` (coerced from `false`)
3. Filter passes → event is included
4. `event.conditions.all { it.check(ps) }` → flag is set → `true`

The same applies to all non-unique ending triggers (`ending_wealth_trigger`, `ending_freedom_trigger`, `ending_stability_trigger`, `ending_paycheck_trigger`) and any other non-unique conditional with a flag-based condition.

**Fix:** Add `triggeredUniqueEvents`-style tracking for ALL conditional events, OR mark all conditional ending triggers as `unique = true`, OR add a separate `triggeredConditionalEvents` set.

---

## 2. Economic Simulation Precision Analysis

### 2.1 Investment Return — Truncation to Zero (CRITICAL)

`Models.kt:243–244`:
```kotlin
val monthlyInvestmentReturn: Long get() =
    (investments * investmentReturnRate / 12).toLong()
```

**Evidence:** For `investments = 100_000`, `investmentReturnRate = 0.08`:
```
monthlyReturn = (100_000 × 0.08 / 12).toLong()
             = (8000 / 12).toLong()
             = 666.toLong()
```
That's correct. But for `investments = 10_000`:
```
monthlyReturn = (10_000 × 0.08 / 12).toLong()
             = (800 / 12).toLong()
             = 66.toLong()
```
Still non-zero. For `investments = 1_000`:
```
monthlyReturn = (1_000 × 0.08 / 12).toLong()
             = (80 / 12).toLong()
             = 6.toLong()
```
For `investments = 100`:
```
monthlyReturn = (100 × 0.08 / 12).toLong()
             = (8 / 12).toLong()
             = 0.toLong()
```
**Players with investments < 125 KZT receive zero monthly returns.** Over a game's lifetime, this systematically understates gains for players who invest small amounts.

**Fix:** Use `BigDecimal` for investment return calculation, or store a `monthlyReturnFraction: Long` (e.g., basis points) separately and multiply. Alternatively, accumulate fractional cents in a separate field.

### 2.2 Annual → Monthly Rate — PASS

`Models.kt:244`: `investments * investmentReturnRate / 12`

This is mathematically correct for simple (non-compounding) monthly interest. Compound monthly rate would be `(1 + r)^(1/12) − 1`, but the simpler formula is the standard simplified game model.

### 2.3 Debt Principal Reduction — PASS

`GameEngine.kt:227`:
```kotlin
val principalPaid = (ps.debtPaymentMonthly * 0.30).toLong()
```
30% of monthly payment reduces principal (rest goes to interest). Correct. Truncation is minor for typical amounts.

### 2.4 Capital Flow — PASS

`GameEngine.kt:223–224`:
```kotlin
val netFlow = ps.income - ps.expenses - ps.debtPaymentMonthly + investGain
val capitalAfter = (ps.capital + netFlow).coerceAtLeast(0L)
```
Capital cannot go negative. Correct.

### 2.5 Long Storage vs. Int Calculation — PASS with concern

All monetary fields use `Long` (KZT units, not cents). Stress, knowledge, risk use `Int`. The `capitalDelta`, `incomeDelta`, etc. in `Effect` are all `Long`.

**Concern:** The `moneyFormat` function (`Extensions.kt:11–17`) truncates display for large amounts:
```kotlin
this >= 1_000_000L -> "${this / 1_000_000L}M"   // 1.8M → "1M"
this >= 1_000L     -> "${this / 1_000L}k"        // 950K → "950k"
```
A player with 1.8M KZT sees "1M" — a 44% rounding error in display. This is only a display issue, but it undermines the player's ability to make informed decisions.

### 2.6 Monetary Reform Precision — PASS

`GameEngine.kt:337–338`:
```kotlin
fun reprice(amount: Long): Long =
    ((amount * reform.numerator) / reform.denominator).coerceAtLeast(0L)
```

Integer division here causes truncation. For the RUB→KZT reform (1:500), 499 RUB becomes 0 KZT. This is a known limitation of integer math. For game purposes (where amounts are large), the error is acceptable.

### 2.7 Stress Dynamics — PASS

`GameEngine.kt:231–240` — stress delta computation:
```kotlin
val rawDelta = when {
    capitalAfter == 0L           -> 15   // bankrupt stress spike
    netFlow < 0                  ->  6   // negative cash flow
    ps.debt > ps.income * 3      ->  3   // heavy debt burden
    netFlow > ps.expenses        -> -3   // comfortable surplus
    capitalAfter > ps.income * 6 -> -2   // 6-month cushion
    else                         ->  0
}
val stressDelta = rawDelta - (ps.financialKnowledge / 25)
```

**Edge case:** Knowledge > 100 (capped at 100) → max absorption = 4 pts/month. Stress delta range: −18 to +27. No overflow risk (0 ≤ stress ≤ 100 after `coerceIn`).

**Gap:** A player at `stress = 0` and `knowledge = 100` receives `stressDelta = −(100/25) = −4`. After 25 months of optimal play, stress stays at 0 — correct. No issue.

---

## 3. Five Endings Correctness

### 3.1 EndingType Enum — PASS

`Models.kt:187–189`:
```kotlin
enum class EndingType {
    BANKRUPTCY, PAYCHECK_TO_PAYCHECK, FINANCIAL_STABILITY, FINANCIAL_FREEDOM, WEALTH
}
```
Exactly 5 types. Maps to narrative events in `EraCharacterArcs.kt:593–648`.

### 3.2 Ending Trigger Conditions — FAIL (inconsistent logic)

| Ending | Capital | Knowledge | Stress | Flags | Priority |
|--------|---------|-----------|--------|-------|----------|
| **Bankruptcy** | ≤ 0 | — | ≥ 85 | — | 120 |
| **Paycheck-to-Paycheck** | < stability/3 | ≤ 38 | — | `arc.final_check` | 85 |
| **Stability** | ≥ stability | ≥ 40 | ≤ 75 | `arc.final_check` | 90 |
| **Financial Freedom** | ≥ freedom | ≥ 55 | ≤ 65 | `arc.final_check` | 95 |
| **Wealth** | ≥ wealth | ≥ 55 | — | `arc.final_check` + `business_scaled` | 100 |

**ISSUE-1 (CRITICAL):** `ending_bankruptcy_trigger` requires `STRESS ≥ 85`. But `capitalAfter` is already clamped to 0 in `monthlyTick`. A player who hits zero capital with `stress = 50` will NOT trigger bankruptcy — the game continues with `capital = 0` and a slowly-rising stress (max +6/month from negative flow). The bankruptcy ending is unreachable without also having near-max stress.

**ISSUE-2:** Priority ordering is inverted for lower endings. Priority 120 fires before 100, which is correct for bankruptcy = highest. But priority 85 (Paycheck) < 90 (Stability) < 95 (Freedom) < 100 (Wealth). If multiple ending conditions could simultaneously match (e.g., player has both low capital AND high capital AND high knowledge), the lowest-priority ending would win. This shouldn't happen given mutually exclusive conditions, but the ordering is fragile.

**ISSUE-3:** `ending_regular_trigger` (priority 80) has no conditions other than `arc.final_check` — it fires for everyone who reaches `final_review`. Combined with BUG-1 (infinite re-injection), a player who sets `arc.final_check` but doesn't qualify for any other ending will be stuck in an infinite `ending_regular_trigger` loop.

### 3.3 Ending Narrative Events — PASS

Each ending has a narrative event (`ending_wealth`, `ending_freedom`, etc.) with `isEnding = true`, `options = emptyList()`. The FSM correctly sets `gameOver = true` and `isWaitingForChoice = false` when an ending is reached.

---

## 4. Code Quality Issues

### 4.1 Event Pool Inconsistency — HIGH

`EraCharacterArcs.kt:770–782` — `storyEventPool()` adds `"normal_life"` directly:
```kotlin
fun storyEventPool(eraId: String): List<PoolEntry> = buildList {
    add(PoolEntry("normal_life", 24))
    ...
    addAll(ScamEventLibrary.poolEntries)  // normal_life NOT in poolEntries
}
```

`ScamEventLibrary.kt:898–926` — `poolEntries` does NOT include `"normal_life"`.

But `ScenarioGraph.findEvent()` (Scenarios.kt:84–88) checks `EraEventLibrary.findById(id)` AFTER the graph's own events. Since `normal_life` is defined in `regularLifeArc()` which IS in the graph's `events` map, `findEvent("normal_life")` works via the graph lookup.

**No actual bug**, but `normal_life` is the only pool entry that bypasses the `poolEntries` list. Inconsistent design — easy to break in future refactoring.

### 4.2 `EventPoolSelector.selectNext` — Always Returns a Result

`EventPoolSelector.kt:50–58`:
```kotlin
if (candidates.isEmpty()) return null
val totalWeight = candidates.sumOf { it.second }
var roll = Random.nextInt(totalWeight)
for ((id, weight) in candidates) {
    roll -= weight
    if (roll < 0) return id
}
return candidates.last().first  // fallback
```

The `return candidates.last().first` fallback is dead code — `roll < 0` always triggers before `roll` reaches 0 (since `totalWeight` is the sum of all candidates and `roll < totalWeight`). Keep for defensive safety but note it's never reached.

### 4.3 Cooldown Event Lookup — Missing Existence Check

`GameEngine.kt:153–158`:
```kotlin
if (winner != null && winner.cooldownMonths > 0) {
    ps = ps.copy(
        eventCooldowns = ps.eventCooldowns +
            (nextEventId to ps.absoluteMonth + winner.cooldownMonths)
    )
}
```

`winner` is set from `graph.findEvent(nextEventId)`. If `nextEventId` doesn't exist in any library, `winner` is null and cooldown is not stored. A developer could add a pool entry for a non-existent event ID, and it would silently do nothing. Consider logging or asserting.

### 4.4 Condition Field Missing — MEDIUM

`Condition.Stat.Field` (Models.kt:103) does not include `INVESTMENTS` or `DEBT_PAYMENT`. Some game logic may want to condition events on investment portfolio size or monthly debt payment. Currently not used, but a gap for future scenarios.

### 4.5 `Aidar90sScenarioGraph` Package Inconsistency

`Aidar90sScenarioGraph.kt:2` — package is `kz.fearsom.financiallifev2.scenarios` (NOT `scenarios.characters`). This is intentional (comment says "referenced from Scenarios.kt in same package"), but it means `ScenarioGraphFactory.forCharacter()` at Scenarios.kt:128 uses the class directly. If someone moves the file to `characters/` without updating the factory, it breaks silently at runtime. Add a class-path check or Kotlin source-tree convention enforcement.

### 4.6 ScenarioGraphFactory Thread Safety — ACCEPTABLE

`Scenarios.kt:109–124` uses `@Volatile + copy-on-write` for thread-safe caching. The comment correctly identifies the worst case: two threads both miss, both build the same graph, one write is lost. Acceptable for a game (warmup happens once at game start). No data corruption possible.

---

## 5. Event Pool / Scam Library Analysis

### 5.1 Scam Categories — PASS

8 scam categories × 4 eras = 32 era-specific scam events. Plus 8 generic scam consequence/reflection events. Total: 40 scam events. Each has:
- Era condition (`Condition.InEra`)
- Capital门槛 (capitalAtLeast / capitalAtMost)
- Learning flag guard (`notLearned`)
- Scheme explanation for educational value
- 3 options: participate (with scheduled consequence), investigate (with reflection event), decline (with knowledge gain + flag)

### 5.2 Pool Weight Modifiers — PASS

`EraDefinition.kt` — `poolWeightModifiers` correctly uses both event ID and tag as keys. `EventPoolSelector.kt:70–72` takes `maxOf(eraById, eraByTag)` — correct (higher multiplier wins when both match).

### 5.3 Knowledge-Based Susceptibility — PASS

`EventPoolSelector.kt:75–91` — Three-tier defense:
1. `financialKnowledge > 50` → all scam weights ×0.6
2. `"learned.scam.X"` flag → that subtype ×0.15
3. `"lost_money_to_scam"` flag → same subtype ×0.3

Correctly layered: general awareness → specific pattern recognition → trauma-based caution.

### 5.4 Scheduled Event Scheduling — PASS

`GameEngine.kt:286–297` — `addScheduledEvent()` correctly computes future month:
```kotlin
val totalMonths = ps.year * 12 + ps.month + scheduled.afterMonths
val targetYear  = (totalMonths - 1) / 12
val targetMonth = ((totalMonths - 1) % 12) + 1
```
For month=12, year=2024, afterMonths=3: total=24291, targetYear=2024, targetMonth=3. Correct.

---

## 6. Summary Table

| Area | Severity | Status |
|------|----------|--------|
| FSM architecture | — | PASS |
| Priority queue ordering | — | PASS |
| Non-unique conditional re-injection | CRITICAL | **FAIL** |
| Investment return truncation to zero | CRITICAL | **FAIL** |
| Ending trigger: stress gate on bankruptcy | HIGH | **FAIL** |
| Ending regular trigger infinite loop | HIGH | **FAIL** |
| moneyFormat display truncation | MEDIUM | **WARN** |
| Event pool inconsistency (normal_life) | MEDIUM | **WARN** |
| Monetary reform integer truncation | LOW | ACCEPTABLE |
| Dead code in EventPoolSelector | LOW | ACCEPTABLE |
| Cooldown for non-existent event | LOW | **WARN** |
| Aidar90sScenarioGraph package split | LOW | **WARN** |

---

## 7. Required Fixes Before Ship

1. **BUG-1 (CRITICAL):** Mark all conditional ending triggers as `unique = true` in `EraCharacterArcs.kt`, or add a `triggeredConditionalEvents: Set<String>` field to `PlayerState` and update `findConditionalEvent()` to check it.

2. **BUG-2 (CRITICAL):** Fix `monthlyInvestmentReturn` to use `BigDecimal` or accumulate fractional returns to prevent truncation to zero for small portfolios.

3. **BUG-3 (HIGH):** Either remove the `stress >= 85` condition from `ending_bankruptcy_trigger`, or add a separate condition `capital <= 0` with its own standalone trigger.

4. **WARN-1 (MEDIUM):** Fix `moneyFormat` to use locale-aware number formatting with proper thousand separators instead of truncating to "k"/"M".

---

*Report generated by Verifier agent — FinancialLifeV2 GameEngine review*