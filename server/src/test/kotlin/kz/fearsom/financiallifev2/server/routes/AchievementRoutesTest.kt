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
import kz.fearsom.financiallifev2.achievements.AchievementCatalog
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.achievements.UnlockAchievementsResponse
import kz.fearsom.financiallifev2.achievements.UserAchievementsResponse
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.AchievementsRepository
import org.junit.Test
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for /api/v1/achievements endpoints using an in-memory mock repo.
 * Same JWT approach as [GameRoutesTest]: real token via [generateAccessJwt].
 */
class AchievementRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Test app setup ────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp(repo: AchievementsRepository) {
        application {
            configureSerialization()
            configureSecurity()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    authenticate("auth-jwt") {
                        achievementRoutes(repo)
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

    private val testToken: String get() = generateAccessJwt(TEST_USER_ID, "testuser")

    private fun HttpRequestBuilder.authHeader() {
        header(HttpHeaders.Authorization, "Bearer $testToken")
    }

    // ── GET /achievements ─────────────────────────────────────────────────────

    @Test
    fun `GET achievements - returns empty list for fresh user`() = testApplication {
        setupApp(MockAchievementsRepository())
        val client = testClient()

        val response = client.get("/api/v1/achievements") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<UserAchievementsResponse>(response.bodyAsText())
        assertTrue(body.unlocks.isEmpty())
    }

    @Test
    fun `GET achievements - requires auth`() = testApplication {
        setupApp(MockAchievementsRepository())
        val client = testClient()

        val response = client.get("/api/v1/achievements")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ── POST /achievements/unlock ─────────────────────────────────────────────

    @Test
    fun `POST unlock - inserts new achievements and is idempotent`() = testApplication {
        val repo = MockAchievementsRepository()
        setupApp(repo)
        val client = testClient()

        val body = """{"unlocks":[
            {"achievementId":"${AchievementCatalog.FIRST_STEPS}","unlockedAt":1000,"sourceCharacterId":"daniyar","sourceEraId":"kz_2024"},
            {"achievementId":"${AchievementCatalog.SCAM_PONZI}","unlockedAt":2000}
        ]}"""

        val first = client.post("/api/v1/achievements/unlock") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        assertEquals(HttpStatusCode.OK, first.status)
        val firstBody = json.decodeFromString<UnlockAchievementsResponse>(first.bodyAsText())
        assertEquals(2, firstBody.inserted)
        assertEquals(2, firstBody.unlocks.size)

        // Replay the exact same batch — nothing new inserted, list unchanged.
        val second = client.post("/api/v1/achievements/unlock") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val secondBody = json.decodeFromString<UnlockAchievementsResponse>(second.bodyAsText())
        assertEquals(0, secondBody.inserted)
        assertEquals(2, secondBody.unlocks.size)
    }

    @Test
    fun `POST unlock - rejects unknown achievement ids`() = testApplication {
        setupApp(MockAchievementsRepository())
        val client = testClient()

        val response = client.post("/api/v1/achievements/unlock") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"unlocks":[{"achievementId":"scam.totally_fake","unlockedAt":1000}]}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("scam.totally_fake"))
    }

    @Test
    fun `POST unlock - rejects oversized batch`() = testApplication {
        setupApp(MockAchievementsRepository())
        val client = testClient()

        val entries = (1..101).joinToString(",") {
            """{"achievementId":"${AchievementCatalog.FIRST_STEPS}","unlockedAt":$it}"""
        }
        val response = client.post("/api/v1/achievements/unlock") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"unlocks":[$entries]}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST unlock - first unlock wins on replay with different metadata`() = testApplication {
        val repo = MockAchievementsRepository()
        setupApp(repo)
        val client = testClient()

        client.post("/api/v1/achievements/unlock") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"unlocks":[{"achievementId":"${AchievementCatalog.CUSHION}","unlockedAt":1000,"sourceCharacterId":"daniyar"}]}""")
        }
        val replay = client.post("/api/v1/achievements/unlock") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"unlocks":[{"achievementId":"${AchievementCatalog.CUSHION}","unlockedAt":9999,"sourceCharacterId":"aigul"}]}""")
        }

        val body = json.decodeFromString<UnlockAchievementsResponse>(replay.bodyAsText())
        val record = body.unlocks.single()
        assertEquals(1000, record.unlockedAt)
        assertEquals("daniyar", record.sourceCharacterId)
    }

    // ── POST /achievements/{id}/feedback ──────────────────────────────────────

    @Test
    fun `POST feedback - stores vote and clear works`() = testApplication {
        val repo = MockAchievementsRepository()
        setupApp(repo)
        val client = testClient()

        val up = client.post("/api/v1/achievements/${AchievementCatalog.SCAM_PONZI}/feedback") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"vote":"up"}""")
        }
        assertEquals(HttpStatusCode.OK, up.status)
        assertEquals("up", repo.feedback[TEST_USER_ID to AchievementCatalog.SCAM_PONZI])

        val clear = client.post("/api/v1/achievements/${AchievementCatalog.SCAM_PONZI}/feedback") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"vote":null}""")
        }
        assertEquals(HttpStatusCode.OK, clear.status)
        assertTrue((TEST_USER_ID to AchievementCatalog.SCAM_PONZI) !in repo.feedback)
    }

    @Test
    fun `POST feedback - rejects invalid vote value`() = testApplication {
        setupApp(MockAchievementsRepository())
        val client = testClient()

        val response = client.post("/api/v1/achievements/${AchievementCatalog.SCAM_PONZI}/feedback") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"vote":"meh"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST feedback - 404 for unknown achievement`() = testApplication {
        setupApp(MockAchievementsRepository())
        val client = testClient()

        val response = client.post("/api/v1/achievements/scam.totally_fake/feedback") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"vote":"up"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    // ── GET /achievements/feedback ────────────────────────────────────────────

    @Test
    fun `GET feedback - returns stored votes`() = testApplication {
        val repo = MockAchievementsRepository()
        setupApp(repo)
        val client = testClient()

        client.post("/api/v1/achievements/${AchievementCatalog.SCAM_PONZI}/feedback") {
            authHeader()
            contentType(ContentType.Application.Json)
            setBody("""{"vote":"down"}""")
        }

        val response = client.get("/api/v1/achievements/feedback") { authHeader() }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"${AchievementCatalog.SCAM_PONZI}\":\"down\""))
    }

    companion object {
        const val TEST_USER_ID = "test-user-ach-001"
    }
}

// ── In-memory mock ─────────────────────────────────────────────────────────────

internal class MockAchievementsRepository : AchievementsRepository {

    private val unlocks = ConcurrentHashMap<Pair<String, String>, AchievementUnlockDto>()
    val feedback = ConcurrentHashMap<Pair<String, String>, String>()

    override suspend fun listUnlocks(userId: String): List<AchievementUnlockDto> =
        unlocks.filterKeys { it.first == userId }.values.sortedBy { it.unlockedAt }

    override suspend fun unlock(userId: String, unlocks: List<AchievementUnlockDto>): Int {
        var inserted = 0
        unlocks.distinctBy { it.achievementId }.forEach { dto ->
            val key = userId to dto.achievementId
            if (this.unlocks.putIfAbsent(key, dto) == null) inserted++
        }
        return inserted
    }

    override suspend fun setFeedback(userId: String, achievementId: String, vote: String?) {
        val key = userId to achievementId
        if (vote == null) feedback.remove(key) else feedback[key] = vote
    }

    override suspend fun listFeedback(userId: String): Map<String, String> =
        feedback.filterKeys { it.first == userId }
            .entries.associate { (k, v) -> k.second to v }
}
