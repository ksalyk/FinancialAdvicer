package kz.fearsom.financiallifev2.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.scenarios.ScenarioGraphFactory
import kz.fearsom.financiallifev2.server.models.GameStateRequest
import kz.fearsom.financiallifev2.server.models.GameStateResponse
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.RecordSessionRequest
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

private val log = LoggerFactory.getLogger("GameRoutes")

// ── DTOs ──────────────────────────────────────────────────────────────────────

@Serializable
data class CharacterDto(
    val name: String,
    val description: String,
    val initialState: PlayerState
)

@Serializable
data class StartGameRequest(
    val characterId: String   = "asan",
    val eraId: String         = "kz_2024",
    val characterName: String = "Асан"
)

@Serializable
data class StartGameResponse(
    val userId: String,
    val characterId: String,
    val eraId: String,
    val characterName: String
)

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

/**
 * Per-user mutex that serialises concurrent /game/choose requests for the same userId.
 *
 * Without this, two simultaneous POST /game/choose calls both load the same stale stateJson,
 * compute divergent new states, and the last upsertSession write wins — silently dropping
 * one choice. The mutex ensures load → process → save is atomic per user.
 *
 * The map grows by one entry per distinct userId that ever calls /choose and is never evicted.
 * For a game with reasonable user counts this is fine; add a size-bound cache if needed.
 */
private val choiceLocks = ConcurrentHashMap<String, Mutex>()

fun Route.gameRoutes(
    gameRepository: GameRepository,
    statisticsRepository: StatisticsRepository
) {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    route("/game") {

        // ── GET /game/character ───────────────────────────────────────────────
        // Returns the initial state for a given character+era combination.
        // Query params: characterId (default: asan), eraId (default: kz_2024)
        get("/character") {
            val characterId = call.request.queryParameters["characterId"] ?: "asan"
            val eraId       = call.request.queryParameters["eraId"]       ?: "kz_2024"

            val graph = ScenarioGraphFactory.forCharacter(characterId, eraId)

            call.respond(CharacterDto(
                name         = characterId,
                description  = "$characterId / $eraId",
                initialState = graph.initialPlayerState
            ))
        }

        // ── POST /game/start ──────────────────────────────────────────────────
        // Body: StartGameRequest (characterId, eraId, characterName).
        // Defaults to Асан/kz_2024 when body is absent — keeps old clients working.
        post("/start") {
            val userId = call.jwtUserId()
            val req    = runCatching { call.receive<StartGameRequest>() }
                .getOrDefault(StartGameRequest())

            val engine = GameEngine.forCharacterAndEra(req.characterId, req.eraId)
            val state  = engine.startGame(characterName = req.characterName)

            gameRepository.upsertSession(
                userId         = userId,
                stateJson      = json.encodeToString(state),
                currentEventId = state.currentEventId
            )

            log.info("Game started userId={} character={} era={}", userId, req.characterId, req.eraId)

            call.respond(StartGameResponse(
                userId        = userId,
                characterId   = req.characterId,
                eraId         = req.eraId,
                characterName = req.characterName
            ))
        }

        // ── GET /game/event ───────────────────────────────────────────────────
        get("/event") {
            val userId = call.jwtUserId()

            val session = gameRepository.loadSession(userId)
                ?: return@get call.respond(
                    HttpStatusCode.NotFound,
                    mapOf("error" to "No active session — call /game/start first")
                )

            // Decode just enough of the state to resolve the correct graph.
            val state = json.decodeFromString<GameState>(session.stateJson)
            val graph = ScenarioGraphFactory.forCharacter(
                state.playerState.characterId,
                state.playerState.eraId
            )

            val event = graph.findEvent(session.currentEventId)
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

            // Serialise concurrent choices for the same user so that the
            // load → process → save sequence is never interleaved.
            val mutex = choiceLocks.computeIfAbsent(userId) { Mutex() }
            mutex.withLock {
                val session = gameRepository.loadSession(userId)
                    ?: return@withLock call.respond(HttpStatusCode.NotFound, mapOf("error" to "No active session"))

                // Restore saved state into a correctly wired engine.
                // loadState() internally calls ScenarioGraphFactory (now cached), so no
                // redundant graph construction occurs here.
                val savedState = json.decodeFromString<GameState>(session.stateJson)
                val engine = GameEngine.forCharacterAndEra(
                    savedState.playerState.characterId,
                    savedState.playerState.eraId
                )
                engine.loadState(savedState)
                val newState = engine.makeChoice(optionId)

                gameRepository.upsertSession(
                    userId         = userId,
                    stateJson      = json.encodeToString(newState),
                    currentEventId = newState.currentEventId
                )

                log.debug("Choice made userId={} optionId={} nextEvent={}", userId, optionId, newState.currentEventId)

                call.respond(ChoiceResponse(success = true))
            }
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

        // ── POST /game/statistics/record ──────────────────────────────────────
        // Called by the client when a game session ends (any ending).
        post("/statistics/record") {
            val userId = call.jwtUserId()
            val req    = call.receive<RecordSessionRequest>()

            val id = statisticsRepository.recordSession(userId, req)

            log.info("Session recorded userId={} ending={} id={}", userId, req.ending, id)

            call.respond(mapOf("id" to id))
        }

        // ── GET /game/statistics ──────────────────────────────────────────────
        // Returns aggregated lifetime statistics for the authenticated user.
        get("/statistics") {
            val userId = call.jwtUserId()
            val stats  = statisticsRepository.getPlayerStatistics(userId)

            call.respond(stats)
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
