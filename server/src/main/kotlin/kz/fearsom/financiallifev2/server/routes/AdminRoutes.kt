package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import kz.fearsom.financiallifev2.server.repository.UpsertCharacterRequest
import kz.fearsom.financiallifev2.server.repository.UpsertEraRequest
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdminRoutes")

/** Checks the ADMIN_KEY Bearer token; returns false and responds 401 if invalid. */
private fun ApplicationCall.isAdminAuthorized(): Boolean {
    val adminKey = System.getenv("ADMIN_KEY") ?: "dev-admin-key"
    val provided = request.header(HttpHeaders.Authorization)
        ?.removePrefix("Bearer ")?.trim() ?: ""
    return provided == adminKey
}

/**
 * Admin API for character and era management.
 *
 * All routes are protected by a static ADMIN_KEY (Bearer token), configured
 * via the ADMIN_KEY environment variable.  Set it to a long random string
 * in production; defaults to "dev-admin-key" for local development only.
 *
 * Authentication: include `Authorization: Bearer <ADMIN_KEY>` in every request.
 *
 * Character endpoints:
 *   GET    /admin/characters              — list all (incl. inactive)
 *   GET    /admin/characters/{id}         — single character
 *   POST   /admin/characters              — create or update (upsert)
 *   DELETE /admin/characters/{id}         — hard-delete + cascade stats (all users)
 *   POST   /admin/characters/{id}/deactivate — soft-delete (hide from UI, keep stats)
 *   POST   /admin/characters/{id}/activate   — re-activate
 *
 * Era endpoints (mirror of character endpoints):
 *   GET    /admin/eras                    — list all (incl. inactive)
 *   GET    /admin/eras/{id}               — single era
 *   POST   /admin/eras                    — create or update (upsert)
 *   DELETE /admin/eras/{id}               — hard-delete + cascade stats (all users)
 *   POST   /admin/eras/{id}/deactivate    — soft-delete (hide from UI, keep stats)
 *   POST   /admin/eras/{id}/activate      — re-activate
 */
fun Route.adminRoutes(
    charactersRepository: CharactersRepository,
    erasRepository: ErasRepository,
    statisticsRepository: StatisticsRepository
) {
    route("/admin") {

        // ── GET /admin/characters ─────────────────────────────────────────────
        get("/characters") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@get
            }
            val characters = charactersRepository.listAll(activeOnly = false)
            call.respond(characters)
        }

        // ── GET /admin/characters/{id} ────────────────────────────────────────
        get("/characters/{id}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@get
            }
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val character = charactersRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character not found"))
            call.respond(character)
        }

        // ── POST /admin/characters ────────────────────────────────────────────
        // Creates a new character or updates an existing one (upsert by id).
        post("/characters") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@post
            }
            val req = call.receive<UpsertCharacterRequest>()
            if (req.id.isBlank() || req.name.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id and name are required"))
            }
            val character = charactersRepository.upsert(req)
            log.info("Character upserted id={} name={}", character.id, character.name)
            call.respond(HttpStatusCode.OK, character)
        }

        // ── DELETE /admin/characters/{id} ─────────────────────────────────────
        // Hard-delete: removes the character AND all completed_sessions rows
        // for this character across ALL users (application-level cascade).
        delete("/characters/{id}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@delete
            }
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val result = charactersRepository.deleteWithStatsCascade(id)
            if (!result.characterFound) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character '$id' not found"))
            }

            log.info("Character hard-deleted id={} statsDeleted={}", id, result.statsRowsDeleted)
            call.respond(mapOf(
                "deleted"          to true,
                "characterId"      to id,
                "statsRowsDeleted" to result.statsRowsDeleted
            ))
        }

        // ── POST /admin/characters/{id}/deactivate ────────────────────────────
        // Soft-delete: hides from game UI but preserves statistics.
        post("/characters/{id}/deactivate") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@post
            }
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val success = charactersRepository.softDelete(id)
            if (!success) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character '$id' not found"))
            }
            log.info("Character soft-deleted id={}", id)
            call.respond(mapOf("deactivated" to true, "characterId" to id))
        }

        // ── POST /admin/characters/{id}/activate ──────────────────────────────
        // Re-activates a previously deactivated character.
        post("/characters/{id}/activate") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@post
            }
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val character = charactersRepository.findById(id)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character '$id' not found"))

            val updated = charactersRepository.upsert(
                UpsertCharacterRequest(id = character.id, name = character.name,
                    emoji = character.emoji, type = character.type,
                    eraIds = character.eraIds, isActive = true)
            )
            log.info("Character re-activated id={}", id)
            call.respond(updated)
        }

        // ════════════════════════════════════════════════════════════════════
        //  ERA ENDPOINTS
        // ════════════════════════════════════════════════════════════════════

        // ── GET /admin/eras ───────────────────────────────────────────────────
        get("/eras") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@get
            }
            val eras = erasRepository.listAll(activeOnly = false)
            call.respond(eras)
        }

        // ── GET /admin/eras/{id} ──────────────────────────────────────────────
        get("/eras/{id}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@get
            }
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val era = erasRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era not found"))
            call.respond(era)
        }

        // ── POST /admin/eras ──────────────────────────────────────────────────
        // Creates a new era or updates an existing one (upsert by id).
        post("/eras") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@post
            }
            val req = call.receive<UpsertEraRequest>()
            if (req.id.isBlank() || req.name.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id and name are required"))
            }
            val era = erasRepository.upsert(req)
            log.info("Era upserted id={} name={}", era.id, era.name)
            call.respond(HttpStatusCode.OK, era)
        }

        // ── DELETE /admin/eras/{id} ───────────────────────────────────────────
        // Hard-delete: removes the era AND all completed_sessions rows
        // for this era across ALL users (application-level cascade).
        delete("/eras/{id}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@delete
            }
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val result = erasRepository.deleteWithStatsCascade(id)
            if (!result.eraFound) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era '$id' not found"))
            }

            log.info("Era hard-deleted id={} statsDeleted={}", id, result.statsRowsDeleted)
            call.respond(mapOf(
                "deleted"          to true,
                "eraId"            to id,
                "statsRowsDeleted" to result.statsRowsDeleted
            ))
        }

        // ── POST /admin/eras/{id}/deactivate ──────────────────────────────────
        // Soft-delete: hides from game UI but preserves statistics.
        post("/eras/{id}/deactivate") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@post
            }
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val success = erasRepository.softDelete(id)
            if (!success) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era '$id' not found"))
            }
            log.info("Era soft-deleted id={}", id)
            call.respond(mapOf("deactivated" to true, "eraId" to id))
        }

        // ── POST /admin/eras/{id}/activate ────────────────────────────────────
        // Re-activates a previously deactivated era.
        post("/eras/{id}/activate") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid admin key")); return@post
            }
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val era = erasRepository.findById(id)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era '$id' not found"))

            val updated = erasRepository.upsert(
                UpsertEraRequest(
                    id                    = era.id,
                    name                  = era.name,
                    description           = era.description,
                    emoji                 = era.emoji,
                    startYear             = era.startYear,
                    endYear               = era.endYear,
                    availableCharacterIds = era.availableCharacterIds,
                    isActive              = true,
                    isLocked              = era.isLocked
                )
            )
            log.info("Era re-activated id={}", id)
            call.respond(updated)
        }
    }
}
