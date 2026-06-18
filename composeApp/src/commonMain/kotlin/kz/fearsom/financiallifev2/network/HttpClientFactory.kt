package kz.fearsom.financiallifev2.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// ── Wire-protocol DTOs (must match ServerModels.AuthResponse / RefreshRequest) ─

@Serializable
private data class RefreshRequestDto(val refreshToken: String)

@Serializable
private data class AuthResponseDto(
    val success: Boolean = false,
    val accessToken: String = "",
    val refreshToken: String = ""
)

// ── Factory ───────────────────────────────────────────────────────────────────

/**
 * Builds a pre-configured [HttpClient] with:
 *  - JSON content negotiation
 *  - Napier-backed request/response logging (BODY level)
 *  - Bearer token auth with automatic silent refresh
 *
 * The [tokenStorage] singleton is shared with [AuthRepository].
 * [onTokenRefreshFailed] is called when the server rejects the refresh token
 * (expired / revoked) — use it to trigger a logout flow.
 *
 * Engine is resolved via [createPlatformEngine]:
 *   Android → OkHttp (with optional CertificatePinner for public-key pinning)
 *   iOS     → Darwin (NSURLSession; ATS enforces HTTPS by default)
 */
fun buildHttpClient(
    tokenStorage: TokenStorage,
    baseUrl: String,
    onTokenRefreshFailed: () -> Unit,
    /** Called after a successful silent token rotation so the new pair can be persisted. */
    onTokensRefreshed: suspend (accessToken: String, refreshToken: String) -> Unit = { _, _ -> },
): HttpClient = HttpClient(createPlatformEngine()) {

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            encodeDefaults    = true
            isLenient         = true
        })
    }

    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                Napier.d(message, tag = "KtorHttp")
            }
        }
        level = LogLevel.BODY
    }

    install(Auth) {
        bearer {
            // Called once to seed the plugin with whatever tokens are in memory.
            loadTokens {
                BearerTokens(
                    accessToken  = tokenStorage.accessToken,
                    refreshToken = tokenStorage.refreshToken
                )
            }

            // Called when the server responds with 401 on a protected endpoint.
            // `client` here is a special instance WITHOUT the Auth plugin to
            // avoid infinite recursion on the refresh call itself.
            refreshTokens {
                Napier.d("Access token expired, refreshing…", tag = "KtorAuth")

                val refreshToken = oldTokens?.refreshToken
                if (refreshToken.isNullOrBlank()) {
                    // No refresh token yet — this is a race condition during cold-start
                    // (e.g. a background request fired before restoreSession() completed).
                    // Don't logout; just let the request fail with 401 and retry later.
                    Napier.w("refreshTokens called with empty refresh token — skipping logout", tag = "KtorAuth")
                    return@refreshTokens null
                }

                val resp = client.post("$baseUrl/auth/refresh") {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequestDto(refreshToken))
                }

                if (resp.status == HttpStatusCode.OK) {
                    val body = resp.body<AuthResponseDto>()
                    tokenStorage.update(body.accessToken, body.refreshToken)
                    onTokensRefreshed(body.accessToken, body.refreshToken)
                    Napier.d("Token rotated successfully", tag = "KtorAuth")
                    BearerTokens(body.accessToken, body.refreshToken)
                } else {
                    Napier.w("Token refresh failed (${resp.status}), logging out", tag = "KtorAuth")
                    tokenStorage.clear()
                    onTokenRefreshFailed()
                    null
                }
            }

            // Proactively attach the token to every request except auth endpoints.
            sendWithoutRequest { request ->
                val path = request.url.encodedPath
                tokenStorage.isAccessTokenPresent() &&
                    !path.contains("/auth/login") &&
                    !path.contains("/auth/register")
            }
        }
    }
}
