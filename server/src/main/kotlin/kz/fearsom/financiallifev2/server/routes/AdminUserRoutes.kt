package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.admin.AdminUserDetailRow
import kz.fearsom.financiallifev2.admin.AdminUserListResponse
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import kz.fearsom.financiallifev2.server.repository.UserRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdminUserRoutes")

@Serializable
private data class ResetPasswordRequest(val password: String)

/**
 * Admin user management endpoints.  All are guarded by [isAdminAuthorized].
 * passwordHash is NEVER included in any response.
 *
 *   GET    /admin/users?limit&offset&search  — paginated list
 *   GET    /admin/users/{id}                 — detail + lifetime stats
 *   POST   /admin/users/{id}/reset-password  — body { password }; revokes tokens
 *   DELETE /admin/users/{id}                 — cascade delete
 */
fun Route.adminUserRoutes(
    userRepository: UserRepository,
    statisticsRepository: StatisticsRepository
) {
    route("/admin/users") {

        // ── GET /admin/users ──────────────────────────────────────────────────
        get {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return@get
            }
            val limit  = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 200) ?: 50
            val offset = call.request.queryParameters["offset"]?.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            val search = call.request.queryParameters["search"]?.takeIf { it.isNotBlank() }

            val items = userRepository.listUsers(limit, offset, search)
            val total = userRepository.countUsers(search)
            call.respond(AdminUserListResponse(items = items, total = total))
        }

        // ── GET /admin/users/{id} ─────────────────────────────────────────────
        get("/{id}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return@get
            }
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val user = userRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))

            val stats = statisticsRepository.getPlayerStatistics(id)
            call.respond(
                AdminUserDetailRow(
                    id                  = user.id,
                    username            = user.username,
                    createdAt           = user.createdAt,
                    gamesPlayed         = stats.totalGamesPlayed,
                    bestEnding          = stats.bestEnding,
                    averageCapitalAtEnd = stats.averageCapitalAtEnd,
                    endingDistribution  = stats.endingDistribution
                )
            )
        }

        // ── POST /admin/users/{id}/reset-password ─────────────────────────────
        post("/{id}/reset-password") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return@post
            }
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val req = call.receive<ResetPasswordRequest>()
            if (req.password.length < 6) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Password too short (min 6 chars)"))
            }

            val success = userRepository.updatePassword(id, req.password)
            if (!success) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            }
            log.info("Admin reset password for userId={}", id)
            call.respond(mapOf("success" to true))
        }

        // ── DELETE /admin/users/{id} ──────────────────────────────────────────
        delete("/{id}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return@delete
            }
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val deleted = userRepository.deleteUserCascade(id)
            if (!deleted) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            }
            log.info("Admin deleted userId={} (cascade)", id)
            call.respond(mapOf("deleted" to true, "userId" to id))
        }
    }
}
