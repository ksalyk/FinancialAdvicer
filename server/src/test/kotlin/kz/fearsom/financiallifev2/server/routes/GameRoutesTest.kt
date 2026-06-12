package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.server.models.GameStateResponse
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.GameRepository
import kz.fearsom.financiallifev2.server.repository.GameSessionRow
import kz.fearsom.financiallifev2.server.repository.GameSnapshotRow
import kz.fearsom.financiallifev2.server.repository.PlayerStatisticsDto
import kz.fearsom.financiallifev2.server.repository.RecordSessionRequest
import kz.fearsom.financiallifev2.server.repository.StatisticsRepository
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for /api/v1/game/ endpoints using in-memory mocks.
 *
 * All game routes are JWT-protected. Each test generates a real JWT via
 * [generateAccessJwt] (same helper used in production) and passes it in the
 * Authorization header — no database needed.
 *
 * The test user id is fixed at [TEST_USER_ID] so mocks are easy to inspect.
 */
class GameRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Test app setup ────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp(
        gameRepo: GameRepository,
        statsRepo: StatisticsRepository = MockStatisticsRepository()
    ) {
        application {
            configureSerialization()
            configureSecurity()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    authenticate("auth-jwt") {
                        gameRoutes(gameRepo, statsRepo)
                    }
                }
            }
        }
    }

    private fun ApplicationTestBuilder.testClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    /** Bearer token for [TEST_USER_ID], valid for 15 min — sufficient for any test run. */
    private val testToken: String get() =
        generateAccessJwt(TEST_USER_ID, "testuser")

    private fun HttpRequestBuilder.authHeader() {
        header(HttpHeaders.Authorization, "Bearer $testToken")
    }

    // ── GET /game/character ───────────────────────────────────────────────────

    @Test
    fun `GET game_character - returns character JSON for default params`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/character") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"name\""))
        assertTrue(body.contains("\"initialState\""))
    }

    @Test
    fun `GET game_character - rejects unknown character`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/character?characterId=unknown&eraId=kz_2024") {
            authHeader()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Unsupported characterId='unknown'"))
    }

    @Test
    fun `GET game_character - rejects unsupported character era pair`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/character?characterId=asan&eraId=kz_2005") {
            authHeader()
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Unsupported characterId='asan' for eraId='kz_2005'"))
    }

    // ── POST /game/start ──────────────────────────────────────────────────────

    @Test
    fun `POST game_start - creates session in repository`() = testApplication {
        val repo = MockGameRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.post("/api/v1/game/start") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"characterId":"asan","eraId":"kz_2024","characterName":"Asan"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertNotNull(repo.loadSession(TEST_USER_ID), "Session should be persisted after /start")
    }

    @Test
    fun `POST game_start - rejects unknown character and era pair`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.post("/api/v1/game/start") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"characterId":"ghost","eraId":"kz_9999","characterName":"Ghost"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST game_start - rejects unknown era`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.post("/api/v1/game/start") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"characterId":"asan","eraId":"kz_9999","characterName":"Asan"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Unsupported eraId='kz_9999'"))
    }

    // ── GET /game/event ───────────────────────────────────────────────────────

    @Test
    fun `GET game_event - returns event after game started`() = testApplication {
        val repo = MockGameRepository()
        setupApp(repo)
        val client = testClient()

        // Start first to populate the session
        client.post("/api/v1/game/start") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"characterId":"asan","eraId":"kz_2024","characterName":"Asan"}""")
        }

        val response = client.get("/api/v1/game/event") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"eventId\""))
        assertTrue(body.contains("\"options\""))
    }

    @Test
    fun `GET game_event - returns 404 when no active session`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/event") { authHeader() }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── POST /game/choose/{optionId} ──────────────────────────────────────────

    @Test
    fun `POST game_choose - returns 404 when no active session`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.post("/api/v1/game/choose/some_option") { authHeader() }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── POST /game/save ───────────────────────────────────────────────────────

    @Test
    fun `POST game_save - stores snapshot and returns snapshotId`() = testApplication {
        val repo = MockGameRepository()
        setupApp(repo)
        val client = testClient()

        // Create a minimal valid GameState JSON
        val stateJson = minimalGameStateJson()

        val response = client.post("/api/v1/game/save") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"userId":"ignored","stateJson":${json.encodeToString(kotlinx.serialization.json.JsonPrimitive(stateJson))},"slotName":"slot1"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("snapshotId"))
        assertTrue(body.contains("savedAt"))
    }

    // ── GET /game/load ────────────────────────────────────────────────────────

    @Test
    fun `GET game_load - returns found=false when no session`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/load") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<GameStateResponse>(response.bodyAsText())
        assertEquals(false, body.found)
    }

    @Test
    fun `GET game_load - returns found=true after game started`() = testApplication {
        val repo = MockGameRepository()
        setupApp(repo)
        val client = testClient()

        client.post("/api/v1/game/start") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"characterId":"asan","eraId":"kz_2024","characterName":"Asan"}""")
        }

        val response = client.get("/api/v1/game/load") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<GameStateResponse>(response.bodyAsText())
        assertEquals(true, body.found)
        assertTrue(body.stateJson.isNotBlank())
    }

    // ── GET /game/snapshots ───────────────────────────────────────────────────

    @Test
    fun `GET game_snapshots - returns empty list when no snapshots`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/snapshots") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    // ── POST /game/restore ────────────────────────────────────────────────────

    @Test
    fun `POST game_restore - returns 404 for unknown snapshotId`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.post("/api/v1/game/restore/nonexistent") { authHeader() }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── GET /game/health ──────────────────────────────────────────────────────

    @Test
    fun `GET game_health - returns ok`() = testApplication {
        setupApp(MockGameRepository())
        val client = testClient()

        val response = client.get("/api/v1/game/health") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("ok"))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Minimal serialized GameState accepted by the server. */
    private fun minimalGameStateJson(): String =
        """{"playerState":{"characterId":"asan","eraId":"kz_2024","capital":0,"income":0,"expenses":0,"debt":0,"debtPayment":0,"investments":0,"stress":50,"financialKnowledge":10,"riskLevel":10,"year":2024,"month":1,"flags":[],"pendingEvents":[]},"currentEventId":"start","characterName":"Asan","messages":[],"isWaitingForChoice":false,"gameOver":false}"""

    companion object {
        const val TEST_USER_ID = "test-user-001"
    }
}

// ── In-memory mocks ───────────────────────────────────────────────────────────

internal class MockGameRepository : GameRepository {

    private val sessions  = ConcurrentHashMap<String, GameSessionRow>()
    private val snapshots = ConcurrentHashMap<String, GameSnapshotRow>()

    override suspend fun loadSession(userId: String): GameSessionRow? = sessions[userId]

    override suspend fun upsertSession(userId: String, stateJson: String, currentEventId: String) {
        sessions[userId] = GameSessionRow(userId, stateJson, currentEventId, System.currentTimeMillis())
    }

    override suspend fun deleteSession(userId: String) { sessions.remove(userId) }

    override suspend fun saveSnapshot(userId: String, stateJson: String, slotName: String): GameSnapshotRow {
        val snap = GameSnapshotRow(
            snapshotId = UUID.randomUUID().toString(),
            userId     = userId,
            slotName   = slotName,
            stateJson  = stateJson,
            savedAt    = System.currentTimeMillis()
        )
        snapshots[snap.snapshotId] = snap
        return snap
    }

    override suspend fun listSnapshots(userId: String, limit: Int): List<GameSnapshotRow> =
        snapshots.values.filter { it.userId == userId }.sortedByDescending { it.savedAt }.take(limit)

    override suspend fun loadSnapshot(snapshotId: String): GameSnapshotRow? = snapshots[snapshotId]

    override suspend fun deleteSnapshot(snapshotId: String) { snapshots.remove(snapshotId) }
}

internal class MockStatisticsRepository : StatisticsRepository {

    private val records = ConcurrentHashMap<String, RecordSessionRequest>()

    override suspend fun recordSession(userId: String, request: RecordSessionRequest): String {
        val id = UUID.randomUUID().toString()
        records[id] = request
        return id
    }

    override suspend fun getPlayerStatistics(userId: String): PlayerStatisticsDto =
        PlayerStatisticsDto(
            totalGamesPlayed    = 0,
            gamesCompleted      = 0,
            bestEnding          = null,
            averageCapitalAtEnd = 0L,
            mostPlayedEraId     = null,
            endingDistribution  = emptyMap(),
            perCharacter        = emptyList(),
            perEra              = emptyList()
        )

    override suspend fun deleteByCharacterId(characterId: String): Int = 0
    override suspend fun deleteByEraId(eraId: String): Int = 0
    override suspend fun deleteByCharacterIdForUser(userId: String, characterId: String): Int = 0
}
