package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.achievements.AchievementKind
import kz.fearsom.financiallifev2.admin.UpsertAchievementRequest
import kz.fearsom.financiallifev2.admin.UserAchievementsAdminResponse
import kz.fearsom.financiallifev2.server.repository.AchievementCatalogRepository
import kz.fearsom.financiallifev2.server.repository.AchievementCatalogRepository.DeleteOutcome
import kz.fearsom.financiallifev2.server.repository.AchievementsRepository
import kz.fearsom.financiallifev2.server.repository.UserRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdminAchievementRoutes")

private val ACHIEVEMENT_ID_REGEX = Regex("^[a-z0-9][a-z0-9._\\-]{2,63}$")

@Serializable
private data class AchievementToggleResponse(val id: String, val isActive: Boolean)

@Serializable
private data class AchievementDeleteResponse(val deleted: Boolean, val achievementId: String)

@Serializable
private data class GrantResponse(val granted: Boolean, val userId: String, val achievementId: String)

@Serializable
private data class RevokeResponse(val revoked: Boolean, val userId: String, val achievementId: String)

/**
 * Admin achievement catalog management + per-user unlock administration.
 * Guarded at mount point by `authenticate(ADMIN_SESSION_AUTH, ADMIN_KEY_AUTH)`.
 *
 * Catalog:
 *   GET    /admin/achievements                 — all rows + unlock/vote stats
 *   POST   /admin/achievements                 — upsert definition (CUSTOM on create)
 *   POST   /admin/achievements/{id}/activate   — show in client catalogs
 *   POST   /admin/achievements/{id}/deactivate — hide (unlocks preserved)
 *   DELETE /admin/achievements/{id}            — hard-delete CUSTOM (SEEDED protected)
 *
 * Per-user:
 *   GET    /admin/users/{id}/achievements                — unlock list
 *   POST   /admin/users/{id}/achievements/{achievementId} — grant
 *   DELETE /admin/users/{id}/achievements/{achievementId} — revoke
 */
fun Route.adminAchievementRoutes(
    catalogRepository: AchievementCatalogRepository,
    achievementsRepository: AchievementsRepository,
    userRepository: UserRepository
) {
    route("/admin/achievements") {

        // ── GET /admin/achievements ───────────────────────────────────────────
        get {
            call.respond(catalogRepository.listAll(activeOnly = false))
        }

        // ── POST /admin/achievements ──────────────────────────────────────────
        // Upsert by definition.id. New ids become CUSTOM; existing rows keep source.
        post {
            val req = call.receive<UpsertAchievementRequest>()
            val def = req.definition

            val error = when {
                !ACHIEVEMENT_ID_REGEX.matches(def.id) ->
                    "id must match ${ACHIEVEMENT_ID_REGEX.pattern} (e.g. \"game.first_steps\", \"scam.ponzi\")"
                def.emoji.isBlank() -> "emoji is required"
                // A definition must be displayable: either an i18n key (code catalog)
                // or inline ru text (admin-authored).
                def.titleKey.isBlank() && (def.titleText == null || def.titleText!!.isBlank) ->
                    "either titleKey or titleText.ru is required"
                def.kind == AchievementKind.SCAM && def.dossier == null &&
                    (def.descText == null || def.descText!!.isBlank) ->
                    "SCAM achievements need a dossier or at least descText"
                else -> null
            }
            if (error != null) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
            }

            val row = catalogRepository.upsert(req)
            log.info("Achievement upserted id={} source={}", def.id, row.source)
            call.respond(row)
        }

        // ── POST /admin/achievements/{id}/activate ────────────────────────────
        post("/{id}/activate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            if (!catalogRepository.setActive(id, active = true)) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Achievement '$id' not found"))
            }
            call.respond(AchievementToggleResponse(id = id, isActive = true))
        }

        // ── POST /admin/achievements/{id}/deactivate ──────────────────────────
        post("/{id}/deactivate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            if (!catalogRepository.setActive(id, active = false)) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Achievement '$id' not found"))
            }
            call.respond(AchievementToggleResponse(id = id, isActive = false))
        }

        // ── DELETE /admin/achievements/{id} ───────────────────────────────────
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            when (catalogRepository.delete(id)) {
                DeleteOutcome.NotFound ->
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "Achievement '$id' not found"))
                DeleteOutcome.SeededProtected ->
                    call.respond(
                        HttpStatusCode.Conflict,
                        mapOf("error" to "'$id' is seeded from the code catalog and would be re-created " +
                            "on restart — deactivate it instead")
                    )
                DeleteOutcome.Deleted -> {
                    log.info("Achievement deleted id={}", id)
                    call.respond(AchievementDeleteResponse(deleted = true, achievementId = id))
                }
            }
        }
    }

    // ── Per-user unlock administration ────────────────────────────────────────
    route("/admin/users/{id}/achievements") {

        get {
            val userId = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user id"))
            userRepository.findById(userId)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))

            call.respond(UserAchievementsAdminResponse(
                userId  = userId,
                unlocks = achievementsRepository.listUnlocks(userId)
            ))
        }

        post("/{achievementId}") {
            val userId = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user id"))
            val achievementId = call.parameters["achievementId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing achievement id"))

            userRepository.findById(userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "User not found"))
            if (achievementId !in catalogRepository.allIds()) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Unknown achievement id '$achievementId'"))
            }

            val granted = achievementsRepository.adminGrant(userId, achievementId)
            log.info("Admin grant userId={} achievementId={} granted={}", userId, achievementId, granted)
            call.respond(GrantResponse(granted = granted, userId = userId, achievementId = achievementId))
        }

        delete("/{achievementId}") {
            val userId = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing user id"))
            val achievementId = call.parameters["achievementId"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing achievement id"))

            val revoked = achievementsRepository.adminRevoke(userId, achievementId)
            log.info("Admin revoke userId={} achievementId={} revoked={}", userId, achievementId, revoked)
            call.respond(RevokeResponse(revoked = revoked, userId = userId, achievementId = achievementId))
        }
    }
}
