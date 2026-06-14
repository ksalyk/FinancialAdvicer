package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.admin.UpsertCharacterRequest
import kz.fearsom.financiallifev2.admin.UpsertEraRequest
import kz.fearsom.financiallifev2.server.database.DatabaseTestFixture
import kz.fearsom.financiallifev2.server.plugins.configureAdminSession
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.CharactersRepository
import kz.fearsom.financiallifev2.server.repository.ErasRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the scenario viewer endpoints:
 *   GET /admin/scenarios                       — list combos
 *   GET /admin/scenarios/{characterId}/{eraId} — full ScenarioGraphDto
 *
 * Uses the H2 in-memory database via [DatabaseTestFixture]. Characters and eras
 * are seeded before each test via their repositories.
 *
 * Auth via ADMIN_KEY Bearer token.
 */
class AdminScenarioRoutesTest {

    private val db             = DatabaseTestFixture.database
    private val characterRepo  = CharactersRepository(db)
    private val eraRepo        = ErasRepository(db)
    private val json           = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setup() = DatabaseTestFixture.reset()

    // ── Seed helpers ──────────────────────────────────────────────────────────

    private fun seedAsanData() = runBlocking {
        eraRepo.upsert(UpsertEraRequest(
            id                    = "kz_2024",
            name                  = "Казахстан 2024",
            description           = "Modern Kazakhstan",
            emoji                 = "🇰🇿",
            startYear             = 2020,
            endYear               = 2024,
            availableCharacterIds = listOf("asan"),
            isActive              = true
        ))
        characterRepo.upsert(UpsertCharacterRequest(
            id      = "asan",
            name    = "Асан",
            emoji   = "👨",
            type    = "PREDEFINED",
            eraIds  = listOf("kz_2024"),
            isActive = true
        ))
    }

    // ── App setup ─────────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp() {
        application {
            configureAdminSession()
            configureSerialization()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    adminScenarioRoutes(characterRepo, eraRepo)
                }
            }
        }
    }

    private fun ApplicationTestBuilder.testClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    private fun HttpRequestBuilder.adminAuth() =
        header(HttpHeaders.Authorization, "Bearer dev-admin-key")

    // ── GET /admin/scenarios ──────────────────────────────────────────────────

    @Test
    fun `GET admin_scenarios - returns empty list when no characters seeded`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("[]", res.bodyAsText().trim())
    }

    @Test
    fun `GET admin_scenarios - returns 401 without auth`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios")

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `GET admin_scenarios - returns combo after seeding asan + kz_2024`() = testApplication {
        seedAsanData()
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val combos = json.decodeFromString<List<ScenarioComboDto>>(res.bodyAsText())
        assertEquals(1, combos.size)
        assertEquals("asan",    combos.first().characterId)
        assertEquals("kz_2024", combos.first().eraId)
        assertTrue(combos.first().label.contains("Асан"))
    }

    @Test
    fun `GET admin_scenarios - excludes eraId that has no matching era row`() = testApplication {
        // Character references an era that doesn't exist in the DB
        runBlocking {
            characterRepo.upsert(UpsertCharacterRequest(
                id      = "ghost",
                name    = "Ghost",
                emoji   = "👻",
                type    = "PREDEFINED",
                eraIds  = listOf("nonexistent_era"),
                isActive = true
            ))
        }
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        assertEquals("[]", res.bodyAsText().trim())
    }

    // ── GET /admin/scenarios/{characterId}/{eraId} ────────────────────────────

    @Test
    fun `GET admin_scenarios_char_era - returns graph DTO for asan + kz_2024`() = testApplication {
        seedAsanData()
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios/asan/kz_2024") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val graph = json.decodeFromString<ScenarioGraphDto>(res.bodyAsText())
        assertNotNull(graph.initialPlayerState)
        assertEquals(1, graph.events.size)
        assertTrue(graph.events.first().isEnding, "Empty era intro should be terminal")
    }

    @Test
    fun `GET admin_scenarios_char_era - returns 404 for unknown characterId + eraId`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios/nobody/kz_9999") { adminAuth() }

        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `GET admin_scenarios_char_era - returns 401 without auth`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios/asan/kz_2024")

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `GET admin_scenarios_char_era - graph has empty event pool`() = testApplication {
        seedAsanData()
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/scenarios/asan/kz_2024") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = json.parseToJsonElement(res.bodyAsText()).jsonObject
        val pool = body["eventPool"]?.jsonArray
        assertNotNull(pool)
        assertEquals(0, pool.size, "Empty era graph should not expose pool entries")
    }
}
