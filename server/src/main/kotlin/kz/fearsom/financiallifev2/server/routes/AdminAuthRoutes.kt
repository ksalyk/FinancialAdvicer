package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.server.plugins.AdminSession
import kz.fearsom.financiallifev2.server.plugins.RateLimiter
import kz.fearsom.financiallifev2.server.plugins.authRateLimiter
import kz.fearsom.financiallifev2.server.plugins.checkRateLimit
import kz.fearsom.financiallifev2.server.plugins.isExpired

@Serializable
private data class AdminLoginRequest(val username: String, val password: String)

@Serializable
private data class AdminMeResponse(val username: String)

/**
 * Browser-session auth endpoints for the admin SPA.
 *
 * POST /admin/login   — validates ADMIN_USERNAME / ADMIN_PASSWORD env vars; sets httpOnly cookie
 * POST /admin/logout  — clears the session cookie
 * GET  /admin/me      — 200 {username} when authenticated, 401 otherwise
 *
 * These routes stay OUTSIDE the admin guard (login can't require a session).
 * ADMIN_KEY Bearer access is handled by the ADMIN_KEY_AUTH provider (Security.kt),
 * which — together with ADMIN_SESSION_AUTH — guards all other admin routes.
 */
fun Route.adminAuthRoutes(loginLimiter: RateLimiter? = authRateLimiter) {
    route("/admin") {

        post("/login") {
            // Same budget as /auth/login — the admin panel is the most valuable
            // brute-force target on the server, not the least.
            if (loginLimiter != null && !call.checkRateLimit(loginLimiter)) return@post
            val req = call.receive<AdminLoginRequest>()
            val adminUsername = System.getenv("ADMIN_USERNAME") ?: "admin"
            val adminPassword = System.getenv("ADMIN_PASSWORD") ?: "dev-admin-password"

            // Constant-time compares on both fields — prevents timing-based credential enumeration
            val usernameMatch = MessageDigest.isEqual(
                req.username.trim().toByteArray(Charsets.UTF_8),
                adminUsername.toByteArray(Charsets.UTF_8)
            )
            val passwordMatch = MessageDigest.isEqual(
                req.password.toByteArray(Charsets.UTF_8),
                adminPassword.toByteArray(Charsets.UTF_8)
            )
            if (usernameMatch && passwordMatch) {
                call.sessions.set(AdminSession(username = req.username.trim(), issuedAt = System.currentTimeMillis()))
                call.respond(HttpStatusCode.OK, mapOf("success" to true))
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid credentials"))
            }
        }

        post("/logout") {
            call.sessions.clear<AdminSession>()
            call.respond(HttpStatusCode.OK, mapOf("success" to true))
        }

        get("/me") {
            val session = call.sessions.get<AdminSession>()
            if (session != null && !session.isExpired()) {
                call.respond(AdminMeResponse(username = session.username))
            } else {
                if (session != null) call.sessions.clear<AdminSession>()
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
            }
        }
    }
}
