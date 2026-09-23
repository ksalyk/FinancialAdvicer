package kz.fearsom.financiallifev2.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.security.MessageDigest
import kz.fearsom.financiallifev2.server.auth.JwtConfig

/** Marker principal for requests authenticated via the static ADMIN_KEY. */
object AdminKeyPrincipal

/**
 * Individual admin auth provider names (kept for the SPA login/logout routes
 * and for reference). Admin API routes use [ADMIN_COMBINED_AUTH] instead, which
 * accepts either a valid session cookie OR an ADMIN_KEY Bearer token (OR logic).
 *
 * Note: `authenticate(ADMIN_SESSION_AUTH, ADMIN_KEY_AUTH)` in Ktor 3.x uses AND
 * logic — both providers must succeed simultaneously, which is never the case for
 * bearer-only API requests. Use [ADMIN_COMBINED_AUTH] for all protected routes.
 */
const val ADMIN_SESSION_AUTH  = "admin-auth"
const val ADMIN_KEY_AUTH      = "admin-key"
const val ADMIN_COMBINED_AUTH = "admin-any"

/**
 * Custom provider that accepts an admin session cookie OR a static ADMIN_KEY
 * Bearer token (OR logic). Ktor's built-in `authenticate(A, B)` requires BOTH
 * providers to succeed (AND logic), which is wrong for session-vs-bearer auth.
 */
class AdminCombinedAuthProvider(config: Config) : AuthenticationProvider(config) {

    class Config(name: String?) : AuthenticationProvider.Config(name)

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call

        // 1. Try session cookie (browser SPA).
        val session = runCatching { call.sessions.get<AdminSession>() }.getOrNull()
        if (session != null && !session.isExpired()) {
            context.principal(config.name, session)
            return
        }

        // 2. Try static ADMIN_KEY Bearer token (API / programmatic access).
        val authHeader = call.request.headers[HttpHeaders.Authorization]
        if (authHeader != null) {
            val parts = authHeader.split(" ", limit = 2)
            if (parts.size == 2 && parts[0].equals("Bearer", ignoreCase = true)) {
                val adminKey = System.getenv("ADMIN_KEY") ?: "dev-admin-key"
                if (MessageDigest.isEqual(
                        parts[1].toByteArray(Charsets.UTF_8),
                        adminKey.toByteArray(Charsets.UTF_8)
                    )
                ) {
                    context.principal(config.name, AdminKeyPrincipal)
                    return
                }
            }
        }

        // 3. Neither credential present — challenge with 401.
        context.challenge("AdminCombined", AuthenticationFailedCause.NoCredentials) { challenge, challengeCall ->
            runCatching { challengeCall.sessions.clear<AdminSession>() }
            challengeCall.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Admin authentication required"))
            challenge.complete()
        }
    }
}

fun Application.configureSecurity() {
    install(Authentication) {
        // ── Game API: JWT bearer tokens ──────────────────────────────────────
        jwt("auth-jwt") {
            realm = "Finance LifeLine"
            verifier(
                JWT.require(Algorithm.HMAC256(JwtConfig.secret))
                    .withIssuer(JwtConfig.ISSUER)
                    .withAudience(JwtConfig.AUDIENCE)
                    .build()
            )
            validate { credential ->
                val userId   = credential.payload.getClaim("userId").asString()
                val username = credential.payload.getClaim("username").asString()
                if (!userId.isNullOrBlank() && !username.isNullOrBlank()) {
                    JWTPrincipal(credential.payload)
                } else null
            }
        }

        // ── Admin SPA: cookie session (used by login/logout/me routes) ───────
        session<AdminSession>(ADMIN_SESSION_AUTH) {
            validate { session -> session.takeUnless { it.isExpired() } }
            challenge {
                call.sessions.clear<AdminSession>()
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Admin session required"))
            }
        }

        // ── Admin API: static ADMIN_KEY Bearer token ─────────────────────────
        bearer(ADMIN_KEY_AUTH) {
            realm = "Finance LifeLine Admin"
            authenticate { credential ->
                val adminKey = System.getenv("ADMIN_KEY") ?: "dev-admin-key"
                val match = MessageDigest.isEqual(
                    credential.token.toByteArray(Charsets.UTF_8),
                    adminKey.toByteArray(Charsets.UTF_8)
                )
                if (match) AdminKeyPrincipal else null
            }
        }

        // ── Admin routes guard: session OR bearer (OR logic) ─────────────────
        register(AdminCombinedAuthProvider(AdminCombinedAuthProvider.Config(ADMIN_COMBINED_AUTH)))
    }
}
