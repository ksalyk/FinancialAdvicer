package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.toDto
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AdminScenarioRoutes")

/**
 * Admin scenario graph viewer endpoints.  All guarded by [isAdminAuthorized].
 *
 *   GET /admin/scenarios                          — list all valid {characterId, eraId} combos
 *   GET /admin/scenarios/{characterId}/{eraId}    — full ScenarioGraphDto for that combo
 *
 * The combo list is derived from the database: for each active character, every eraId
 * in character.eraIds that also exists as a known era produces one [ScenarioComboDto].
 * The label is formatted as "<character.name> × <era.name>".
 *
 * The detail endpoint delegates to [ScenarioGraphFactory.forCharacter]; returns 404 if
 * the factory throws (unknown / unsupported combo).
 */
fun Route.adminScenarioRoutes(
    charactersRepository: CharactersRepository,
    erasRepository: ErasRepository
) {
    route("/admin/scenarios") {

        // ── GET /admin/scenarios ──────────────────────────────────────────────
        get {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return@get
            }

            val characters = charactersRepository.listAll(activeOnly = false)
            val erasById   = erasRepository.listAll(activeOnly = false).associateBy { it.id }

            val combos = characters.flatMap { character ->
                character.eraIds.mapNotNull { eraId ->
                    val era = erasById[eraId] ?: return@mapNotNull null
                    ScenarioComboDto(
                        characterId = character.id,
                        eraId       = era.id,
                        label       = "${character.name} × ${era.name}"
                    )
                }
            }

            call.respond(combos)
        }

        // ── GET /admin/scenarios/{characterId}/{eraId} ────────────────────────
        get("/{characterId}/{eraId}") {
            if (!call.isAdminAuthorized()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Unauthorized")); return@get
            }

            val characterId = call.parameters["characterId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing characterId"))
            val eraId = call.parameters["eraId"]
                ?: return@get call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing eraId"))

            val graph = try {
                ScenarioGraphFactory.forCharacter(characterId, eraId)
            } catch (e: IllegalStateException) {
                log.warn("No scenario graph for characterId={} eraId={}: {}", characterId, eraId, e.message)
                return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No scenario graph for characterId=$characterId eraId=$eraId")
                )
            }

            call.respond(graph.toDto())
        }
    }
}
