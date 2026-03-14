package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.scenarios.AsanScenarioGraph
import kz.fearsom.financiallifev2.server.models.GameStateRequest
import kz.fearsom.financiallifev2.server.models.GameStateResponse
import kz.fearsom.financiallifev2.server.repository.GameRepository
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("GameRoutes")

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
data class CharacterDto(
    val name: String,
    val description: String,
    val initialState: PlayerState
)

@Serializable
data class StartGameResponse(val userId: String, val characterName: String)

@Serializable
data class ChoiceResponse(val success: Boolean = false, val message: String = "")

@Serializable
data class EventResponse(
    val eventId: String,
    val message: String,
    val flavor: String,
    val options: List<OptionDto>
)

@Serializable
data class OptionDto(val id: String, val text: String, val emoji: String)

@Serializable
data class SnapshotDto(
    val snapshotId: String,
    val slotName: String,
    val savedAt: Long,
    val stateJson: String
)

// ── Routes ────────────────────────────────────────────────────────────────────
// All routes here are assumed to be mounted inside authenticate("auth-jwt") {}
// in Routing.kt, so call.principal<JWTPrincipal>()!! is always non-null.

fun Route.gameRoutes(gameRepository: GameRepository) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    route("/game") {

        // ── GET /game/character ───────────────────────────────────────────────
        get("/character") {
            call.respond(CharacterDto(
                name         = "Асан",
                description  = "28-летний джун-разработчик из Алматы",
                initialState = AsanScenarioGraph().initialPlayerState
            ))
        }

        // ── POST /game/start ──────────────────────────────────────────────────
        post("/start") {
            val userId = call.jwtUserId()

            val engine = GameEngine()
            val state  = engine.startGame()

            gameRepository.upsertSession(
                userId         = userId,
                stateJson      = json.encodeToString(state),
                currentEventId = state.currentEventId
            )

            log.info("Game started userId={}", userId)

            call.respond(StartGameResponse(userId = userId, characterName = "Асан"))
        }

        // ── GET /game/event ───────────────────────────────────────────────────
        get("/event") {
            val userId = call.jwtUserId()

            val session = gameRepository.loadSession(userId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No active session — call /game/start first")
                )

            val event = AsanScenarioGraph().findEvent(session.currentEventId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "Event '${session.currentEventId}' not in graph")
                )

            call.respond(
                EventResponse(
                    eventId = event.id,
                    message = event.message,
                    flavor  = event.flavor,
                    options = event.options.map { OptionDto(it.id, it.text, it.emoji) }
                )
            )
        }

        // ── POST /game/choose/{optionId} ──────────────────────────────────────
        post("/choose/{optionId}") {
            val userId   = call.jwtUserId()
            val optionId = call.parameters["optionId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing optionId"))

            val session = gameRepository.loadSession(userId)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active session"))

            val engine = GameEngine()
            engine.loadState(json.decodeFromString<GameState>(session.stateJson))
            val newState = engine.makeChoice(optionId)

            gameRepository.upsertSession(
                userId         = userId,
                stateJson      = json.encodeToString(newState),
                currentEventId = newState.currentEventId
            )

            log.debug("Choice made userId={} optionId={} nextEvent={}", userId, optionId, newState.currentEventId)

            call.respond(ChoiceResponse(success = true))
        }

        // ── POST /game/save ───────────────────────────────────────────────────
        post("/save") {
            val userId = call.jwtUserId()
            val req    = call.receive<GameStateRequest>()

            val snapshot = gameRepository.saveSnapshot(
                userId    = userId,           // always use the JWT userId, never trust body
                stateJson = req.stateJson,
                slotName  = req.slotName ?: "manual"
            )

            log.info("Snapshot saved userId={} snapshotId={}", userId, snapshot.snapshotId)

            call.respond(mapOf(
                "snapshotId" to snapshot.snapshotId,
                "savedAt"    to snapshot.savedAt.toString()
            ))
        }

        // ── GET /game/load ────────────────────────────────────────────────────
        get("/load") {
            val userId  = call.jwtUserId()
            val session = gameRepository.loadSession(userId)

            call.respond(
                GameStateResponse(
                    found     = session != null,
                    stateJson = session?.stateJson ?: ""
                )
            )
        }

        // ── GET /game/snapshots ───────────────────────────────────────────────
        get("/snapshots") {
            val userId    = call.jwtUserId()
            val snapshots = gameRepository.listSnapshots(userId)

            call.respond(snapshots.map {
                SnapshotDto(it.snapshotId, it.slotName, it.savedAt, it.stateJson)
            })
        }

        // ── POST /game/restore/{snapshotId} ───────────────────────────────────
        post("/restore/{snapshotId}") {
            val requestingUserId = call.jwtUserId()
            val snapshotId = call.parameters["snapshotId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing snapshotId"))

            val snapshot = gameRepository.loadSnapshot(snapshotId)
                ?: return@post call.respond(HttpStatusCode.NotFound, mapOf("error" to "Snapshot not found"))

            // Ensure user can only restore their own snapshots.
            if (snapshot.userId != requestingUserId) {
                return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Forbidden"))
            }

            val state = json.decodeFromString<GameState>(snapshot.stateJson)
            gameRepository.upsertSession(
                userId         = requestingUserId,
                stateJson      = snapshot.stateJson,
                currentEventId = state.currentEventId
            )

            log.info("Snapshot restored userId={} snapshotId={}", requestingUserId, snapshotId)

            call.respond(mapOf("restored" to true))
        }

        // ── GET /game/health ──────────────────────────────────────────────────
        get("/health") {
            call.respond(mapOf("status" to "ok", "storage" to "postgresql"))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Extracts userId from the JWT principal attached by Ktor's auth plugin.
 * Only valid inside routes wrapped with authenticate("auth-jwt") {}.
 */
private fun ApplicationCall.jwtUserId(): String =
    principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
