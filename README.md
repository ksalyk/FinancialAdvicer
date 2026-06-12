# Finance LifeLine (FinancialLifeV2)

Interactive chat-based financial literacy game (Lifeline-style) for Kazakhstan / Central Asia.
Kotlin Multiplatform: Android + iOS clients, Ktor server, Wasm landing page and admin panel.

**Stack:** Kotlin 2.3.20 · Compose Multiplatform 1.10.3 · Ktor 3.4.2 · Koin 4.2.1 · Exposed + PostgreSQL · Android minSdk 26 / targetSdk 36

## Modules

| Module | Target | Purpose |
|---|---|---|
| `:shared` | KMP (Android/iOS/JVM/Wasm) | Game engine, models, scenarios, i18n strings, DTOs |
| `:composeApp` | Android + iOS | Client app — UI, presenters, auth, network |
| `:server` | JVM | Ktor API server — PostgreSQL/Exposed, JWT auth, admin API, serves admin SPA |
| `:landing` | Wasm (browser) | Web landing page |
| `:admin` | Wasm (browser) | Admin SPA — login, manage Characters/Eras/Users, view Scenario graphs |

## Quick Start

### 1. Server (+ PostgreSQL)

```bash
docker compose up -d        # PostgreSQL 16 on localhost:5432 (db: financelifeline, user/pass: postgres/postgres)
./gradlew :server:run       # → http://localhost:8082
```

`:server:run` automatically builds the `:admin` Wasm bundle and copies it into server resources (`copyAdminUi` task), so the admin panel is included.

On startup the server runs versioned schema migrations (`MigrationRunner`), validates data, and seeds predefined characters/eras (idempotent upsert).

Sanity check: `curl http://localhost:8082/` → `{"name":"Finance LifeLine API",...}`.

```bash
docker compose down         # stop Postgres
docker compose down -v      # stop + wipe DB volume
```

### 2. Admin panel

Served by the server — no separate process needed:

```
http://localhost:8082/admin
```

Default dev credentials: `admin` / `dev-admin-password` (override via `ADMIN_USERNAME` / `ADMIN_PASSWORD`).

After changing `:admin` code, rebuild the bundle and restart:

```bash
./gradlew :admin:wasmJsBrowserDistribution :server:run
```

Note: the SPA derives its API base URL from `window.location.origin`, so running `:admin:wasmJsBrowserDevelopmentRun` standalone will point API calls at the webpack dev server and fail. Always run it through the server.

### 3. Landing page

Standalone, no backend dependency:

```bash
./gradlew :landing:wasmJsBrowserDevelopmentRun   # dev server with hot reload (usually http://localhost:8080)
./gradlew :landing:wasmJsBrowserDistribution     # prod bundle → landing/build/dist/wasmJs/productionExecutable
```

### 4. Android app

```bash
./gradlew :composeApp:assembleDebug
```

Or run from Android Studio. The emulator reaches the local server via `10.0.2.2:8082` (configured in `NetworkConfig`).

### 5. iOS app

Open `/iosApp` in Xcode and run. iOS simulator reaches the server via `localhost:8082`.

### 6. Tests

```bash
./gradlew test              # all modules
./gradlew :server:test      # server only (uses H2 in-memory, no Docker needed)
```

## Server Configuration (env vars)

| Variable | Default | Notes |
|---|---|---|
| `PORT` | `8082` | HTTP port |
| `DATABASE_URL` | — | `postgres://user:pass@host:5432/db` (Heroku/Railway style) or full JDBC URL; takes precedence |
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `5432` / `financelifeline` | Used when `DATABASE_URL` is not set |
| `DB_USER` / `DB_PASSWORD` | `postgres` / `postgres` | |
| `JWT_SECRET` | dev fallback | **Set in production** |
| `ADMIN_KEY` | `dev-admin-key` | Bearer key for admin API |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `dev-admin-password` | Admin panel login |
| `SESSION_SECRET` | dev fallback | Admin session cookie signing |
| `SESSION_SECURE` | — | Set `true` behind HTTPS |
| `ALLOWED_ORIGINS` | — | CORS origins (comma-separated) |
| `SKIP_MIGRATIONS` | `false` | Skip schema migrations (dev/CI only) |

## API Routes

```
GET  /                      → API status
GET  /admin                 → Admin SPA (static)
/api/v1/auth/*              → register, login, refresh, logout, me (JWT + refresh token rotation)
/api/v1/game/*              → session CRUD, save/load state (requires access token)
/api/v1/admin/*             → login/logout/me (session), characters, eras, users, scenario graphs
```

## Architecture Highlights

- **3-layer game engine** (`:shared`): narrative event graph (`GameEngine`, `ScenarioGraphFactory`) → `PlayerState` with 10 financial metrics → monthly economic simulation (`monthlyTick()`).
- **Event priority queue** on each monthly tick: era-scheduled crises → deferred consequences → conditional state events (debt crisis, burnout) → weighted random pool.
- **No Android ViewModel** — KMP-friendly presenters (Koin + `CoroutineScope` + `MutableStateFlow`), manual back-stack navigation with `AnimatedContent` transitions.
- **Auth:** JWT + refresh rotation; tokens in `TokenStorage` (memory) + `SecureStorage` (Android Keystore AES-256-GCM / iOS Keychain); transparent refresh via Ktor Auth plugin; cold-start session restore.
- **DB migrations:** versioned, applied on startup (`server/database/migrations/versions/`), followed by data validation.
- **i18n:** `Strings` singleton keyed by `StringKeys`, reactive locale switching persisted to `SecureStorage`.
- **Feature flags:** `FeatureFlagRepository` with local `SecureStorage` backend, designed for remote-config swap.
- **Theme:** Material You dynamic colors (Android 12+) with static fallback; auto light/dark.

## Documentation

Full docs live in `.claude/`:

- `PROJECT_OVERVIEW.md` — architecture, character/era roster, design tokens
- `AdminPanelImpl.md` — admin panel spec
- `EXECUTION_PLAN_DETAILED.md` — task backlog (read Codex review notes at the bottom first)
- `SCENARIO_GRAPH_GUIDE.md` / `SCENARIO_REFERENCE.md` — scenario authoring
- `TEST_GUIDE.md` — testing guide
