# Scenario Validation Rules

Every rule the validators enforce, grouped by how it's checked. A scenario is
"solid" only when all four layers pass: **lint → compile → content test →
simulation test**. Each rule lists *why* it matters so reviewers can reason about
edge cases instead of pattern-matching.

Legend: 🧪 simulation harness · 📋 content test · 🐍 static lint · 👁 review-only

---

## 1. Mechanical correctness (compiler + 🐍 lint)

| Rule | Why |
|---|---|
| Money literals use `L` (`50_000L`) on `Long` fields | `capitalDelta` etc. are `Long`; a bare `Int` won't compile. Lint flags it pre-compile. |
| Soft-stat deltas have **no** `L` (`stressDelta = 5`) | `stress/knowledge/risk` are `Int`. |
| `scheduleEvent = ScheduledEvent(id, afterMonths = n)` | It's a typed object, not a `Pair`. |
| `eraId` lowercase (`"kz_90s"`) | Matched case-sensitively against `EraRegistry` ids. |
| Graph is `class X : ScenarioGraph()` | `object` / free function can't be instantiated per character+era by the factory. |
| No direct `GameEvent(...)`/`GameOption(...)`/`Effect(...)` positional construction in scenario files | Use `event/option/cond` DSL; positional ctors drift when fields change. |

## 2. Referential integrity (📋 content + 🧪 reachability)

| Rule | Why |
|---|---|
| Every `option.next` resolves via `findEvent` (or `== MONTHLY_TICK`) | A dangling `next` strands the player on a blank turn. |
| Every `Effect.scheduleEvent.eventId` resolves | A deferred consequence that points nowhere silently no-ops. |
| Every `PoolEntry.eventId` resolves | An unresolved pool ref reduces variety and can skew weights. |
| Every conditional / era-global `eventId` resolves | Same as above for tiers 1 & 3. |
| `"intro"` exists | Engine starts every game at `intro` (`startGame` errors otherwise). |
| `"normal_life"` exists | Hard-coded fallback when the 4-tier queue selects nothing. |

## 3. Endings (📋 content + 🧪 reachability)

| Rule | Why |
|---|---|
| Terminal ending events have `options = emptyList()` | `ScenarioGraphContentTest` asserts endings are terminal; the engine never reads an ending's options. |
| Endings set `isEnding = true` **and** a non-null `endingType` | `endingType == null` on a terminal node is flagged by the harness (`ENDING_WITHOUT_TYPE`); the UI/result screen needs the type. |
| All 5 `EndingType`s have a terminal event | Players must be able to land any outcome the design promises. |
| Each ending is reachable from `intro` | An unreachable ending is dead content / a wiring bug (harness reachability). |
| At least one ending is reachable | A graph with zero reachable endings can never resolve. |
| Endings are reached via a **trigger** event (normal event or conditional whose option `next`s to the ending) | Keeps the terminal node optionless while still routable; `ending_bankruptcy_trigger` (priority 120) is the safety net for `CAPITAL ≤ 0`. |

## 4. Liveness / no traps (🧪 simulation)

The harness drives the real `GameEngine` with random choices across many seeds.

| Rule | Why |
|---|---|
| No exception during `startGame` or `makeChoice` | A crash mid-playthrough is a hard defect (`START_CRASH`/`CHOICE_CRASH`). |
| No dead-end: a non-terminal current event always has ≥ 1 option | Zero options + not `gameOver` freezes the player (`DEAD_END`). |
| Money fields never < 0 at any step | Engine coerces, so a negative here signals an engine regression. |
| `stress`, `financialKnowledge`, `riskLevel` stay in `0..100` | Same — guards the clamp contract. |
| (review) Avoid pure direct-`next` cycles with no `MONTHLY_TICK` | Two events that ping-pong without a tick never advance time; not auto-failed (not always a bug) but flag in review. |

## 5. Balance & pacing (📋 partial + 👁 review)

| Rule | Why |
|---|---|
| `normal_life` baseWeight (10) > any single scam event's weight | Scams must be seasoning, not the default month (`ScenarioGraphContentTest`). |
| Initial `capital ≈ 3–6× expenses`; net flow slightly positive | Creates slow tension; too rich removes stakes, too poor forces instant loss. |
| `financialKnowledge` starts 10–25 | The game teaches via consequences; high start removes the arc. |
| `investmentReturnRate` is annual (`0.05`–`0.12` realistic) | Engine divides by 12; a "monthly" value here yields absurd compounding. |
| Effect deltas scale to the era's money (use `StoryBalance`) | A 50,000₸ hit is catastrophic in `kz_90s`, trivial in `kz_2024`. |
| No unrealistic returns (no "500%/yr", no guaranteed-profit framing) | Project restriction — scam events teach skepticism; don't model real get-rich schemes as winning. |

## 6. Scam-event convention (📋 partial + 👁 review)

| Rule | Why |
|---|---|
| Tags include `"scam"` and a specific `"scam.{type}"` | Drives era weight modifiers + the "knowledge reduces susceptibility" logic in `EventPoolSelector`. |
| Conditions include `Condition.NotFlag("learned.scam.{type}")` | A player who learned the lesson shouldn't be re-offered the identical naive trap. |
| The refuse/skeptical option sets `"learned.scam.{type}"` (and a costly option may set `"lost_money_to_scam"`) | Feeds the weight-reduction model and makes learning persistent. |
| Include a `schemeExplanation` for headline scams | Post-choice teaching moment without lecturing inside the scene. |

## 7. Localization & content (👁 review)

| Rule | Why |
|---|---|
| Player-facing strings via `Strings[...]` keys for shipped library events | Game is tri-lingual (en/kk/ru). Character-local prose may be inline RU per existing graphs, but new shared/era events should be keyed. |
| Tone: 2nd person, era-specific, teach-through-consequences, ≤ ~15-word options starting with an infinitive | Matches the existing voice; keep it grounded, never preachy. |

---

## Quality checklist (paste into the PR / final review)

- [ ] `class XxxScenarioGraph : ScenarioGraph()`; registered in `Scenarios.kt`
- [ ] `characterId` unique; `eraId` lowercase & in `EraRegistry`
- [ ] `intro` and `normal_life` present
- [ ] Initial state: capital ≈ 3–6× expenses, net flow > 0, knowledge 10–25, annual return rate
- [ ] All money `Effect`/`PlayerState` fields use `L`; soft stats don't
- [ ] `scheduleEvent` uses `ScheduledEvent(...)`
- [ ] All `next` / scheduled / pool / conditional ids resolve
- [ ] 5 terminal endings with `options = emptyList()` + `endingType`; each reachable
- [ ] `ending_bankruptcy_trigger` (CAPITAL ≤ 0), `debt_crisis`, `burnout_{era}` conditionals present
- [ ] Scam events follow the `learned.scam.{type}` convention
- [ ] `normal_life` weight > every single scam weight; ≥ 30 pool entries
- [ ] Added to `ScenarioTestCatalog.combos`
- [ ] `python3 .claude/skills/scenario-writer/scripts/validate_scenarios.py` clean
- [ ] `./gradlew :shared:test` green (simulation + content)
