# Finance LifeLine — Execution Plan for Sub-Agents

> **Purpose.** A self-contained, step-wise plan that lets a sub-agent (Sonnet, Haiku, or another Opus instance) pick up a single task in isolation and ship it. Each task has its own context block, files-to-touch, ADR notes, design-system notes, and acceptance criteria. There is no implicit shared state across tasks — anything an executing agent needs is in the task body.

**How to read this doc**
- Section 0 — orientation: what already exists in the repo (skim once, then refer back).
- Sections 1–6 — the six options. Each is broken into atomic tasks (1.1, 1.2, …).
- Each task is tagged with a **recommended model** (Haiku / Sonnet / Opus), an **effort** (S/M/L), and **dependencies** on other tasks.
- Per-task **ADR** blocks capture the “why” using the engineering:architecture skill format: *Context → Options considered → Decision → Consequences*.
- Per-task **DS** (design-system) blocks follow design:design-system: *Tokens used → New tokens needed → Component placement → Accessibility*.

**Stack reminder** (don't re-derive this in every task):
- Kotlin 2.3, Compose Multiplatform 1.10, Ktor 3.1.2, Koin 4.2.0-RC1
- Modules: `:composeApp` (Android+iOS UI), `:shared` (engine), `:server` (Ktor JVM + PG/Exposed), `:landing` (wasmJs)
- Auth: JWT RS256 + refresh-token rotation. Client stores tokens in `TokenStorage` + platform `SecureStorage`.
- Presenters use plain Koin + `MutableStateFlow` (no Android ViewModel — KMP-friendly).
- Navigation: manual back-stack in `AppNavigation.kt`, slide transitions via `AnimatedContent`.
- Theme: Material 3 + Material You dynamic colors on Android 12+. Design tokens live in `ui/theme/Tokens.kt` (Spacing, Radius, Elevation, Motion).

---

## 0. Current State Cheat-Sheet (read before any task)

Already done — **do not redo**:

Game engine (`:shared`)
- `engine/GameEngine.kt` — 3-layer FSM, 4-tier event priority queue, monthly tick simulation, monetary reform support, template substitution `{income}/{name}/…`.
- `model/Models.kt` — `PlayerState` (10 metrics + flags + pendingScheduled + eventCooldowns), `GameEvent`, `GameOption`, `Effect`, `Condition` (Stat/HasFlag/NotFlag/InEra/ForCharacter), `EndingType` enum, `MonetaryReform`, `MonthlyReport`, `ChatMessage`, `GameState`.
- `scenarios/EraDefinition.kt` — 3 registered eras (`MODERN_KZ_2024`, `KZ_90S`, `KZ_2015_DEVALUATION`) with global events + pool-weight modifiers.
- `scenarios/characters/*ScenarioGraph.kt` — 5 character graphs: Aidar, Aidar90s, Asan, Dana, Erbolat.
- `scenarios/Scenarios.kt`, `scenarios/ScamEventLibrary.kt`, `scenarios/EventPoolSelector.kt`, `scenarios/NarrativeDsl.kt`.
- Tests: `ScenarioGraphFactoryTest`, `ScenarioGraphContentTest`, `RandomEventCooldownRegressionTest`, `ScenarioNarrativeRegressionTest`, `MoneyFormatTest`.

Client (`:composeApp`)
- Presenters: `AuthPresenter`, `MainMenuPresenter`, `NewGamePresenter`, `CharactersPresenter`, `GamePresenter`, `StatisticsPresenter`.
- Screens (refs in `AppNavigation.kt`): Splash, Login, MainMenu, EraSelection, CharacterSelection, Characters, CharacterDetail, Statistics, Chat (Game).
- Theme: `Theme.kt`, `AppColors.kt`, `Tokens.kt` (Spacing/Radius/Elevation/Motion), `Shapes.kt`, `SystemTheme.kt`, `DynamicColorTheme.kt` (Android-side wired to `dynamicDarkColorScheme()` / `dynamicLightColorScheme()`).
- Network: `HttpClientFactory.kt`, `NetworkConfig.kt`, `GameApiService.kt`, `TokenStorage.kt`, platform `SecureStorage`.
- Repos: `AuthRepository.kt`, `GameSessionRepository.kt`.

Server (`:server`)
- Routes: `/auth` (login/register/refresh/logout), `/game` (start, event, choose/{optionId}, save, load, snapshots, restore/{id}, health, statistics, statistics/record), `/admin`.
- Tables: `UsersTable`, `RefreshTokensTable`, `GameSessionsTable`, `GameStatesTable`, `CompletedSessionsTable`, `CharactersTable`, `ErasTable`.
- Repos: `UserRepository`, `GameRepository`, `StatisticsRepository`, `CharactersRepository`, `ErasRepository`.
- Plugins: `Security` (JWT), `RateLimiter`, `CORS`, `SecurityHeaders`, `Serialization`, `StatusPages`, `Logging`.
- Per-user `Mutex` map serializing `/game/choose` to prevent state-write races.

Strings — currently **hardcoded Russian** literals scattered across screens, scenarios, era definitions, and `MonthlyReport.toMessage()`.

---

# Option 1 — Game Features 🎮

## 1.1 Achievements engine (data model + evaluator)

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Add a generic, declarative achievement system that runs after every `makeChoice` and after `monthlyTick`, fires once per achievement per session, and surfaces unlocked achievements as an in-engine event the UI can present.

**ADR — Why a separate evaluator (not inline in `GameEngine.makeChoice`)**
- *Context.* `GameEngine` is already 400+ lines and owns event-priority logic. Mixing achievement checks would couple two concerns.
- *Options.* (a) Inline checks in `GameEngine`. (b) Separate `AchievementEvaluator` invoked by the engine. (c) Reactive listener on `_state` flow.
- *Decision.* (b). The engine calls a pure function `AchievementEvaluator.evaluate(prev, next): Set<UnlockedAchievement>`; engine threads them into `GameState` so persisted state contains unlock history.
- *Consequences.* Pure function = trivially testable. Cost: one extra pass per choice (~µs). State payload grows by `Set<String>`.

**Files to create**
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/achievements/Achievement.kt` — data model.
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/achievements/AchievementCatalog.kt` — registry of achievements.
- `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/achievements/AchievementEvaluator.kt` — pure evaluator.
- `shared/src/commonTest/kotlin/kz/fearsom/financiallifev2/achievements/AchievementEvaluatorTest.kt`.

**Files to modify**
- `shared/.../model/Models.kt` — add `unlockedAchievements: Set<String> = emptySet()` to `PlayerState`.
- `shared/.../engine/GameEngine.kt` — call evaluator at end of `makeChoice`, append a `SYSTEM` `ChatMessage` for each new unlock.

**Data model**
```kotlin
@Serializable
data class Achievement(
    val id: String,
    val titleKey: String,           // i18n key — see Task 3.1
    val descriptionKey: String,
    val emoji: String,
    val tier: Tier = Tier.BRONZE,
    val hidden: Boolean = false,    // hide title/desc until unlocked
    val rule: AchievementRule
) {
    enum class Tier { BRONZE, SILVER, GOLD, PLATINUM }
}

@Serializable
sealed class AchievementRule {
    abstract fun matches(prev: PlayerState, next: PlayerState, ending: EndingType?): Boolean

    @Serializable data class FirstTimeStat(val field: Condition.Stat.Field, val op: Condition.Stat.Op, val value: Long): AchievementRule()
    @Serializable data class FlagSet(val flag: String): AchievementRule()
    @Serializable data class EndingReached(val ending: EndingType): AchievementRule()
    @Serializable data class CapitalGrowth(val multiplier: Int): AchievementRule()  // capital >= multiplier × initialCapital
    @Serializable data class SurvivedCrisis(val crisisFlag: String): AchievementRule()
    @Serializable data class CompositeAll(val rules: List<AchievementRule>): AchievementRule()
}
```

**Initial achievement seed (12 entries — extendable)**
- First salary received, first 1M tenge, first 10M tenge.
- First investment made, first scam survived (rejected pyramid/crypto/romance), first scam fallen for.
- Reached each `EndingType` (×5).
- "Stress survivor" — finished a game with stress < 30 in modern era.
- "Era explorer" — finished games in 3 different eras (cross-session, see Task 1.5).

**ADR — Where to store cross-session achievements (e.g. "Era explorer")**
- *Decision.* Cross-session achievements are tracked server-side via `CompletedSessionsTable` aggregation, NOT in `PlayerState`. Surface them in `StatisticsPresenter`. In-game `PlayerState.unlockedAchievements` only holds in-session achievements.
- *Consequence.* Two evaluator entry points: `AchievementEvaluator.evaluateInSession(...)` and `AchievementEvaluator.evaluateCrossSession(stats: PlayerStatistics)`.

**Acceptance criteria**
- `./gradlew :shared:test` passes including new `AchievementEvaluatorTest` with at least 8 cases.
- Re-firing the same achievement returns no new unlocks (idempotent).
- `PlayerState` round-trips through `Json` with `unlockedAchievements` preserved.
- Engine emits a `SYSTEM` `ChatMessage` per new unlock.

---

## 1.2 Achievements hub UI screen

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** 1.1

**Goal.** A new screen accessible from MainMenu showing all achievements grouped by tier. Locked entries are dimmed; hidden ones show "???" until unlocked.

**DS — design-system audit & extension**
- *Tokens used.* `Spacing.lg/xl/xxl`, `Radius.md/lg`, `Elevation.card`, color roles `surface/surfaceVariant/onSurface/primary/tertiary`.
- *New tokens needed.* `Tier.BRONZE/SILVER/GOLD/PLATINUM` colors. Add to `AppColors.kt` as `tierBronze/tierSilver/tierGold/tierPlatinum` for both light & dark schemes. Use these on the achievement card's left accent strip — not on background to keep contrast.
- *Component placement.* Reuse `Card` + `Surface` + `Icon`. Create `ui/components/AchievementCard.kt` with three states: `locked`, `unlocked`, `hiddenLocked`. One row per achievement, 56dp icon left, two-line title/desc center, tier chip right.
- *Accessibility.* Each card needs `Modifier.semantics { contentDescription = ... }`. Min touch target 48dp. Tier color accent must NOT be the only differentiator — also use icon outline + text label so colorblind users can distinguish tiers (WCAG 1.4.1).

**Files to create**
- `composeApp/.../presentation/AchievementsPresenter.kt` — pulls in-session + cross-session achievements and merges with catalog.
- `composeApp/.../ui/screens/AchievementsScreen.kt` — composable.
- `composeApp/.../ui/components/AchievementCard.kt`.

**Files to modify**
- `ui/navigation/AppNavigation.kt` — add `AppScreen.Achievements` (depth 2), wire `MainMenu` button.
- `ui/screens/MainMenuScreen.kt` — add "Achievements" entry.
- `ui/theme/AppColors.kt` — tier colors.
- `di/AppModule.kt` — register `AchievementsPresenter`.

**Acceptance criteria**
- Navigates from MainMenu → Achievements with the standard slide transition.
- Locked achievements render with `alpha = 0.4f` and a lock icon.
- Hidden+locked entries show "???" for title and description.
- Achievements sorted: unlocked first (newest at top), then locked, then hidden+locked.
- TalkBack/VoiceOver reads "Bronze, Locked, achievement description" correctly.

---

## 1.3 Difficulty scaling

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Three difficulty levels — `EASY`, `NORMAL`, `HARD` — selectable at game start. Difficulty applies multipliers to scam frequency, expense volatility, stress gains, and starting capital.

**ADR — Where the multiplier lives**
- *Context.* Multipliers must affect (1) initial state, (2) pool selection weights, (3) effect amplitudes, (4) monthly tick stress.
- *Options.* (a) Mutate `GameEvent` definitions at startup. (b) Pass a `Difficulty` config through `GameEngine` as a constructor param and apply at the integration points.
- *Decision.* (b). Add `data class DifficultyProfile(val id: Difficulty, val scamWeightMultiplier: Float, val stressMultiplier: Float, val expenseMultiplier: Float, val startingCapitalMultiplier: Float)` and pass to `GameEngine` constructor and `EventPoolSelector.selectNext`.
- *Consequence.* Existing tests stay green by defaulting to `Difficulty.NORMAL` (all multipliers = 1.0). `PlayerState` gains `difficulty: Difficulty = NORMAL` so persisted games re-load with the right profile.

**Files to create**
- `shared/.../model/Difficulty.kt` — enum + profiles.

**Files to modify**
- `shared/.../engine/GameEngine.kt` — apply `stressMultiplier` in `monthlyTick`, pass `scamWeightMultiplier` into `EventPoolSelector`.
- `shared/.../scenarios/EventPoolSelector.kt` — accept and apply the multiplier on tag `scam*`.
- `shared/.../model/Models.kt` — `PlayerState.difficulty` field.
- `composeApp/.../presentation/NewGamePresenter.kt` — capture difficulty pre-start.
- `composeApp/.../ui/screens/EraSelectionScreen.kt` (or a new sub-screen) — three difficulty cards.

**Profile values (conservative defaults — tune via playtests)**
- EASY:   scamWeight=0.6, stress=0.7, expense=0.9, startingCapital=1.5
- NORMAL: 1.0 / 1.0 / 1.0 / 1.0
- HARD:   scamWeight=1.6, stress=1.4, expense=1.2, startingCapital=0.7

**Acceptance criteria**
- New `DifficultyTest.kt` proves: starting capital scales as expected, scam pool weight changes by the right factor, monthly stress delta scales.
- Existing `RandomEventCooldownRegressionTest` still passes (NORMAL = identity).
- Difficulty round-trips in saved game JSON.

---

## 1.4 Progression mechanics — character & era unlocks

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** 1.1

**Goal.** Some characters/eras start locked. Unlock conditions: completing specific endings or unlocking specific achievements. Rendered in `CharacterSelectionScreen` and `EraSelectionScreen`.

**ADR — Server vs client truth**
- *Context.* Unlock state must survive reinstall and sync across devices.
- *Decision.* Server is source of truth. Add `UserUnlocksTable(userId, kind, id, unlockedAt)` and an endpoint `GET /game/unlocks` returning unlocked character/era IDs. Client caches in `GameSessionRepository` for offline.
- *Consequence.* New endpoints: `GET /game/unlocks`, `POST /game/unlocks/check` (server re-evaluates unlock rules from `CompletedSessionsTable` — idempotent).

**Files to create — server**
- `server/.../database/tables/UserUnlocksTable.kt`.
- `server/.../repository/UnlocksRepository.kt`.
- `server/.../routes/UnlocksRoutes.kt` — mounted under `/game/unlocks`.

**Files to create — client**
- `composeApp/.../data/UnlocksRepository.kt`.
- `composeApp/.../network/UnlocksApiService.kt`.

**Files to modify**
- `shared/.../scenarios/EraDefinition.kt` — add `requiresUnlock: UnlockRule? = null` to `EraDefinition`.
- `shared/.../scenarios/Characters.kt` (or wherever character metadata lives) — same for character entries.
- `composeApp/.../ui/screens/EraSelectionScreen.kt` and `CharacterSelectionScreen.kt` — render locked tile with overlay + tooltip "Complete X ending to unlock".

**Unlock rule shape** — reuse `AchievementRule` so the predicate language is shared, OR keep a simpler `UnlockRule` sealed class with `EndingReached` and `AchievementUnlocked` cases. Prefer the simpler one for clarity.

**Acceptance criteria**
- Locked tiles render with padlock icon + body alpha 0.5.
- Tapping a locked tile shows a Snackbar describing the unlock requirement.
- Server `POST /game/statistics/record` triggers unlock re-evaluation; subsequent `GET /game/unlocks` reflects the new unlock.
- `UnlocksRepositoryTest` (server) verifies unlock idempotency.

---

## 1.5 Scenario expansion — 25 new events across existing eras

**Recommended model:** Haiku (content authoring) → Sonnet (review & wiring) · **Effort:** L · **Dependencies:** none

**Goal.** Add 25 new `GameEvent` definitions distributed across the existing eras + characters, increasing replay variety. Each event must carry `tags`, a `poolWeight`, and at least one `Effect` with non-trivial deltas.

**Process**
1. Haiku drafts events as JSON-shaped Kotlin literals against the `GameEvent`/`GameOption`/`Effect` API. One file per theme: `BusinessOpportunityLibrary.kt`, `FamilyEventLibrary.kt`, `HealthEventLibrary.kt`, `EducationEventLibrary.kt`, `SideHustleLibrary.kt` — 5 events each.
2. Sonnet wires each library into `ScenarioGraphFactory` and into the relevant pools, ensuring no `id` collisions and adding cooldowns where needed.
3. Both pass `ScenarioGraphContentTest` plus a new `EventBalanceTest` that checks: every event's options total positive AND negative effects exist somewhere in the option set (no all-positive or all-negative events).

**Authoring rules (must follow — prevent drift from existing tone)**
- Russian language. Short, conversational, Lifeline-style.
- 2–4 options per event; at least one option must have `next = MONTHLY_TICK`.
- Use `{token}` placeholders rather than baking in numbers when referring to player money.
- Tag events from this enumerable set: `career, business, family, health, education, sidehustle, investment, scam, scam.<sub>, crisis, world, reflection, mortgage, windfall, consequence`.
- Cooldown: any pool event tagged `career/family/health` must have `cooldownMonths >= 6` to prevent spam.

**Acceptance criteria**
- All 5 new files compile.
- `ScenarioGraphContentTest` passes (no broken `next` references, no duplicate ids).
- New `EventBalanceTest` passes.
- A 10-year simulated playthrough using `GameEngine` shows at least 30% of monthly ticks pulling from the new libraries.

---

## 1.6 Random monthly micro-events (no-choice flavor)

**Recommended model:** Haiku · **Effort:** S · **Dependencies:** none

**Goal.** Inject occasional flavor messages into the chat without options — purely cosmetic context (e.g., "Тенге сегодня 458 — норм"). Doesn't affect state.

**Decision.** New `MicroEventLibrary` returning `ChatMessage` (not `GameEvent`), called from `GameEngine.monthlyTick` 15% of the time, appended after the `MonthlyReport`. Don't go through the priority queue — these are not branching events.

**Files to create**
- `shared/.../scenarios/MicroEventLibrary.kt`.

**Acceptance criteria**
- `MicroEventLibraryTest` proves the 15% probability holds within 2 standard deviations across 1000 ticks.
- Visible in the chat with `MessageSender.SYSTEM`.

---

# Option 2 — Backend Integration 🔧

> **Important pre-read.** Most of "session save/load" already exists. The work below targets gaps: cross-session persistent progression, dashboard data, partial-save throttling, and a heartbeat for active sessions.

## 2.1 Persistent progression endpoints

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** 1.4

**Goal.** Server tracks lifetime cross-session progression — currencies/credits if added later, achievement unlock history, and unlocked content (covered by 1.4). This task creates the achievement-side piece.

**ADR — One table or many?**
- *Context.* Three logical buckets: achievements, content unlocks (1.4), and future "currencies".
- *Decision.* Two tables now: `UserUnlocksTable` (1.4 covers it) and `UserAchievementsTable` (this task). Defer "currencies" until there's a concrete need.

**Files to create — server**
- `server/.../database/tables/UserAchievementsTable.kt` — columns: `userId, achievementId, unlockedAt, sessionId(nullable)`.
- `server/.../repository/AchievementsRepository.kt`.
- `server/.../routes/AchievementsRoutes.kt` — `GET /game/achievements` (list unlocked), `POST /game/achievements/sync` (client → server bulk sync at end of session).

**Files to modify — client**
- `composeApp/.../presentation/GamePresenter.kt` — when a game ends, POST `unlockedAchievements` from `PlayerState` to `/game/achievements/sync`.
- `composeApp/.../network/GameApiService.kt` — add the methods.

**Acceptance criteria**
- Sync is idempotent (PK = `userId, achievementId`).
- AuthRoutesTest-style tests for sync + list endpoints.
- Client retries on transient failure with `kotlinx.coroutines.flow.retry`.

---

## 2.2 Player statistics dashboard — extend payload

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Existing `/game/statistics` returns aggregates. Add deeper breakdowns for the dashboard screen (Task 5.1):
- Choice distribution per ending type (which top-3 events most often led to each ending).
- Average game length in months.
- Capital trajectory percentiles (p25/p50/p75) — calculated server-side from snapshots.
- Per-era win/loss ratio.

**ADR — Compute server-side or client-side?**
- *Context.* Capital trajectory percentiles need O(N) over historical sessions per user, but N is small (typically <100 games per user).
- *Decision.* Server-side. Calculated on demand inside `StatisticsRepository.getPlayerStatistics`. Cache invalidation = none (re-compute per request, it's tiny).
- *Consequence.* If user count grows, add a 60-second in-memory cache keyed by userId.

**Files to modify — server**
- `server/.../repository/StatisticsRepository.kt` — extend `getPlayerStatistics` return shape.
- `server/.../models/ServerModels.kt` — extend the response DTO.

**Files to modify — client**
- `shared/.../model/Models.kt` (or wherever `PlayerStatistics` lives) — extend.
- `composeApp/.../presentation/StatisticsPresenter.kt` — pass through to UI.

**Acceptance criteria**
- New fields in DTO: `averageGameMonths: Int`, `capitalTrajectoryP25/P50/P75: List<Long>` (12 values for 12 months, capped to game length), `choiceDistribution: Map<EndingType, List<TopChoice>>`.
- `StatisticsRepositoryTest` covers the new aggregations with seeded data.

---

## 2.3 Partial state save with debounce + ETag

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Today every choice writes the full `GameState` JSON to PostgreSQL via `/game/choose/{optionId}` (server reconstructs and saves). For large `messages` lists that grows linearly. Add:
1. Server: only persist `playerState` + `currentEventId` after each choice; persist full `GameState` snapshot only on explicit `/game/save` or every 10 monthly ticks.
2. Client: `GamePresenter.saveAndPause()` uses an ETag header to avoid double-writes.

**ADR — Why split state**
- *Context.* `messages` grows unboundedly; a 50-month playthrough may have 1500+ chat messages × ~250 chars = 375KB. Writing this per choice wastes bandwidth and PG WAL.
- *Decision.* Two columns on `GameSessionsTable`: `compactStateJson` (PlayerState + currentEventId only) written every choice; `fullStateJson` (with messages) written on save/restore boundaries. Loading: prefer fullState when available; otherwise rebuild messages from event-history (acceptable for resumes).
- *Consequence.* Resume after crash may show fewer historical messages but gameplay continuity is preserved (current event + state are intact). Document this in the loading flow.

**Files to modify — server**
- `server/.../database/tables/GameSessionsTable.kt` — add `compactStateJson` column, migration.
- `server/.../repository/GameRepository.kt` — split write paths.
- `server/.../routes/GameRoutes.kt` — `/game/choose` writes compact only; `/game/save` writes full.

**Files to modify — client**
- `composeApp/.../network/GameApiService.kt` — send `If-Match: <etag>` header.
- `composeApp/.../presentation/GamePresenter.kt` — debounce calls 250ms.

**Acceptance criteria**
- `GameRoutesTest` adds a regression: 100 choices in a row produces ≤100 KB of write to `compactStateJson`.
- Resume flow tested: kill app mid-game, relaunch, current event + stats intact.

---

## 2.4 Heartbeat & active-session metric

**Recommended model:** Haiku · **Effort:** S · **Dependencies:** none

**Goal.** Client sends a heartbeat (`POST /game/heartbeat`) every 60s while the chat screen is foregrounded so the server can compute "active players" and detect zombie sessions.

**Files to create — server**
- `server/.../routes/HeartbeatRoutes.kt`.
- Add `lastHeartbeatAt` column to `GameSessionsTable`.

**Files to modify — client**
- `composeApp/.../presentation/GamePresenter.kt` — `LaunchedEffect`-style ticker via `flow { while (true) { emit(Unit); delay(60_000) } }`.

**Acceptance criteria**
- No heartbeat when game over.
- Server endpoint rejects without auth.

---

## 2.5 Dashboard endpoint — `/game/dashboard`

**Recommended model:** Sonnet · **Effort:** S · **Dependencies:** 2.2

**Goal.** A single endpoint returning everything the new Statistics/Achievements/Settings screens need, to avoid 4 round-trips on screen open.

**Decision.** Aggregator route only — internally calls `StatisticsRepository`, `AchievementsRepository`, `UnlocksRepository`. Returns one JSON. Client calls once per screen open.

**Acceptance criteria**
- p95 server latency under 50ms for a user with 100 sessions (load-tested with `wrk`).
- `DashboardRoutesTest` covers populated and empty users.

---

# Option 3 — Content & Localization 📝

## 3.1 Multi-language string infrastructure (Russian + Kazakh + English)

**Recommended model:** Sonnet · **Effort:** L · **Dependencies:** none

**Goal.** Today all user-facing text is hardcoded Russian. Replace with a key-based string lookup that works in `:shared` (event messages) AND `:composeApp` (UI). Three target languages: `ru` (default), `kk`, `en`.

**ADR — Why not Compose's built-in `stringResource`?**
- *Context.* `:shared` is KMP and doesn't depend on Compose. Event messages live in `:shared` — they MUST be localizable from there.
- *Options.* (a) Use Compose `stringResource` only and move all event copy into `:composeApp`. (b) Use a KMP-friendly i18n library (moko-resources, libres). (c) Roll a minimal `Strings` object backed by a per-language `Map<String, String>` baked in at compile time.
- *Decision.* (c) for v1. moko-resources adds Gradle complexity not justified at current scale. We get type-safe access via a generated `Strings` object and a `Locale` picker. If/when we need pluralization or device-locale auto-pick across iOS, migrate to moko-resources — design (c) so the migration is mostly find/replace.
- *Consequence.* Each string lives in three plain-Kotlin maps. Translators get plain-text exports.

**Architecture**
```
:shared
  └── i18n/
       ├── Strings.kt            // typed accessor: Strings.ach_first_million_title
       ├── StringKeys.kt         // const val keys
       ├── LocaleSource.kt       // expect/actual platform default-locale provider
       └── translations/
            ├── ru.kt            // val ru = mapOf(...)
            ├── kk.kt
            └── en.kt
```

**Files to create**
- All under `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/i18n/`.
- Platform `actual` `LocaleSource.kt` for Android (`Locale.getDefault().language`) and iOS (`NSLocale.currentLocale.languageCode`).

**Files to modify (mass refactor — must be Sonnet+)**
- All `*ScenarioGraph.kt` — replace `message = "..."` with `message = Strings.evt_event_id_message()`. Use a code generator if you want — but a careful manual sweep is fine.
- `ChatMessage.text` for system messages in `GameEngine`.
- `AppNavigation.kt`, every `*Screen.kt` — replace string literals with `Strings.ui_*`.
- `ui/theme/Tokens.kt` — no change.

**Hand-off rule for content tasks** — when adding new events later (Task 1.5, 3.3), authors only add strings to `ru.kt` first; `kk.kt` and `en.kt` get a `// TODO i18n` and inherit `ru` until translated.

**Acceptance criteria**
- Setting `Locale.current = Locale("kk")` in a unit test changes returned strings (with a fallback to `ru` for missing keys).
- Build still green on both Android and iOS targets.
- A grep for hardcoded Cyrillic literals in `:composeApp` and `:shared` returns zero hits outside the `translations/` package.

---

## 3.2 Settings screen — language picker (also wired in 5.2)

**Recommended model:** Haiku · **Effort:** S · **Dependencies:** 3.1

**Goal.** A simple list of languages with a single-select. Persisted in `SecureStorage` so it survives reinstall on iOS too.

**DS notes**
- Use `RadioButton` rows. Avoid a dropdown — the app's pattern is full-row tap targets.
- Each row 56dp tall (≥48dp touch target).

**Acceptance criteria**
- Choosing "English" immediately re-renders the screen in English (verified by snapshot of Strings).
- Persisted across cold start.

---

## 3.3 New character — "Aigerim" (modern KZ, single mom, 35)

**Recommended model:** Haiku (content) → Sonnet (wiring) · **Effort:** L · **Dependencies:** 3.1 (so all new copy lands in `i18n/translations/ru.kt`)

**Goal.** Add a new playable character with her own scenario graph (~25 events) showcasing single-parent finance: childcare costs, school fees, side income, government support.

**Files to create**
- `shared/.../scenarios/characters/AigerimScenarioGraph.kt`.
- `shared/.../i18n/translations/ru.kt` — append all event keys.
- (Optional) Era variants if she should appear in `KZ_2015_DEVALUATION`.

**Files to modify**
- `shared/.../scenarios/ScenarioGraphFactory.kt` — register Aigerim.
- `server/.../routes/GameRoutes.kt` — add `aigerim` to `VALID_CHARACTER_IDS`.
- `composeApp/.../ui/screens/CharacterSelectionScreen.kt` and `CharactersScreen.kt` — auto-pick from registry, but verify her tile renders.

**Acceptance criteria**
- New `ScenarioGraphContentTest` case for `aigerim` passes.
- 10-year simulated playthrough returns at least one valid ending.

---

## 3.4 New era — "KZ 2030 (post-AI economy)"

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** 3.1

**Goal.** A speculative near-future era to broaden replay. Theme: AI-driven job displacement, deepfake scams, new investment vehicles.

**Files to modify**
- `shared/.../scenarios/EraDefinition.kt` — add `KZ_2030` to `EraRegistry.all`.
- `shared/.../scenarios/EraDefinition.kt` `EraEventLibrary` — 4 new global events: ai_layoff_wave (2031/3), deepfake_voice_scam_wave (2032/6), tokenized_real_estate (2033/9), basic_income_pilot (2034/1).
- `composeApp/.../ui/screens/EraSelectionScreen.kt` — verify the new tile shows (driven by registry).

**Acceptance criteria**
- Build green; era selectable.
- New era covered by `ScenarioGraphContentTest`.

---

## 3.5 Extend game duration — soft cap removal & late-game content

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Today endings can be reached early, ending the game. Add late-game content (year 5+) for players approaching `FINANCIAL_FREEDOM` so the run keeps going meaningfully.

**Investigation step (must do first)** — read `:shared/scenarios/Scenarios.kt` and the character graphs to see how `isEnding` is gated. Grep for `endingType =`.

**ADR — How to extend without invalidating saved games**
- *Decision.* Add an `extended_play` flag on `PlayerState` set when player chooses "Continue" at the first ending screen. Adds a "post-victory" event pool that emphasizes wealth-management dilemmas (philanthropy, business expansion, market crashes affecting a large portfolio).
- *Consequence.* `EndingType` still computed for stats, but game does not terminate. Old saves don't have the flag — they end normally.

**Acceptance criteria**
- Reaching `FINANCIAL_FREEDOM` shows ending screen with "Continue playing" button.
- Continuing yields events tagged `late_game` only.
- 5-year simulated post-victory playthrough produces at least 30 events.

---

# Option 4 — Testing & Polish 🧪

## 4.1 Engine fuzz tests

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Property-test the engine — random sequences of choices over a fixed seed must never throw, never produce negative monetary values (already coerced, but verify), never lose `pendingScheduled` entries, and `eventCooldowns` must monotonically advance.

**Files to create**
- `shared/.../engine/GameEngineFuzzTest.kt`.

**Approach.** No external lib — write a deterministic random walker: for each step pick the first option, log, advance. Run 5000 iterations against each character. Assert invariants after every step.

**Invariants to encode**
- `playerState.capital >= 0`
- `playerState.income >= 0`
- `playerState.stress in 0..100`
- `currentEventId` resolves in graph (or game is over)
- `pendingScheduled.fireAtYear * 12 + fireAtMonth >= playerState.absoluteMonth`
- `unlockedAchievements ⊆ AchievementCatalog.all.ids` (after Task 1.1)

**Acceptance criteria**
- 5000-iteration fuzz on all 5 characters in <30s on CI (the engine is pure code; this is achievable).

---

## 4.2 UI snapshot tests for Chat screen — Paparazzi (Android only) or Roborazzi

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Lock down the chat screen's rendering for the four main message kinds (`CHARACTER`, `PLAYER`, `SYSTEM`, `MONTHLY_REPORT`), light & dark themes.

**ADR — Paparazzi vs Roborazzi**
- *Context.* Compose Multiplatform — Paparazzi works on Android JVM tests. Roborazzi is more KMP-friendly but adds dependency complexity.
- *Decision.* Roborazzi for KMP commonTest; if too disruptive, fall back to Paparazzi as Android-only smoke tests of the Compose UI.
- *Consequence.* New dev dep, ~15MB of golden PNGs in repo.

**Acceptance criteria**
- 8 golden PNGs (4 message types × 2 themes) committed.
- CI gate: `./gradlew :composeApp:verifyRoborazziDebug` (or equivalent).

---

## 4.3 Performance audit — chat list with 1000+ messages

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** A long playthrough accumulates 500+ messages. Verify `ChatScreen` stays at 60fps and memory stays bounded.

**Investigation steps**
1. Read `ChatScreen.kt` (you'll need to grep — it's in `ui/screens/`).
2. Confirm `LazyColumn` is used and `ChatMessage.id` is the key.
3. Profile with Layout Inspector / Android Studio CPU profiler at 1000-message length.

**Likely fixes (apply only if profile says so)**
- `derivedStateOf` for any computed message-list summary.
- Move `MonthlyReport.toMessage()` formatting onto an IO dispatcher — it does string building per item.
- Use `key = { it.id }` on `LazyColumn.items`.

**Acceptance criteria**
- Frame time < 16ms on a Pixel 6 emulator scrolling a 1000-message list (measured with Macrobenchmark or manually with the profiler).
- No memory growth between scroll-to-top and scroll-to-bottom (a leak indicator).

---

## 4.4 Accessibility deep-dive (WCAG 2.1 AA)

**Recommended model:** Sonnet · **Effort:** L · **Dependencies:** none

**Goal.** Pass an accessibility audit on all screens. Cover: contrast ratios, content descriptions, touch targets, keyboard navigation (DPad on Android TV style), screen reader flow, dynamic type / `fontScale` up to 1.3.

**DS — what changes (apply to `:composeApp/.../ui`)**
- All `IconButton` and tap-able icons need `contentDescription`.
- `ChatScreen` choice buttons must have `Modifier.semantics { contentDescription = "${option.emoji} ${option.text}" }`.
- Confirm light theme color contrast: `TextSecondaryLight on BackgroundLightCard` likely fails 4.5:1 — verify and fix in `AppColors.kt` if so.
- Money values rendered as `+125 000 ₸` should use `liveRegion = LiveRegionMode.Polite` so changes announce on TalkBack.
- Min touch target 48×48dp on every button — audit `ChoiceButton` and back-arrow.
- Support `LocalDensity` `fontScale = 1.3f`. Test by setting OS font size to "Largest". The chat bubbles must reflow without truncation.

**Files to touch (broad sweep)**
- All composables under `composeApp/.../ui/screens/` and `ui/components/`.
- `composeApp/.../ui/theme/AppColors.kt` if contrast fails.

**Acceptance criteria**
- Espresso accessibility-test scanner reports zero issues at level Error.
- Manual VoiceOver pass on iOS sim — full flow (login → start game → make 3 choices → return to menu) navigable by swipe.

---

## 4.5 Edge case sweep — saved-game compatibility & reform interactions

**Recommended model:** Sonnet · **Effort:** S · **Dependencies:** none

**Goal.** Cover three known sharp edges:
1. Loading a save serialized before fields like `unlockedAchievements` (1.1) or `difficulty` (1.3) were added — `Json { ignoreUnknownKeys = true; encodeDefaults = true }` already handles this in server, verify on client.
2. Monetary reform (RUB → KZT 500:1) followed by a deferred event scheduled before the reform — verify amounts stored in the deferred `Effect` are NOT auto-repriced (they are pre-baked deltas).
3. Scheduled event firing in the same month as an era global event — priority order says era > scheduled; confirm with a test.

**Files to create**
- `shared/.../engine/SaveCompatibilityTest.kt`.
- `shared/.../engine/EngineEdgeCaseTest.kt`.

**Acceptance criteria**
- Each edge case has at least one failing-then-passing test commit.

---

# Option 5 — Additional Screens 🖥️

## 5.1 Statistics dashboard — UI

**Recommended model:** Sonnet · **Effort:** L · **Dependencies:** 2.2 (new fields), 2.5 (consolidated dashboard endpoint)

**Goal.** A rich, scrollable dashboard. Sections in order:
1. Header — total games, completion ratio, best ending tier.
2. Capital trajectory chart (p25/p50/p75 across game months).
3. Ending distribution donut.
4. Per-character win/loss bar group.
5. Per-era breakdown.
6. Recent sessions list (last 10).

**DS audit & extensions**
- *Tokens.* Re-use `Spacing.lg/xl`, `Radius.lg`, `Elevation.card`. Tier colors from 1.2.
- *New components.* Build inline; do NOT pull in MPAndroidChart or similar — it's not KMP. Use Compose Canvas:
  - `ui/components/charts/SparklineChart.kt` — simple line + shaded area for percentile band.
  - `ui/components/charts/DonutChart.kt`.
  - `ui/components/charts/BarGroupChart.kt`.
- *Layout.* `LazyColumn` so very tall content scrolls smoothly. Each section is a `Card` with `Spacing.lg` internal padding.
- *Accessibility.* Each chart's `Modifier.semantics` must contain a text description ("p50 capital trajectory: starts 200K, ends 1.5M over 24 months"). Use `liveRegion = Polite` for refreshed numbers.
- *Empty state.* When `hasAnyGames == false`, render a centered illustration + CTA "Start your first game".

**Files to create**
- `composeApp/.../ui/screens/StatisticsScreen.kt` (replace/expand existing).
- `composeApp/.../ui/components/charts/*.kt`.

**Acceptance criteria**
- 4-section layout renders at fontScale 1.0 and 1.3.
- Charts render correctly with 0, 1, 5, 50 sessions.
- TalkBack reads each chart's summary description.

---

## 5.2 Settings & preferences screen

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** 3.2 (language)

**Goal.** A settings hub with grouped sections:
- Account: email, change password, logout, delete account.
- Gameplay: difficulty default, sound effects on/off, haptic feedback on/off.
- Display: theme override (Auto / Light / Dark), language picker.
- Data: export saves, sync now, clear local cache.
- About: version, privacy policy link, terms.

**ADR — Persistence**
- *Decision.* Settings live in `SecureStorage` (already platform-impl'd). Account-bound settings (e.g., language preference) also synced server-side via a small `/user/preferences` endpoint, defer to the next release.
- *Consequence.* Local-first; offline-friendly.

**DS audit**
- *New components.* `ui/components/SettingsRow.kt` (icon + title + subtitle + trailing widget). `ui/components/SettingsSection.kt` (section header + card group).
- *Tokens.* Reuse Spacing.lg, Radius.md, Elevation.card.
- *Toggles.* Material `Switch`. Min 48dp row.
- *Destructive actions* (logout, delete) — use `error` color on text, no destructive default in primary buttons.

**Files to create**
- `composeApp/.../ui/screens/SettingsScreen.kt`.
- `composeApp/.../ui/components/SettingsRow.kt` and `SettingsSection.kt`.
- `composeApp/.../presentation/SettingsPresenter.kt`.

**Acceptance criteria**
- Theme override flips immediately (no restart) — wire via `MutableStateFlow<ThemeOverride>` in `SettingsPresenter` collected in `App.kt`.
- Logout calls `authPresenter.logout()` and `gamePresenter.saveAndPause()`.

---

## 5.3 Onboarding tutorial overlay (one-time, after first login)

**Recommended model:** Haiku · **Effort:** S · **Dependencies:** none

**Goal.** A 4-step tooltip tour the first time the user reaches MainMenu after registration: "Continue", "New Game", "Statistics", "Achievements".

**Decision.** Pure-Compose overlay; persist `tutorial_completed_v1` flag in `SecureStorage`.

**Files to create**
- `composeApp/.../ui/components/Tooltip.kt`.
- `composeApp/.../ui/components/OnboardingOverlay.kt`.

**Acceptance criteria**
- Shown exactly once; "Skip" works; "Reset tutorial" available in Settings (5.2).

---

# Option 6 — Analytics 📊

## 6.1 Analytics abstraction & event taxonomy

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** none

**Goal.** Vendor-neutral interface so we can swap providers (Firebase Analytics, PostHog, Amplitude, Mixpanel) without touching feature code.

**ADR — Why an interface?**
- *Decision.* `interface AnalyticsClient { fun track(event: AnalyticsEvent); fun identify(userId: String, traits: Map<String,Any>); fun setEnabled(b: Boolean) }`. Implementations: `NoopAnalyticsClient` (default in tests), `FirebaseAnalyticsClient` (Android only via `expect/actual`), `IosAnalyticsClient` (TBD). Server-side: an HTTP fan-out for events that must hit a backend collector.
- *Consequence.* Feature code calls `Analytics.track(GameStarted(characterId, eraId, difficulty))`; the implementation is wired in `AppModule.kt`.

**Event taxonomy (canonical names — do not deviate)**
- `app_open`, `app_background`
- `auth_login_attempted`, `auth_login_succeeded`, `auth_login_failed{reason}`
- `auth_register_succeeded`, `auth_register_failed{reason}`
- `game_started{characterId, eraId, difficulty}`
- `game_choice_made{eventId, optionId, currentMonth, capitalBucket}` — bucket capital to avoid PII-shaped data: `<0`, `0-100K`, `100K-1M`, `1M-10M`, `>10M`
- `game_ended{ending, totalMonths, capitalBucket}`
- `achievement_unlocked{achievementId, tier}`
- `screen_view{screen}`
- `error_occurred{domain, code}` — non-fatal client errors

**Files to create**
- `shared/.../analytics/AnalyticsClient.kt` — interface + sealed `AnalyticsEvent` hierarchy.
- `shared/.../analytics/NoopAnalyticsClient.kt`.
- `composeApp/.../analytics/PlatformAnalytics.kt` (expect/actual).
- `composeApp/src/androidMain/.../analytics/PlatformAnalytics.android.kt`.
- `composeApp/src/iosMain/.../analytics/PlatformAnalytics.ios.kt`.

**Files to modify**
- `composeApp/.../di/AppModule.kt`, `AndroidModule.kt`, `IosModule.kt` — bind.
- All presenters — call `analytics.track(...)` at the boundaries (login success, choice made, ending reached).

**Acceptance criteria**
- Default build compiles with `NoopAnalyticsClient` — no vendor SDK required for OSS.
- `AnalyticsContract`-style test: a fake implementation captures events; assert each user flow emits the expected sequence.

---

## 6.2 Server-side event sink (`/analytics/events`)

**Recommended model:** Sonnet · **Effort:** M · **Dependencies:** 6.1

**Goal.** A simple POST endpoint that accepts batched events and writes them to a `RawAnalyticsEventsTable`. Aggregation later. Lets us own first-party data without an external vendor lock-in.

**Files to create — server**
- `server/.../database/tables/RawAnalyticsEventsTable.kt` — `id, userId(nullable), eventName, payloadJsonb, occurredAt, receivedAt`.
- `server/.../routes/AnalyticsRoutes.kt`.
- `server/.../repository/AnalyticsRepository.kt`.

**ADR — JSONB payload vs strict schema?**
- *Decision.* JSONB. Schema is event-name dependent and we want to evolve the taxonomy without migrations.
- *Consequence.* Aggregation queries use PG JSON operators. Add a partial index on `eventName` for cheap filters.

**Acceptance criteria**
- `POST /analytics/events` with a 100-event batch persists in <50ms p95.
- Rejects payloads >256KB (rate-limit, body-limit middleware).

---

## 6.3 Analytics opt-in/opt-out & GDPR plumbing

**Recommended model:** Sonnet · **Effort:** S · **Dependencies:** 6.1, 5.2

**Goal.** Surface analytics consent in Settings (5.2). Default: enabled with a clear note. User can opt out — calls `analyticsClient.setEnabled(false)` and POSTs `/user/analytics/erase` to delete server-side history.

**Files to modify — server**
- `server/.../routes/UserRoutes.kt` (or new `PrivacyRoutes.kt`) — `POST /user/analytics/erase`.

**Files to modify — client**
- `composeApp/.../ui/screens/SettingsScreen.kt` — toggle + privacy text.
- `composeApp/.../presentation/SettingsPresenter.kt` — wire toggle.

**Acceptance criteria**
- Opting out within 1s stops emitting events client-side.
- `/user/analytics/erase` deletes rows with matching `userId` and returns 204.

---

## 6.4 Funnel-ready event timing — span around critical actions

**Recommended model:** Haiku · **Effort:** S · **Dependencies:** 6.1

**Goal.** Wrap "make choice" with `track("game_choice_processing_started")` and `track("game_choice_processing_completed", durationMs)` so backend/perf regressions are visible.

**Acceptance criteria**
- Two events emitted around every `GamePresenter.onChoiceSelected`.
- Durations include network round-trip if server-write is part of the flow.

---

## 6.5 Crash & non-fatal error reporting

**Recommended model:** Sonnet · **Effort:** S · **Dependencies:** 6.1

**Goal.** Pipe crashes to a vendor (Crashlytics / Sentry) — abstracted behind an `ErrorReporter` interface analogous to `AnalyticsClient`.

**Files to create**
- `shared/.../analytics/ErrorReporter.kt` (interface + Noop default).
- Wire in `App.kt` `LaunchedEffect`-level try/catch and in HttpClient `expectSuccess` failure.

**Acceptance criteria**
- Default build still has no vendor dep.
- A thrown exception in `GamePresenter.onChoiceSelected` is captured exactly once.

---

# Cross-Cutting — Conventions & Hand-off Rules

These apply to every task. They live here so each task body stays short.

**Code style**
- Top of every new file: `package kz.fearsom.financiallifev2.<area>` then a short KDoc.
- 4-space indent. Trailing-comma where natural.
- Public APIs have KDoc; internal ones get a one-line comment when non-obvious.
- Prefer `data class` + `sealed class/interface` over inheritance.

**Tests**
- Pure logic → `:shared` `commonTest`.
- Server logic → `server/test/.../routes` (Ktor `testApplication`).
- UI (when added) → `composeApp` JVM unit tests via Compose UI test or Roborazzi.
- Always add at least one regression test per behavioral change.

**i18n**
- Once Task 3.1 lands, all NEW player-facing strings go through `Strings.*`. Russian is the source of truth; Kazakh and English get `// TODO` placeholders if no translation yet.

**Server ↔ client contract**
- DTOs duplicated between `server/models/ServerModels.kt` and `shared/model/Models.kt` MUST stay in sync. Any change touches both files in the same PR.
- Server uses `Json { ignoreUnknownKeys = true; encodeDefaults = true }`. Don't change.
- All authenticated routes assume `principal<JWTPrincipal>()!!` — they're inside `authenticate("auth-jwt") {}`.

**Security**
- Never log JWT or refresh tokens.
- Never accept `userId` from the request body for protected operations — always extract from JWT.
- Body size limits — server already has a global limit; new routes can override per-route if a payload is justifiably larger (analytics batch only).

**Performance budget**
- Cold start to MainMenu: <1.5s on Pixel 6.
- 60fps in chat scroll up to 1000 messages.
- Per-choice round trip: <300ms p95.

**Migration discipline**
- Every PG schema change adds a migration script under `server/migrations/V<N>__<name>.sql`. Don't auto-create tables in production via Exposed `SchemaUtils.create` — that path is only for local dev.

---

# Suggested Execution Order

If we want fastest perceived progress with lowest risk:
1. **3.1 i18n infrastructure** — unlocks everything else without a string-literal rebase later.
2. **1.1 + 1.2** — achievements engine + UI together.
3. **2.2 + 2.5** — extended stats + dashboard endpoint.
4. **5.1** — Statistics screen, now backed by 2.2.
5. **1.3 difficulty** — small, satisfying, no dependencies.
6. **5.2 settings** — also surfaces 1.3 difficulty default + 3.2 language.
7. **1.4 unlocks** — needs 1.1 in place.
8. **1.5 + 3.3 + 3.4** — content expansion in parallel.
9. **2.3 + 2.4 + 6.x** — perf & analytics polish.
10. **4.1–4.5** — testing & a11y once feature surface is stable.

If the user only wants one shippable thing right now, start with **Option 5.1 (Statistics dashboard UI)** — depends only on the backend that's already wired (with 2.2 if richer data is desired) and gives the most visible win for someone reopening the app.

---

# Codex Review Notes — 2026-05-02

This plan is a useful feature backlog, but parts of it are stale against the current repository. Before handing tasks to sub-agents, update the task scopes below so agents do not duplicate existing work or build endpoints that cannot satisfy their acceptance criteria.

## Corrections Needed

1. **Task 3.1 is mostly already done.**
   The repo already has shared KMP i18n under `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/i18n/` with `Strings.kt`, `StringKeys.kt`, and `ru/kk/en` maps. Scenario text is already keyed in many graph files.

   Replace Task 3.1 with: **Reactive language switching**.
   - Current `Strings.currentLocale` is a mutable singleton and comments say language changes apply on app restart.
   - Add a `LocalePresenter`/settings-backed `StateFlow`.
   - Expose locale through Compose state or a `CompositionLocal`.
   - Keep shared engine string lookup compatible with non-Compose code.

2. **Difficulty already exists as metadata.**
   `Difficulty { EASY, MEDIUM, HARD, NIGHTMARE }` exists in `shared/model/GameModels.kt` and is used for character/bundle display difficulty. It is not gameplay scaling.

   Do not overload it silently. Prefer:
   - `GameDifficulty` for player-selected gameplay mode, or
   - `DifficultyProfile` stored in `PlayerState`.

   If reusing the existing enum, explicitly migrate all UI labels and semantics so “character difficulty” and “engine difficulty” do not diverge.

3. **Achievements should not be surfaced only through chat messages.**
   `PlayerState.unlockedAchievements` is fine, but chat-only unlock delivery is fragile.

   Add one of:
   - `GameState.newlyUnlockedAchievements: List<String>`, cleared by presenter acknowledgement, or
   - presenter-level one-shot UI events derived from engine results.

   Chat messages can remain as a secondary presentation, not the source of truth.

4. **Task 2.2 asks for data the server does not currently store.**
   `CompletedSessionsTable` stores final session values only. It cannot compute:
   - capital trajectory percentiles,
   - top choices that led to endings,
   - per-month trends.

   Add a prerequisite data decision:
   - persist monthly snapshots,
   - persist choice/event history,
   - or reduce dashboard scope to final-session aggregates only.

5. **Task 2.3 compact saves need an event log or explicit history loss.**
   Current `GameSessionsTable` stores full `stateJson`. If this is split into compact state + full snapshots, “rebuild messages from event-history” is impossible unless event history is persisted.

   Correct options:
   - add `GameEventLogTable(sessionId, sequence, eventId, optionId, messageJson, createdAt)`, or
   - keep periodic full snapshots and explicitly accept truncated chat history after crash restore.

6. **Unlocks are partially implemented locally.**
   `UnlockCondition`, `isUnlocked`, difficulty badges, and locked UI hints already exist in shared/client models.

   Rewrite Task 1.4 as: **server-backed unlock persistence + rule evaluator + client merge**.
   Avoid rebuilding local locked-tile UI from scratch.

7. **Roborazzi/Paparazzi scope should be Android/JVM, not `commonTest`.**
   Roborazzi and Paparazzi are not pure KMP common tests. Put snapshot tests under Android/JVM test source sets and keep golden count small.

8. **Settings should not all use `SecureStorage`.**
   Keep tokens/secrets in `SecureStorage`. Theme, language, sound, haptics, and tutorial flags should use normal preferences/settings storage unless there is no existing multiplatform settings abstraction.

9. **Analytics default should be opt-in/out per product policy before implementation.**
   The plan says default enabled. Decide product/legal stance first. At minimum, no PII-shaped payloads, no raw money values, no tokens, no free-form user text.

10. **Build verification prerequisite is missing.**
    In the current worktree, `./gradlew :shared:test` fails before compilation because Android SDK location is not configured:
    - missing `ANDROID_HOME`, or
    - missing `local.properties` with `sdk.dir=...`.

    Fix this before assigning implementation tasks; otherwise agents cannot verify changes.

## Revised Execution Order

Recommended order for lowest rework:

1. **Build health** — configure SDK path and run `./gradlew :shared:test`.
2. **Reactive i18n/settings foundation** — replace stale 3.1 with runtime language switching.
3. **Achievements engine minimal** — shared model/evaluator + engine output, no server yet.
4. **Achievements UI** — consume catalog + unlock state.
5. **Server achievements/unlocks persistence** — durable cross-device progress.
6. **Statistics data-model decision** — snapshots/event log/final aggregates only.
7. **Dashboard endpoint + UI** — only after the data shape is real.
8. **Gameplay difficulty profile** — separate from character display difficulty.
9. **Content expansion** — new events/character/era after i18n path is stable.
10. **Analytics/performance/accessibility** — after feature surface stabilizes.

If only one visible feature is needed now, do **Achievements engine + Achievements hub** first. It has a clear product payoff and does not require solving statistics storage or dashboard charting.

*End of plan. Each task is intentionally self-contained — an agent reading only Section 0 and a single Section X.Y should be able to execute it. Update this file after each task completes (mark done with `✅` next to the task heading).*
