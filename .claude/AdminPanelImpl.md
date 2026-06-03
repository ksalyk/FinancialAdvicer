# Admin Panel — Implementation Spec

> Hand-off spec for an AI coding agent. Self-contained: it states the decisions, the
> relevant facts about this repo, the exact files to touch, and acceptance criteria.
> Read `CLAUDE.md` (root) first for build commands and architecture conventions.

## 0. Goal & locked decisions

Build a web admin panel for FinancialLifeV2 that can **edit Characters, Eras, and Users**, and **view Scenarios** (read-only).

Decisions already made (do not re-litigate):

- **Frontend:** Compose for Web (`wasmJs`), a new `:admin` module modeled on the existing `:landing` module.
- **Scenario scope:** **Viewer only.** Scenarios are compiled Kotlin DSL — no editing in v1. Build a read-only graph inspector.
- **Auth:** Cookie session login for the browser UI. Keep the existing static `ADMIN_KEY` Bearer token working for programmatic/API access.
- **Hosting:** Serve the compiled admin bundle from the Ktor server (same origin) so the session cookie needs no CORS-credentials handling in prod. Dev uses a webpack proxy.

## 1. Repo facts the implementation depends on

These were verified against the codebase. Trust them, but confirm if files moved.

- Modules: `:shared` (KMP game logic/models), `:composeApp` (Android+iOS client), `:server` (Ktor JVM), `:landing` (Compose `wasmJs` web). Declared in `settings.gradle.kts`. `TYPESAFE_PROJECT_ACCESSORS` is enabled (use `projects.shared`).
- Server port: `8082` (`server/.../Application.kt`, env `PORT`).
- **Admin API already exists** for Characters & Eras: `server/.../routes/AdminRoutes.kt`, mounted at `/api/v1/admin/**` in `server/.../plugins/Routing.kt`. Full CRUD + soft-delete (`/deactivate`), `/activate`, hard-delete (cascade). Auth is a manual check `ApplicationCall.isAdminAuthorized()` comparing `Authorization: Bearer <ADMIN_KEY>` to env `ADMIN_KEY` (default `dev-admin-key`).
- Admin DTOs currently live in `:server`: `CharacterRow`, `UpsertCharacterRequest` (in `CharactersRepository.kt`); `EraRow`, `UpsertEraRequest` (in `ErasRepository.kt`). They are `@Serializable`. **They must be moved to `:shared`** so the wasmJs client can use them (a wasmJs module cannot depend on the JVM `:server`).
- **Users have NO admin routes.** `UsersTable` columns: `id` (varchar36 PK), `username` (unique, lowercased), `passwordHash` (SHA-256 hex, **unsalted**), `createdAt` (epoch millis). `UserRepository`/`DatabaseUserRepository` expose find/create/verify/refresh-token ops only. No `is_active` column.
- **Scenarios are compiled code**, not data. Built via `ScenarioGraphFactory.forCharacter(characterId, eraId): ScenarioGraph` in `shared/.../scenarios/Scenarios.kt` (a `when(characterId)` dispatch). `ScenarioGraph` is an abstract class exposing `initialPlayerState: PlayerState`, `events: Map<String, GameEvent>`, `conditionalEvents: List<GameEvent>`, `eventPool: List<PoolEntry>`.
- The game models are all `@Serializable` (in `shared/.../model/`): `GameEvent`, `GameOption`, `Effect`, `PoolEntry`, `PlayerState`, `Era`. `Condition` is a `@Serializable` **sealed class** with an abstract `check(state): Boolean` — kotlinx handles sealed classes via closed polymorphism automatically; the method is ignored by serialization.
- No DB-level foreign keys. Cascades are application-level (see `CharactersRepository.deleteWithStatsCascade` for the pattern). User-owned tables to clean on delete: `refresh_tokens`, `game_states`, `game_sessions`, `completed_sessions` (confirm exact table/column names in `server/.../database/tables/`).
- CORS: `server/.../plugins/CORS.kt` — `anyHost()` when `ALLOWED_ORIGINS` unset, else restricted. Currently allows `Authorization`/`ContentType` headers; does NOT set `allowCredentials`.
- Plugins are wired in `Application.module()`: `configureSecurityHeaders/Logging/Serialization/CORS/StatusPages/Security(JWT)/Routing`. JWT auth provider is `"auth-jwt"`.
- `:landing` build (`landing/build.gradle.kts`): `wasmJs { browser { … } binaries.executable() }`, Compose deps via `compose.*` extensions, `allWarningsAsErrors=true`. Mirror this for `:admin`.

## 2. Module dependency shape

```
:admin (wasmJs, Compose M3, Ktor client JS)
   └── implementation(projects.shared)   // reuse @Serializable models + moved admin DTOs
:server
   └── (already) depends on :shared       // reference moved DTOs from :shared
```

The `:admin` SPA calls `/api/v1/admin/**` over HTTP. It never imports `:server`.

---

## 3. Work breakdown

Implement in this order. Each step is independently testable.

### Step 1 — Move admin DTOs to `:shared`

- Create `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/admin/AdminDtos.kt`.
- Move `CharacterRow`, `UpsertCharacterRequest`, `EraRow`, `UpsertEraRequest` there (keep package import updates in `:server`). Keep them `@Serializable`.
- Leave repository logic in `:server`; only the data classes move.
- Build `:shared` and `:server` to confirm no breakage.

### Step 2 — Session auth on the server

Add cookie-session auth additively; keep `ADMIN_KEY` working.

1. `server/.../plugins/AdminSession.kt`:
   - `@Serializable data class AdminSession(val username: String, val issuedAt: Long)`.
   - `install(Sessions) { cookie<AdminSession>("admin_session") { cookie.httpOnly = true; cookie.secure = <prod>; cookie.extensions["SameSite"]="Lax"; transform(SessionTransportTransformerMessageAuthentication(secret)) } }` where `secret = System.getenv("SESSION_SECRET")` (fail fast if missing in prod; allow a dev default).
   - Register a session auth provider on the existing `Authentication` install: `session<AdminSession>("admin-auth") { validate { it }; challenge { call.respond(HttpStatusCode.Unauthorized) } }`. Put this in `configureSecurity()` or a dedicated `configureAdminAuth()` called from `Application.module()` after `configureSecurity()`.
2. In `AdminRoutes.kt`, replace `isAdminAuthorized()` with a helper that returns true if **either** a valid `AdminSession` exists (`call.sessions.get<AdminSession>() != null`) **or** the `ADMIN_KEY` Bearer matches. Apply to all existing handlers (mechanical edit).
3. New `server/.../routes/AdminAuthRoutes.kt`:
   - `POST /admin/login` — body `{username, password}`; validate against env `ADMIN_USERNAME`/`ADMIN_PASSWORD` (dev defaults ok); on success `call.sessions.set(AdminSession(...))`, respond 200; else 401.
   - `POST /admin/logout` — `call.sessions.clear<AdminSession>()`, 200.
   - `GET /admin/me` — 200 `{username}` if session present, else 401. Drives the SPA auth gate.
   - Mount inside the `/api/v1` route block alongside `adminRoutes(...)` in `Routing.kt`.

### Step 3 — User admin endpoints

1. Extend `UserRepository` (+ `DatabaseUserRepository`):
   - `suspend fun listUsers(limit: Int, offset: Int, search: String?): List<AdminUserRow>`
   - `suspend fun countUsers(search: String?): Long`
   - `suspend fun updatePassword(userId: String, rawPassword: String): Boolean` (hash, then call `revokeAllTokens(userId)`)
   - `suspend fun deleteUserCascade(userId: String): Boolean` — single transaction deleting `refresh_tokens`, `game_states`, `game_sessions`, `completed_sessions` for the user, then the `users` row. Mirror `CharactersRepository.deleteWithStatsCascade`.
   - Update the in-memory fake in `AuthRoutesTest` if it implements `UserRepository`.
2. `@Serializable data class AdminUserRow(id, username, createdAt, gamesPlayed: Int)` in `:shared` `admin/AdminDtos.kt`. **Never** include `passwordHash`.
3. New `server/.../routes/AdminUserRoutes.kt` (guarded by the same admin check):
   - `GET /admin/users?limit&offset&search` → `{ items: [...], total }`
   - `GET /admin/users/{id}` → detail + their sessions/stats (use `StatisticsRepository`)
   - `POST /admin/users/{id}/reset-password` → body `{password}`
   - `DELETE /admin/users/{id}` → cascade delete
   - Mount in `Routing.kt`.

> Note: SHA-256 unsalted hashing is a known weakness (`DatabaseUserRepository.sha256Hex`). The reset-password endpoint is the natural seam to later migrate to BCrypt/Argon2. Out of scope for v1 — do not change hashing now unless asked.

### Step 4 — Scenario viewer endpoint

1. In `:shared`, add `@Serializable data class ScenarioGraphDto(val initialPlayerState: PlayerState, val events: List<GameEvent>, val conditionalEvents: List<GameEvent>, val eventPool: List<PoolEntry>)` and a mapper from `ScenarioGraph` (`events.values.toList()` etc.).
2. New `server/.../routes/AdminScenarioRoutes.kt`:
   - `GET /admin/scenarios` → list of valid `{characterId, eraId, label}` combos. Derive from `SeedData` characters × their `compatibleEraIds`; only include combos `ScenarioGraphFactory.forCharacter` actually supports (wrap in try/catch or check the `when` arms).
   - `GET /admin/scenarios/{characterId}/{eraId}` → `ScenarioGraphFactory.forCharacter(...)` → map to `ScenarioGraphDto` → respond.
   - Ensure the server's `Json` (in `plugins/Serialization.kt`) serializes the sealed `Condition`. Sealed classes are auto-polymorphic in kotlinx; if a runtime error appears, register a `SerializersModule` with `polymorphic(Condition::class){ subclass(...) }`.
   - Mount in `Routing.kt`.

### Step 5 — `:admin` Compose/wasmJs module

1. `settings.gradle.kts`: add `include(":admin")`.
2. `admin/build.gradle.kts`: copy `landing/build.gradle.kts`; add `implementation(projects.shared)`, the Ktor client (`io.ktor:ktor-client-core` + the wasmJs/js engine used elsewhere — check `gradle/libs.versions.toml` for the existing Ktor version/aliases), `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`. Add an `index.html` + entry `main()` calling `CanvasBasedWindow` (see `:landing` for the exact wasmJs entry-point idiom in this Compose version).
3. Structure under `admin/src/wasmJsMain/kotlin/kz/fearsom/financiallifev2/admin/`:
   - `Main.kt` — wasm entry point, mounts `AdminApp()`.
   - `net/AdminApiClient.kt` — Ktor `HttpClient` with `ContentNegotiation(Json)`; base URL `""` (same origin in prod) or proxy in dev; send cookies (browser does this automatically for same-origin). One suspend fn per endpoint (login/logout/me, characters CRUD, eras CRUD, users, scenarios).
   - `AdminApp.kt` — root composable. Manual back-stack navigation like `composeApp/.../AppNavigation.kt` (`AnimatedContent`). On launch, call `/admin/me`; if 401 → Login screen, else → Dashboard.
   - `screens/LoginScreen.kt`
   - `screens/CharactersScreen.kt` — list (incl. inactive), edit form, create, activate/deactivate, delete. Pure UI over the existing API.
   - `screens/ErasScreen.kt` — same shape as characters.
   - `screens/UsersScreen.kt` — paged table + search, detail view, reset-password dialog, delete with confirm.
   - `screens/ScenariosScreen.kt` — pick character+era, render `ScenarioGraphDto` as an expandable tree: event → message/flavor → options → effects + condition + nextEventId; plus initial state and event-pool weights. Read-only.
   - Per-screen state via `MutableStateFlow` in a small presenter object (Koin not required here; keep it simple).
4. Use Material 3 (`compose.material3`) for tables/forms.

### Step 6 — Hosting, CORS, build wiring

1. Static serving (prod): in `Routing.kt`, serve the built dist, e.g. `staticResources("/admin", "admin-ui")`, with a Gradle step copying `:admin` distribution into `server/src/main/resources/admin-ui`. (Confirm the wasmJs distribution task name, e.g. `:admin:wasmJsBrowserDistribution`.)
2. CORS: keep same-origin in prod (no change needed). For dev only, if not using a proxy, gate `allowCredentials = true` + `allowHost(adminDevHost, listOf("http"))` behind an env flag so prod stays locked down. Preferred dev path: webpack `devServer.proxy` forwarding `/api` → `http://localhost:8082` (configure in `admin/build.gradle.kts` `commonWebpackConfig`/`devServer`).
3. Env/secrets to document in README: `SESSION_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD` (browser login), `ADMIN_KEY` (API), `ALLOWED_ORIGINS` (prod CORS).

---

## 4. Build & run

```bash
# Server (needs Postgres)
docker compose up -d
./gradlew :server:run

# Admin dev (webpack dev server, proxy /api -> :8082)
./gradlew :admin:wasmJsBrowserDevelopmentRun

# Admin prod bundle
./gradlew :admin:wasmJsBrowserDistribution
# then served by Ktor at /admin

# Tests
./gradlew :server:test
./gradlew test
```

## 5. Acceptance criteria

- `/admin/login` with correct creds sets an httpOnly session cookie; `/admin/me` returns 200 after, 401 before; `/admin/logout` clears it.
- All existing Character/Era admin routes still work with **both** a session cookie **and** an `ADMIN_KEY` Bearer token.
- Admin UI (served at `/admin` in prod): can list/create/edit/activate/deactivate/delete Characters and Eras; list/search/paginate Users, view detail, reset password, delete; view any scenario graph as a tree. Scenario screen has no edit controls.
- Deleting a user removes all their rows in `refresh_tokens`, `game_states`, `game_sessions`, `completed_sessions` (verify in DB).
- New server route tests added following `AuthRoutesTest`/`GameRoutesTest` patterns: admin login/logout/me, user list/reset/delete, scenario fetch. `./gradlew :server:test` green.
- No `passwordHash` ever leaves the server.
- Prod runs same-origin (no `allowCredentials`/`anyHost` in prod config).

## 6. Out of scope (do not build now)

- Editing scenarios. (Future: make the engine data-driven — serialize graphs to JSON, store/override in DB, have `ScenarioGraphFactory` load data instead of the `when(characterId)` dispatch. Models are already `@Serializable`, so it's mechanical, but it's a separate project.)
- Migrating password hashing off SHA-256.
- A `users.is_active` soft-disable column (v1 uses hard delete + reset-password only).
- Role-based admin or per-action audit logging.
