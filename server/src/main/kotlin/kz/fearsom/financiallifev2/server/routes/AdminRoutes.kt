package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.admin.UpsertCharacterRequest
import kz.fearsom.financiallifev2.admin.UpsertEraRequest
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdminRoutes")

// Typed responses — kotlinx cannot serialize Map<String, Any> (mixed-type maps
// compile but fail at runtime inside ContentNegotiation).
@Serializable
private data class EntityDeleteResponse(val deleted: Boolean, val id: String, val statsRowsDeleted: Int)

@Serializable
private data class EntityToggleResponse(val id: String, val isActive: Boolean)

/**
 * Admin API for character and era management.
 *
 * Authorization is handled ONCE at mount point: these routes live inside
 * `authenticate(ADMIN_SESSION_AUTH, ADMIN_KEY_AUTH)` in Routing.kt — either a
 * valid admin session cookie (SPA) or `Authorization: Bearer <ADMIN_KEY>`
 * (programmatic access). No per-handler checks: a new endpoint added here is
 * guarded automatically.
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
    erasRepository: ErasRepository
) {
    route("/admin") {

        // ════════════════════════════════════════════════════════════════════
        //  CHARACTER ENDPOINTS
        // ════════════════════════════════════════════════════════════════════

        get("/characters") {
            call.respond(charactersRepository.listAll(activeOnly = false))
        }

        get("/characters/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val character = charactersRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character not found"))
            call.respond(character)
        }

        // Creates a new character or updates an existing one (upsert by id).
        post("/characters") {
            val req = call.receive<UpsertCharacterRequest>()
            if (req.id.isBlank() || req.name.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id and name are required"))
            }
            val character = charactersRepository.upsert(req)
            log.info("Character upserted id={} name={}", character.id, character.name)
            call.respond(HttpStatusCode.OK, character)
        }

        // Hard-delete: removes the character AND all completed_sessions rows
        // for this character across ALL users (application-level cascade).
        delete("/characters/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val result = charactersRepository.deleteWithStatsCascade(id)
            if (!result.characterFound) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character '$id' not found"))
            }

            log.info("Character hard-deleted id={} statsDeleted={}", id, result.statsRowsDeleted)
            call.respond(EntityDeleteResponse(deleted = true, id = id, statsRowsDeleted = result.statsRowsDeleted))
        }

        // Soft-delete: hides from game UI but preserves statistics.
        post("/characters/{id}/deactivate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            if (!charactersRepository.setActive(id, active = false)) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character '$id' not found"))
            }
            log.info("Character deactivated id={}", id)
            call.respond(EntityToggleResponse(id = id, isActive = false))
        }

        // Re-activates a previously deactivated character (atomic flag flip).
        post("/characters/{id}/activate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            if (!charactersRepository.setActive(id, active = true)) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Character '$id' not found"))
            }
            log.info("Character re-activated id={}", id)
            call.respond(charactersRepository.findById(id)!!)
        }

        // ════════════════════════════════════════════════════════════════════
        //  ERA ENDPOINTS
        // ════════════════════════════════════════════════════════════════════

        get("/eras") {
            call.respond(erasRepository.listAll(activeOnly = false))
        }

        get("/eras/{id}") {
            val id = call.parameters["id"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))
            val era = erasRepository.findById(id)
                ?: return@get call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era not found"))
            call.respond(era)
        }

        // Creates a new era or updates an existing one (upsert by id).
        post("/eras") {
            val req = call.receive<UpsertEraRequest>()
            if (req.id.isBlank() || req.name.isBlank()) {
                return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "id and name are required"))
            }
            val era = erasRepository.upsert(req)
            log.info("Era upserted id={} name={}", era.id, era.name)
            call.respond(HttpStatusCode.OK, era)
        }

        // Hard-delete: removes the era AND all completed_sessions rows
        // for this era across ALL users (application-level cascade).
        delete("/eras/{id}") {
            val id = call.parameters["id"]
                ?: return@delete call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            val result = erasRepository.deleteWithStatsCascade(id)
            if (!result.eraFound) {
                return@delete call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era '$id' not found"))
            }

            log.info("Era hard-deleted id={} statsDeleted={}", id, result.statsRowsDeleted)
            call.respond(EntityDeleteResponse(deleted = true, id = id, statsRowsDeleted = result.statsRowsDeleted))
        }

        // Soft-delete: hides from game UI but preserves statistics.
        post("/eras/{id}/deactivate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            if (!erasRepository.setActive(id, active = false)) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era '$id' not found"))
            }
            log.info("Era deactivated id={}", id)
            call.respond(EntityToggleResponse(id = id, isActive = false))
        }

        // Re-activates a previously deactivated era (atomic flag flip).
        post("/eras/{id}/activate") {
            val id = call.parameters["id"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing id"))

            if (!erasRepository.setActive(id, active = true)) {
                return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Era '$id' not found"))
            }
            log.info("Era re-activated id={}", id)
            call.respond(erasRepository.findById(id)!!)
        }
    }
}
