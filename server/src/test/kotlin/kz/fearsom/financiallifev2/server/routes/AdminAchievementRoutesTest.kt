package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.achievements.AchievementCatalog
import kz.fearsom.financiallifev2.achievements.AchievementCondition
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.achievements.AchievementKind
import kz.fearsom.financiallifev2.achievements.AchievementRarity
import kz.fearsom.financiallifev2.achievements.LocalizedText
import kz.fearsom.financiallifev2.admin.AchievementAdminRow
import kz.fearsom.financiallifev2.admin.UpsertAchievementRequest
import kz.fearsom.financiallifev2.admin.UserAchievementsAdminResponse
import kz.fearsom.financiallifev2.server.database.DatabaseTestFixture
import kz.fearsom.financiallifev2.server.plugins.ADMIN_COMBINED_AUTH
import kz.fearsom.financiallifev2.server.plugins.configureAdminSession
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.AchievementCatalogRepository
import kz.fearsom.financiallifev2.server.repository.DatabaseAchievementsRepository
import kz.fearsom.financiallifev2.server.repository.DatabaseUserRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the achievement catalog admin endpoints + per-user grant/revoke.
 * H2 in-memory DB via [DatabaseTestFixture]; auth via ADMIN_KEY Bearer.
 */
class AdminAchievementRoutesTest {

    private val db          = DatabaseTestFixture.database
    private val catalogRepo = AchievementCatalogRepository(db)
    private val unlockRepo  = DatabaseAchievementsRepository(db)
    private val userRepo    = DatabaseUserRepository(db)
    private val json        = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setup() {
        DatabaseTestFixture.reset()
        runBlocking { catalogRepo.seedMissing(AchievementCatalog.all) }
    }

    // ── App setup ─────────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp() {
        application {
            configureAdminSession()
            configureSerialization()
            configureSecurity()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    authenticate(ADMIN_COMBINED_AUTH) {
                        adminAchievementRoutes(catalogRepo, unlockRepo, userRepo)
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

    private fun HttpRequestBuilder.adminAuth() =
        header(HttpHeaders.Authorization, "Bearer ${System.getenv("ADMIN_KEY") ?: "dev-admin-key"}")

    private fun customDefinition(id: String = "game.custom_test") = AchievementDefinition(
        id        = id,
        kind      = AchievementKind.GAME,
        emoji     = "🏆",
        rarity    = AchievementRarity.RARE,
        condition = AchievementCondition.EmergencyFund(months = 3),
        titleText = LocalizedText(ru = "Тестовая ачивка"),
        descText  = LocalizedText(ru = "Сделай х")
    )

    // ── Catalog ───────────────────────────────────────────────────────────────

    @Test
    fun `GET achievements - returns seeded catalog with stats`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/achievements") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val rows = json.decodeFromString<List<AchievementAdminRow>>(res.bodyAsText())
        assertEquals(AchievementCatalog.all.size, rows.size)
        assertTrue(rows.all { it.source == "SEEDED" })
        assertTrue(rows.all { it.unlockCount == 0L })
    }

    @Test
    fun `GET achievements - requires auth`() = testApplication {
        setupApp()
        val client = testClient()

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/admin/achievements").status)
    }

    @Test
    fun `POST achievements - creates CUSTOM definition with inline texts`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/achievements") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(
                UpsertAchievementRequest.serializer(),
                UpsertAchievementRequest(definition = customDefinition(), sortOrder = 99)
            ))
        }

        assertEquals(HttpStatusCode.OK, res.status)
        val row = json.decodeFromString<AchievementAdminRow>(res.bodyAsText())
        assertEquals("CUSTOM", row.source)
        assertEquals("Тестовая ачивка", row.definition.titleText?.ru)
        // Condition survives the JSON round-trip with its type discriminator.
        assertTrue(row.definition.condition is AchievementCondition.EmergencyFund)
    }

    @Test
    fun `POST achievements - rejects definition without title`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/achievements") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(
                UpsertAchievementRequest.serializer(),
                UpsertAchievementRequest(definition = customDefinition().copy(titleText = null))
            ))
        }

        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `DELETE achievements - SEEDED is protected, CUSTOM is deletable`() = testApplication {
        setupApp()
        val client = testClient()

        val seeded = client.delete("/api/v1/admin/achievements/${AchievementCatalog.FIRST_STEPS}") { adminAuth() }
        assertEquals(HttpStatusCode.Conflict, seeded.status)

        client.post("/api/v1/admin/achievements") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(
                UpsertAchievementRequest.serializer(),
                UpsertAchievementRequest(definition = customDefinition())
            ))
        }
        val custom = client.delete("/api/v1/admin/achievements/game.custom_test") { adminAuth() }
        assertEquals(HttpStatusCode.OK, custom.status)
    }

    @Test
    fun `activate and deactivate toggle catalog visibility`() = testApplication {
        setupApp()
        val client = testClient()
        val id = AchievementCatalog.CUSHION

        assertEquals(
            HttpStatusCode.OK,
            client.post("/api/v1/admin/achievements/$id/deactivate") { adminAuth() }.status
        )
        val active = runBlocking { catalogRepo.listActiveDefinitions() }
        assertTrue(active.none { it.id == id })

        client.post("/api/v1/admin/achievements/$id/activate") { adminAuth() }
        val reActivated = runBlocking { catalogRepo.listActiveDefinitions() }
        assertTrue(reActivated.any { it.id == id })
    }

    // ── Per-user grant / revoke ───────────────────────────────────────────────

    @Test
    fun `grant and revoke user achievement`() = testApplication {
        setupApp()
        val client = testClient()
        val user = runBlocking { userRepo.create("achuser", "password123") }
        val achId = AchievementCatalog.DEBT_FREE

        val grant = client.post("/api/v1/admin/users/${user.id}/achievements/$achId") { adminAuth() }
        assertEquals(HttpStatusCode.OK, grant.status)

        val list = client.get("/api/v1/admin/users/${user.id}/achievements") { adminAuth() }
        val body = json.decodeFromString<UserAchievementsAdminResponse>(list.bodyAsText())
        assertEquals(listOf(achId), body.unlocks.map { it.achievementId })

        val revoke = client.delete("/api/v1/admin/users/${user.id}/achievements/$achId") { adminAuth() }
        assertEquals(HttpStatusCode.OK, revoke.status)

        val after = json.decodeFromString<UserAchievementsAdminResponse>(
            client.get("/api/v1/admin/users/${user.id}/achievements") { adminAuth() }.bodyAsText()
        )
        assertTrue(after.unlocks.isEmpty())
    }

    @Test
    fun `grant rejects unknown achievement id`() = testApplication {
        setupApp()
        val client = testClient()
        val user = runBlocking { userRepo.create("achuser2", "password123") }

        val res = client.post("/api/v1/admin/users/${user.id}/achievements/game.does_not_exist") { adminAuth() }

        assertEquals(HttpStatusCode.NotFound, res.status)
    }
}
