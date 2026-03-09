package kz.fearsom.financiallifev2.network

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
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

                val resp = client.post("$baseUrl/auth/refresh") {
                    markAsRefreshTokenRequest()
                    contentType(ContentType.Application.Json)
                    setBody(RefreshRequestDto(oldTokens?.refreshToken ?: ""))
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
                !path.contains("/auth/login") && !path.contains("/auth/register")
            }
        }
    }
}
