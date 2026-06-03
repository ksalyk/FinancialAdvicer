package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.server.plugins.AdminSession

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
 * These routes must be mounted inside a `/api/v1` block (see Routing.kt).
 * ADMIN_KEY Bearer access is unchanged — see AdminRoutes.isAdminAuthorized().
 */
fun Route.adminAuthRoutes() {
    route("/admin") {

        post("/login") {
            val req = call.receive<AdminLoginRequest>()
            val adminUsername = System.getenv("ADMIN_USERNAME") ?: "admin"
            val adminPassword = System.getenv("ADMIN_PASSWORD") ?: "dev-admin-password"

            if (req.username.trim() == adminUsername && req.password == adminPassword) {
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
            if (session != null) {
                call.respond(AdminMeResponse(username = session.username))
            } else {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
            }
        }
    }
}
