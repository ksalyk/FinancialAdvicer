package kz.fearsom.financiallifev2.di

import kz.fearsom.financiallifev2.auth.AuthRepository
import kz.fearsom.financiallifev2.data.SecureStorage
import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.network.NetworkConfig
import kz.fearsom.financiallifev2.network.TokenStorage
import kz.fearsom.financiallifev2.network.buildHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.Module
import org.koin.dsl.module
/**
 * Common Koin module — shared by Android and iOS.
 *
 * Dependency graph:
 *   TokenStorage ──► HttpClient (Auth plugin reads/writes tokens)
 *                └─► AuthRepository (login/register/logout update tokens)
 *
 * The circular dependency is resolved because [buildHttpClient]'s
 * [onTokenRefreshFailed] lambda resolves [AuthRepository] lazily at runtime
 * via Koin's `get()`, not at declaration time.
 *
 * Platform modules can override [NetworkConfig.baseUrl] before Koin starts:
 *   NetworkConfig.baseUrl = "http://10.0.2.2:8080/api/v1"  // Android emulator
 *   NetworkConfig.baseUrl = "http://localhost:8080/api/v1"  // iOS simulator
 */
val commonModule = module {

    // ── Token storage (in-memory; shared between HttpClient and AuthRepository) ─
    single { TokenStorage() }

    // ── HTTP client ───────────────────────────────────────────────────────────
    single {
        buildHttpClient(
            tokenStorage         = get(),
            baseUrl              = NetworkConfig.baseUrl,
            onTokenRefreshFailed = {
                // Lazily resolved at invocation time to break the circular dependency.
                get<AuthRepository>().logout()
            },
            onTokensRefreshed    = { access, refresh ->
                // Persist rotated tokens so the next cold start still works.
                get<AuthRepository>().persistTokens(access, refresh)
            }
        )
    }

    // ── Repositories ──────────────────────────────────────────────────────────
    single {
        AuthRepository(
            httpClient    = get(),
            baseUrl       = NetworkConfig.baseUrl,
            tokenStorage  = get(),
            secureStorage = get<SecureStorage>()
        )
    }

    // ── Game ──────────────────────────────────────────────────────────────────
    single { GameEngine() }
}
