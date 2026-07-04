# Scenario Validation Rules

Every rule the validators enforce, grouped by how it's checked. A scenario is
"solid" only when all four layers pass: **lint → compile → content test →
simulation test**. Each rule lists *why* it matters so reviewers can reason
about edge cases instead of pattern-matching.

Legend: 🧪 simulation harness · 📋 content test · 🐍 static lint · 👁 review-only

---

## 1. Mechanical correctness (compiler + 🐍 lint)

| Rule | Why |
|---|---|
| Money literals use `L` (`50_000L`) on `Long` fields | `capitalDelta` etc. are `Long`; consistent suffixing prevents silent Int/Long drift. Lint warns pre-compile. |
| Soft-stat deltas have **no** `L` (`stressDelta = 5`) | `stress/knowledge/risk` are `Int`. |
| `scheduleEvent = ScheduledEvent(id, afterMonths = n)` | It's a typed object, not a `Pair`. |
| `eraId` lowercase (`"kz_90s"`) | Matched case-sensitively against `EraRegistry` ids. |
| Graph is `class X : ScenarioGraph()` | The factory instantiates it; `object` breaks the per-locale graph cache. |
| No direct `GameEvent(...)`/`GameOption(...)` construction in scenario files | Use the `event/option/ending/cond` DSL; positional ctors drift when fields change. |
| No `isEnding =` in scenario files | `event()` has no such parameter — terminal nodes go through `ending()`, which bakes in `options = emptyList()`. |

## 2. Referential integrity (📋 content + 🧪 reachability)

| Rule | Why |
|---|---|
| Every `option.next` resolves via `findEvent` (or `== MONTHLY_TICK`) | A dangling `next` strands the player on a blank turn. |
| Every `Effect.scheduleEvent.eventId` resolves | A deferred consequence that points nowhere silently no-ops — and may kill fast-forward pacing. |
| Every `PoolEntry.eventId` resolves | An unresolved pool ref reduces variety and can skew weights. |
| Every conditional `eventId` resolves | Same for tier 3. |
| All ids are defined **in the same graph** | `findEvent` = `events[id] ?: conditionalEvents` — there are no shared libraries to fall back on. |
| `"intro"` exists | Engine starts every game at `intro` (`startGame` errors otherwise). |
| `"normal_life"` exists, non-terminal, with options | Hard-coded engine fallback; 📋 `authored graphs define a playable normal_life fallback`. |

## 3. Endings (📋 content + 🧪 reachability)

| Rule | Why |
|---|---|
| Terminal nodes are authored with `ending()` | Guarantees `options = emptyList()` + `isEnding = true` + a non-null `endingType` in one place. 📋 `ending events are terminal`. |
| All 5 `EndingType`s present | 📋 `authored graphs cover all five ending types`. |
| Each ending is reachable from `intro` | 🧪 reachability — an unreachable ending is dead content or a wiring bug. |
| Endings are **terminal-conditionals**: `ending(…, priority = p, conditions = […])` in `conditionalEvents` | The canonical pattern (see `DaniyarScenarioGraph.endingsArc()`): the node triggers *and* resolves after a tick. An option's direct `next` to an ending id is also legal. |
| `ending_bankruptcy` at top priority (~200) with `cond(CAPITAL, LTE, 0L)` | The safety net — fires regardless of story progress. |
| Non-bankruptcy endings gated on an endgame flag (e.g. `arc.final_check`) | Prevents the game resolving before the story arc finishes. |
| Priority order encodes outcome ranking (bankruptcy > wealth > freedom > stability > paycheck) | After the endgame flag is set, the *best satisfied* ending must win the conditional tier. |

## 4. Liveness / no traps (🧪 simulation)

The harness (`ScenarioSimulationHarness`) drives the real `GameEngine` with
random choices — 40 seeds × up to 800 steps per graph.

| Rule | Why |
|---|---|
| No exception during `startGame` or `makeChoice` | A crash mid-playthrough is a hard defect (`START_CRASH`/`CHOICE_CRASH`). |
| No dead-end: a non-terminal current event always has ≥ 1 option | Zero options + not `gameOver` freezes the player (`DEAD_END`). |
| Money fields never < 0; `stress`/`knowledge`/`risk` stay in `0..100` | Engine clamps; a violation here signals an engine regression. |
| (review) Avoid direct-`next` cycles with no `MONTHLY_TICK` | Ping-ponging events never advance time; fine for short scripted chains (Act I), flag anything that can loop. |
| (review) Keep a future scheduled beat pending until the endgame | Fast-forward over filler months only works while a dated future beat exists; a dry backbone = one filler month per click. |

## 5. Balance & pacing (👁 review; sim is the referee)

| Rule | Why |
|---|---|
| `normal_life` carries the top pool weight (10); scams 2–3 | Routine dominates (Bible §5); scams are seasoning, not the default month. |
| ~12–20 pool entries | Shipped range (Daniyar: 15). Fewer = repetitive; more = beats never surface. |
| Repeatable filler: `cooldownMonths` + `maxOccurrences ≈ 3` | Retires filler before it gets stale; `FillerEventCapRegressionTest` guards the mechanism. |
| Initial capital ≈ 3–6× expenses; net flow slightly positive | Default arc shape. Hardship arcs may go far tighter (Daniyar: 0.55×, +500 ₸/mo) — then the sim harness proves survivability. |
| `financialKnowledge` starts 5–25 | The game teaches via consequences; a high start removes the arc. |
| `investmentReturnRate` is annual (`0.05`–`0.12` realistic) | Engine divides by 12; a "monthly" value yields absurd compounding. |
| Effect deltas scale to the era's money | A 50 000 hit is catastrophic in 1994 KZT, trivial in 2024. There is no shared `StoryBalance` helper — scale by hand against `initialPlayerState`. |
| No unrealistic returns (no "500%/yr", no guaranteed-profit framing) | Project restriction — scams teach skepticism; never model a get-rich scheme as winning. |

## 6. Scam-event convention (👁 review + pool-selector mechanics)

| Rule | Why |
|---|---|
| Tags include `"scam"` and a specific `"scam.{type}"` | Drives the knowledge/learned weight reductions in `EventPoolSelector` and the scam scene image. |
| Conditions include `Condition.NotFlag("learned.scam.{type}")` | A player who learned the lesson shouldn't be re-offered the identical naive trap. |
| The refuse/skeptical option sets `"learned.scam.{type}"`; a costly option may set `"lost_money_to_scam"` | Feeds the weight model (×0.15 learned, ×0.3 after real loss) and makes learning persistent. |
| Include a `schemeExplanation` for headline scams | Post-choice teaching moment without lecturing inside the scene. |
| Source schemes from `.claude/SCENARIO_REFERENCE.md` | Real KZ/CIS cases — keeps scams grounded and era-accurate. |

## 7. Content & style (👁 review)

| Rule | Why |
|---|---|
| Character-graph prose: inline Russian, 2nd person, era-specific detail | Shipped convention (all five authored graphs). The tri-lingual `Strings` layer covers UI, not scenario prose. |
| Options ≤ ~15 words, start with a verb | Chat-bubble UI; long options wrap badly. |
| Teach through consequences, never lecture in-scene | Core design principle; use `schemeExplanation` for the explicit lesson. |

---

## Quality checklist (paste into the PR / final review)

- [ ] `class XxxScenarioGraph : ScenarioGraph()`; registered in `ScenarioGraphFactory.buildGraph()`
- [ ] `characterId` unique; `eraId` lowercase & in `EraRegistry`
- [ ] `intro` present; `normal_life` present, non-terminal, top pool weight
- [ ] Arcs composed via `arc()` → `buildEvents()` / `flattenEvents()`; no duplicate ids
- [ ] Historical beats = scheduled backbone; a future beat stays pending until endgame
- [ ] All money fields use `L`; soft stats don't; `scheduleEvent` uses `ScheduledEvent(...)`
- [ ] All `next` / scheduled / pool / conditional ids resolve **within this graph**
- [ ] 5 endings via `ending()`, priority-ranked, bankruptcy at ~200 on `CAPITAL ≤ 0`, others gated on the endgame flag
- [ ] `debt_crisis` + `burnout` conditionals present, thresholds scaled to era money
- [ ] Scam events follow the `learned.scam.{type}` convention; headline scams have `schemeExplanation`
- [ ] ~12–20 pool entries; filler uses `cooldownMonths` + `maxOccurrences`
- [ ] Added to `ScenarioTestCatalog.authoredCombos`
- [ ] `python3 .claude/skills/scenario-writer/scripts/validate_scenarios.py` clean
- [ ] `./gradlew :shared:test` green (simulation + content + regressions)
