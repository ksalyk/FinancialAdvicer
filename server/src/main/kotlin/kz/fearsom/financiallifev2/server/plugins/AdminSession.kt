package kz.fearsom.financiallifev2.server.plugins

import io.ktor.server.application.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable

/** Represents an authenticated browser admin session. */
@Serializable
data class AdminSession(
    val username: String,
    val issuedAt: Long
)

/**
 * Installs the Sessions plugin with an HMAC-signed httpOnly cookie for admin auth.
 *
 * Required env vars:
 *   SESSION_SECRET — arbitrary bytes (use a long random string in production)
 *
 * Dev default is intentionally weak — override before deploying.
 */
fun Application.configureAdminSession() {
    val secretKey = (System.getenv("SESSION_SECRET") ?: "dev-admin-session-secret-key!!!!").toByteArray()

    install(Sessions) {
        cookie<AdminSession>("admin_session") {
            cookie.httpOnly = true
            cookie.secure = System.getenv("SESSION_SECURE")?.lowercase() == "true"
            cookie.extensions["SameSite"] = "Lax"
            cookie.path = "/"
            transform(SessionTransportTransformerMessageAuthentication(secretKey, "HmacSHA256"))
        }
    }
}
