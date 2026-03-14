# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Android debug build
./gradlew :composeApp:assembleDebug

# Run all tests
./gradlew test

# Run server tests only
./gradlew :server:test

# Start the Ktor server
./gradlew :server:run

# Clean build
./gradlew clean
```

## Module Structure

```
FinancialLifeV2/
├── composeApp/    # KMP client (Android + iOS) — UI, presenters, auth, network
├── shared/        # KMP shared game logic — engine, models, scenarios
├── server/        # Ktor JVM server — PostgreSQL + Exposed ORM, JWT auth
└── landing/       # Web landing page
```

Key versions: Kotlin 2.3, Compose Multiplatform 1.10, Ktor 3.1.2, Koin 4.2.0-RC1, Android minSdk 26 / targetSdk 36.

## Architecture

### 3-Layer Game Engine (in `:shared`)
1. **Narrative Graph** (`engine/GameEngine.kt`) — directed event graph with branching via `GameEvent`, `GameOption`, and `Condition`. `ScenarioGraphFactory` builds graphs per character + era combination.
2. **Player State** (`model/Models.kt`) — `PlayerState` holds 10 financial metrics (capital, income, expenses, debt, stress, knowledge, risk level, etc.) plus flags, era/character IDs, and event tracking.
3. **Economic Simulation** — `GameEngine.monthlyTick()` computes net cash flow, reduces debt principal, and updates stress dynamics.

### Event Priority Queue (triggered by `MONTHLY_TICK`)
When a player choice leads to a monthly tick, the next event is selected via:
1. Era-scheduled global event (crises keyed to specific month/year)
2. Deferred consequence (scheduled by a prior choice)
3. Conditional state event (debt crisis, burnout, unlocks based on `PlayerState`)
4. Weighted pool event (scam, career, normal life)

### Presentation Pattern (in `:composeApp`)
- No Android ViewModel — presenters use Koin + `CoroutineScope` + `MutableStateFlow` for KMP compatibility
- UI collects state via `collectAsStateWithLifecycle()`
- Navigation uses a manual back-stack (`AppNavigation.kt`) with `AnimatedContent` slide transitions

### Data Flow
```
ChatScreen → GamePresenter.onChoiceSelected()
           → GameEngine.makeChoice()
             → applyEffects() + optional monthlyTick()
             → emits new GameState via StateFlow
           → UI recomposes
```

### Dependency Injection (Koin)
- `commonModule` in `di/AppModule.kt` — all shared dependencies
- `androidModule` in `di/AndroidModule.kt` — platform overrides (e.g., `SecureStorage` via `EncryptedSharedPreferences`)
- Circular dependency between `HttpClient` and `AuthRepository` is broken by lazy `get()` resolution

### Authentication
- JWT (RS256) + refresh token rotation on the server
- Client stores tokens in `TokenStorage` (in-memory) + `SecureStorage` (platform-specific)
- Cold-start session restore via `LaunchedEffect` calling `authPresenter.restoreSession()`
- Ktor Auth plugin handles token refresh transparently; refresh failure triggers logout callback

### Network
- Platform HTTP engines: OkHttp (Android), Darwin (iOS)
- Base URL configured in `NetworkConfig` — Android emulator uses `10.0.2.2`, iOS sim uses `localhost`

### Theme
- Material You dynamic colors on Android 12+; static palette fallback for older Android and iOS
- Light/dark mode detected automatically from system

### Server
- Routes: `/auth` (login/register/refresh/logout), `/game` (session CRUD + save/load state)
- Database: PostgreSQL via Exposed ORM — tables in `server/database/tables/`
- Plugins: rate limiting, CORS, security headers, JWT validation
