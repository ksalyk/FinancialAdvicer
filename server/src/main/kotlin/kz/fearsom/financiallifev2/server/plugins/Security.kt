package kz.fearsom.financiallifev2.server.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import java.security.MessageDigest
import kz.fearsom.financiallifev2.server.auth.JwtConfig

/** Marker principal for requests authenticated via the static ADMIN_KEY. */
object AdminKeyPrincipal

/**
 * Names of the two admin auth providers. Admin routes are mounted inside
 * `authenticate(ADMIN_SESSION_AUTH, ADMIN_KEY_AUTH)` — cookie session for the
 * SPA, static Bearer key for programmatic/API access. One guard, applied once,
 * instead of a manual check in every handler (which is easy to forget on a new
 * endpoint).
 */
const val ADMIN_SESSION_AUTH = "admin-auth"
const val ADMIN_KEY_AUTH     = "admin-key"

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

        // ── Admin SPA: cookie session ────────────────────────────────────────
        // Used by the browser admin panel. API clients use ADMIN_KEY Bearer below.
        session<AdminSession>(ADMIN_SESSION_AUTH) {
            validate { session -> session.takeUnless { it.isExpired() } }
            challenge {
                // Clear an expired/invalid cookie so the SPA falls back to login cleanly.
                call.sessions.clear<AdminSession>()
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Admin session required"))
            }
        }

        // ── Admin API: static ADMIN_KEY Bearer token ─────────────────────────
        // Fallback provider inside authenticate(ADMIN_SESSION_AUTH, ADMIN_KEY_AUTH):
        // tried when no valid session cookie is present.
        bearer(ADMIN_KEY_AUTH) {
            realm = "Finance LifeLine Admin"
            authenticate { credential ->
                val adminKey = System.getenv("ADMIN_KEY") ?: "dev-admin-key"
                // Constant-time compare — prevents timing-based key enumeration attacks.
                val match = MessageDigest.isEqual(
                    credential.token.toByteArray(Charsets.UTF_8),
                    adminKey.toByteArray(Charsets.UTF_8)
                )
                if (match) AdminKeyPrincipal else null
            }
        }
    }
}
