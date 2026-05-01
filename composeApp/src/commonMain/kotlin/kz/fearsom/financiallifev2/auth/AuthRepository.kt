package kz.fearsom.financiallifev2.auth

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.data.SecureStorage
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.network.TokenStorage

private const val TAG = "AuthRepository"

// ── Wire-protocol DTOs (mirror ServerModels on client side) ──────────────────

@Serializable
private data class AuthRequest(val username: String, val password: String)

@Serializable
data class AuthApiResponse(
    val success: Boolean      = false,
    val accessToken: String   = "",
    val refreshToken: String  = "",
    val userId: String        = "",
    val username: String      = "",
    val message: String       = ""
)

// ── Auth state ────────────────────────────────────────────────────────────────

data class AuthState(
    val isLoggedIn: Boolean   = false,
    val userId: String        = "",
    val username: String      = "",
    val accessToken: String   = "",
    val refreshToken: String  = ""
)

// ── Repository ────────────────────────────────────────────────────────────────

/**
 * HTTP-backed auth repository.
 *
 * [tokenStorage] is shared with [buildHttpClient] so that login/register/restore
 * immediately seeds the Ktor Auth plugin without requiring a restart.
 *
 * Tokens are kept in-memory only. For persistence across cold starts, persist
 * accessToken + refreshToken in EncryptedSharedPreferences (Android) or
 * Keychain (iOS) using an expect/actual wrapper, then call [restoreSession] on
 * app start.
 */
class AuthRepository(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStorage: TokenStorage,
    private val secureStorage: SecureStorage,
) {
    companion object {
        internal const val KEY_ACCESS_TOKEN  = "fl_access_token"
        internal const val KEY_REFRESH_TOKEN = "fl_refresh_token"
    }

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    // ── Session restore ───────────────────────────────────────────────────────

    /**
     * Called once on app cold start. Reads tokens from [SecureStorage] and
     * validates them via /auth/me. Silently stays logged-out on any failure.
     */
    suspend fun restoreSessionFromStorage() {
        val access  = withContext(Dispatchers.Default) { secureStorage.get(KEY_ACCESS_TOKEN) }
        val refresh = withContext(Dispatchers.Default) { secureStorage.get(KEY_REFRESH_TOKEN) }
        Napier.d("Restoring session: access=${access?.take(10)}..., refresh=${refresh?.take(10)}...", tag = TAG)
        if (access.isNullOrBlank() || refresh.isNullOrBlank()) {
            Napier.d("No stored tokens found", tag = TAG)
            return
        }
        restoreSession(access, refresh)
    }

    /** Persist a rotated token pair (called by HttpClientFactory after silent refresh). */
    suspend fun persistTokens(accessToken: String, refreshToken: String) {
        withContext(Dispatchers.Default) {
            secureStorage.save(KEY_ACCESS_TOKEN,  accessToken)
            secureStorage.save(KEY_REFRESH_TOKEN, refreshToken)
            Napier.d("Persisted tokens: access=${accessToken.take(10)}..., refresh=${refreshToken.take(10)}...", tag = TAG)
        }
    }

    /**
     * Called at app start if persisted tokens were found (e.g. from DataStore).
     * Seeds TokenStorage and validates the access token via /auth/me.
     * Silently stays logged-out on network failure or expired tokens.
     */
    suspend fun restoreSession(accessToken: String, refreshToken: String) {
        tokenStorage.update(accessToken, refreshToken)

        runCatching {
            val resp: AuthApiResponse = httpClient.get("$baseUrl/auth/me").body()
            if (resp.success) {
                // Read tokens from tokenStorage rather than from the function args.
                // By the time /auth/me succeeds, Ktor's Bearer plugin may have silently
                // refreshed the token pair (401 → /auth/refresh → retry). The args would
                // then contain the old, already-consumed tokens while tokenStorage holds
                // the fresh pair that the Ktor plugin stored via onTokensRefreshed.
                _authState.value = AuthState(
                    isLoggedIn   = true,
                    userId       = resp.userId,
                    username     = resp.username,
                    accessToken  = tokenStorage.accessToken,
                    refreshToken = tokenStorage.refreshToken
                )
                Napier.d("Session restored userId=${resp.userId}", tag = TAG)
            } else {
                Napier.w("Session restore rejected by server", tag = TAG)
                tokenStorage.clear()
            }
        }.onFailure { e ->
            Napier.w("Session restore failed: ${e.message}", tag = TAG)
            tokenStorage.clear()
        }
    }

    // ── Auth actions ──────────────────────────────────────────────────────────

    suspend fun login(username: String, password: String): Result<AuthState> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException(Strings.errAuthFillFields))
        }

        return runCatching {
            val response = httpClient.post("$baseUrl/auth/login") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(username.trim(), password))
            }
            if (!response.status.isSuccess()) {
                val msg = runCatching { response.body<AuthApiResponse>().message }.getOrNull()
                throw IllegalStateException(msg?.ifBlank { null } ?: "${Strings.errAuthServerUnavailable} (${response.status.value})")
            }
            val resp = response.body<AuthApiResponse>()

            if (!resp.success) throw IllegalArgumentException(resp.message)

            tokenStorage.update(resp.accessToken, resp.refreshToken)
            withContext(Dispatchers.Default) {
                secureStorage.save(KEY_ACCESS_TOKEN,  resp.accessToken)
                secureStorage.save(KEY_REFRESH_TOKEN, resp.refreshToken)
            }
            Napier.i("Login success userId=${resp.userId}, persisted tokens: access=${resp.accessToken.take(10)}...", tag = TAG)

            AuthState(
                isLoggedIn   = true,
                userId       = resp.userId,
                username     = resp.username,
                accessToken  = resp.accessToken,
                refreshToken = resp.refreshToken
            ).also { _authState.value = it }
        }
    }

    suspend fun register(username: String, password: String): Result<AuthState> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException(Strings.errAuthFillFields))
        }
        if (username.length < 3) {
            return Result.failure(IllegalArgumentException(Strings.errAuthLoginTooShort))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException(Strings.errAuthPasswordTooShort))
        }

        return runCatching {
            val response = httpClient.post("$baseUrl/auth/register") {
                expectSuccess = false
                contentType(ContentType.Application.Json)
                setBody(AuthRequest(username.trim(), password))
            }
            if (!response.status.isSuccess()) {
                val msg = runCatching { response.body<AuthApiResponse>().message }.getOrNull()
                throw IllegalStateException(msg?.ifBlank { null } ?: "${Strings.errAuthServerUnavailable} (${response.status.value})")
            }
            val resp = response.body<AuthApiResponse>()

            if (!resp.success) throw IllegalArgumentException(resp.message)

            tokenStorage.update(resp.accessToken, resp.refreshToken)
            withContext(Dispatchers.Default) {
                secureStorage.save(KEY_ACCESS_TOKEN,  resp.accessToken)
                secureStorage.save(KEY_REFRESH_TOKEN, resp.refreshToken)
            }
            Napier.i("Registration success userId=${resp.userId}, persisted tokens: access=${resp.accessToken.take(10)}...", tag = TAG)

            AuthState(
                isLoggedIn   = true,
                userId       = resp.userId,
                username     = resp.username,
                accessToken  = resp.accessToken,
                refreshToken = resp.refreshToken
            ).also { _authState.value = it }
        }
    }

    fun logout() {
        Napier.i("Logout userId=${_authState.value.userId}", tag = TAG)
        tokenStorage.clear()
        secureStorage.clear(KEY_ACCESS_TOKEN)
        secureStorage.clear(KEY_REFRESH_TOKEN)
        _authState.value = AuthState()
    }

    // Convenience accessor — prefer reading from authState.collectAsState() in UI.
    val currentAccessToken: String get() = _authState.value.accessToken
}
