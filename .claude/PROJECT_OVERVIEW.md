# FinancialLifeV2 — Project Overview

> Self-reference document for Claude. Kept in `.claude/` alongside all other internal docs.
> All new markdown files go here. README.md and CLAUDE.md stay at root (required by tooling).

---

## What is this project?

Educational mobile game built with **Kotlin Multiplatform**. Players guide a character through financial decisions set in Kazakhstan (1990s–2024). Inspired by Lifeline mechanics — chat-based, async consequences, branching storylines. The character makes choices based on player input; consequences unfold over in-game time. Players learn through failure, not lectures.

Target: Android + iOS. Backend: Ktor/PostgreSQL. Currency: KZT (tenge). Culture: Kazakh/Central Asian context.

---

## Module Structure

```
FinancialLifeV2/
├── composeApp/    # KMP client — UI, presenters, auth, network (Android + iOS)
├── shared/        # KMP shared game logic — engine, models, scenarios
├── server/        # Ktor JVM server — PostgreSQL + Exposed ORM, JWT auth
├── landing/       # Web landing page (Wasm/JS)
└── iosApp/        # iOS entry point (Xcode project)
```

Key versions: **Kotlin 2.3**, **Compose Multiplatform 1.10**, **Ktor 3.1.2**, **Koin 4.2.0-RC1**, Android minSdk 26 / targetSdk 36.

---

## Build Commands

```bash
./gradlew :composeApp:assembleDebug   # Android debug APK
./gradlew test                         # All tests
./gradlew :server:test                 # Server tests only
./gradlew :server:run                  # Start Ktor server locally
./gradlew clean                        # Clean build
```

---

## Architecture

### 3-Layer Game Engine (`shared` module)

**1. Narrative Graph** — `engine/GameEngine.kt`
- Directed event graph: `GameEvent` nodes, `GameOption` edges, `Condition` guards
- `ScenarioGraphFactory` instantiates the right graph per `(characterId, eraId)` pair
- Each `GameOption` carries: `effects: Effect`, `nextEventId: String?`, `scheduleEvent: ScheduledEvent?`

**2. Player State** — `model/Models.kt`
- `PlayerState` — 10 financial metrics:
  - `capital: Long`, `income: Long`, `expenses: Long`, `debt: Long`, `debtPaymentMonthly: Long`
  - `investments: Long`, `investmentReturnRate: Double` (annual; engine divides by 12)
  - `stress: Int` (0–100), `financialKnowledge: Int` (0–100), `riskLevel: Int` (0–100)
  - Plus: `month: Int`, `year: Int`, `characterId`, `eraId`, `currency`
  - Plus: `flags: Set<String>` (unlock/achievement tracking), `visitedEvents`, `scheduledEvents`

**3. Economic Simulation** — `GameEngine.monthlyTick()`
- Net cash flow: `monthlyFlow = income - expenses - debtPaymentMonthly`
- Debt reduction: `debt = max(0, debt - debtPaymentMonthly)`
- Investment returns: `investments * (investmentReturnRate / 12)`
- Stress dynamics based on financial health
- Triggers any scheduled consequences due this tick

### Event Priority Queue (triggered on `MONTHLY_TICK`)

1. **Era-scheduled global events** — crises keyed to real historical dates (USSR collapse Dec 1991, tenge intro Nov 1993, MMM Jun 1994, devaluation Aug 2015, COVID Mar 2020, etc.)
2. **Deferred consequences** — `ScheduledEvent` set by a prior choice, fires when `month >= targetMonth`
3. **Conditional state events** — priority-ordered; checked each tick against `PlayerState` (debt crisis >500k, burnout >75 stress, etc.)
4. **Weighted pool event** — random from pool, filtered by conditions

### Presentation Pattern (`composeApp` module)

- **No ViewModel** — presenters use Koin + `CoroutineScope` + `MutableStateFlow` (KMP-compatible)
- UI collects via `collectAsStateWithLifecycle()`
- Navigation: manual back-stack in `AppNavigation.kt` with `AnimatedContent` slide transitions
- Data flow:
  ```
  ChatScreen → GamePresenter.onChoiceSelected()
             → GameEngine.makeChoice()
               → applyEffects() + optional monthlyTick()
               → emits new GameState via StateFlow
             → UI recomposes
  ```

### Dependency Injection (Koin)

- `commonModule` in `di/AppModule.kt` — all shared dependencies
- `androidModule` in `di/AndroidModule.kt` — platform overrides (`SecureStorage` via `EncryptedSharedPreferences`)
- Circular dep `HttpClient` ↔ `AuthRepository` broken by lazy `get()`

### Authentication

- JWT (RS256) + refresh token rotation on server
- Client: `TokenStorage` (in-memory) + `SecureStorage` (platform-specific persistent)
- Cold-start: `LaunchedEffect` → `authPresenter.restoreSession()`
- Ktor Auth plugin handles refresh transparently; failure → logout callback

### Network

- Android: OkHttp engine | iOS: Darwin engine
- Base URL in `NetworkConfig`: `10.0.2.2` (Android emulator), `localhost` (iOS sim)

### Theme

- Material You dynamic colors on Android 12+; static palette fallback on older Android and iOS
- Auto light/dark from system

### Server (`server` module)

- Routes: `/auth` (login/register/refresh/logout), `/game` (session CRUD + save/load state)
- DB: PostgreSQL via Exposed ORM, tables in `server/database/tables/`
- Plugins: rate limiting, CORS, security headers, JWT validation

---

## Key Files Quick Reference

| What | Where |
|------|-------|
| Game engine core | `shared/.../engine/GameEngine.kt` |
| PlayerState, GameEvent, GameOption, Effect | `shared/.../model/Models.kt` |
| Scenario factory | `shared/.../scenarios/Scenarios.kt` |
| DSL helpers (event/option/cond/story) | `shared/.../scenarios/NarrativeDsl.kt` |
| Era definitions + global crises | `shared/.../scenarios/EraDefinition.kt` |
| Scam event library (reusable) | `shared/.../scenarios/ScamEventLibrary.kt` |
| Event pool selector | `shared/.../scenarios/EventPoolSelector.kt` |
| All scenario graphs | `shared/.../scenarios/characters/` |
| Game presenter | `composeApp/.../presentation/GamePresenter.kt` |
| Main chat UI | `composeApp/.../ui/screens/ChatScreen.kt` |
| Koin DI setup | `composeApp/.../di/AppModule.kt` |
| Navigation | `composeApp/.../ui/navigation/AppNavigation.kt` |
| Auth + JWT server | `server/.../security/` |
| Server routes | `server/.../routes/` |

---

## Implemented Characters

| Character | Era | File |
|-----------|-----|------|
| Aidar | 1990s Kazakhstan | `Aidar90sScenarioGraph.kt` |
| Aidar | Modern | `AidarScenarioGraph.kt` |
| Asan | Modern | `AsanScenarioGraph.kt` |
| Dana | Modern | `DanaScenarioGraph.kt` |
| Erbolat | Modern | `ErbolatScenarioGraph.kt` |

Character ID format: `"{name}_{era_short}"` e.g. `"aidar_90s"`, `"dana_modern"`
Era ID format: `"kz_90s"`, `"kz_2015"`, `"modern_kz_2024"`

---

## Game Mechanics Rules (for writing code or scenarios)

- All monetary values are `Long` — use `50_000L` suffix
- `investmentReturnRate` is **annual** — engine divides by 12 internally
- `monetaryReform` only for currency switches (RUB→KZT, ratio 1/500). For devaluations use `capitalDelta`
- Story events: `unique = true` (fires once per session)
- Conditional events: priority-ordered int, higher = fires sooner
- Pool events: weighted random, filtered by `Condition`
- Ending types: Bankruptcy / Paycheck-to-Paycheck / Stability / Financial Freedom / Wealth

---

## Game Endings

| Ending | Trigger |
|--------|---------|
| 💔 Bankruptcy | debt > capital × 1.5, can't cover basics |
| 😰 Paycheck-to-Paycheck | income barely covers expenses, no buffer, high stress |
| 😊 Financial Stability | 6-month emergency fund, manageable debt, medium stress |
| 🎯 Financial Freedom | passive income ≥ expenses, low debt, investing regularly |
| 🤑 Wealth | multiple income streams, significant capital, very low stress |

---

## Flag Naming Convention

```
"learned.scam.{type}"       # e.g. "learned.scam.pyramid"
"{domain}.{state}"          # e.g. "debt.crisis", "job.lost"
"chose.{option}"            # e.g. "chose.invest.stocks"
```

---

## Internal Docs Index (all in `.claude/`)

| File | Purpose |
|------|---------|
| `PROJECT_OVERVIEW.md` | This file — architecture & quick reference |
| `SCENARIO_GRAPH_GUIDE.md` | Complete manual for writing ScenarioGraph classes (667 lines) |
| `GAME_SCENARIOS_RESEARCH.md` | Development memory, top-5 MVP scenarios, event templates |
| `SCENARIO_REFERENCE.md` | Deep research on financial fraud schemes with real examples |
| `TEST_GUIDE.md` | Server test structure, how to run, add cases, mock repos |
