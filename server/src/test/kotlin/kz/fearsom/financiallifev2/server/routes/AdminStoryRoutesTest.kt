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
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryRow
import kz.fearsom.financiallifev2.admin.StoryStatus
import kz.fearsom.financiallifev2.admin.StoryValidationReport
import kz.fearsom.financiallifev2.admin.UpsertStoryRequest
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.server.database.DatabaseTestFixture
import kz.fearsom.financiallifev2.server.plugins.ADMIN_COMBINED_AUTH
import kz.fearsom.financiallifev2.server.plugins.configureAdminSession
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.StoriesRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the story CRUD + moderation endpoints (AdminStoryRoutes) and the
 * public published-stories catalog.
 *
 * H2 in-memory DB via [DatabaseTestFixture]; auth via ADMIN_KEY Bearer.
 */
class AdminStoryRoutesTest {

    private val db    = DatabaseTestFixture.database
    private val repo  = StoriesRepository(db)
    private val json  = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Before
    fun setup() = DatabaseTestFixture.reset()

    // ── Fixtures ──────────────────────────────────────────────────────────────

    /** Minimal VALID graph: intro → ending. */
    private fun validGraph() = ScenarioGraphDto(
        initialPlayerState = PlayerState(characterId = "test_char", eraId = "kz_2024"),
        events = listOf(
            GameEvent(
                id      = "intro",
                message = "Start",
                options = listOf(GameOption(id = "go", text = "Go", emoji = "🚀", next = "the_end"))
            ),
            GameEvent(
                id         = "the_end",
                message    = "Done",
                options    = emptyList(),
                isEnding   = true,
                endingType = EndingType.FINANCIAL_STABILITY
            )
        ),
        conditionalEvents = emptyList(),
        eventPool         = emptyList()
    )

    /** INVALID graph: non-ending event with no options (dead-end ERROR). */
    private fun brokenGraph() = validGraph().copy(
        events = listOf(
            GameEvent(id = "intro", message = "Stuck", options = emptyList())
        )
    )

    /** INVALID for a DB story: a valid tree whose root is 'start', not 'intro'. */
    private fun noIntroGraph() = validGraph().copy(
        events = listOf(
            GameEvent(
                id      = "start",
                message = "Start",
                options = listOf(GameOption(id = "go", text = "Go", emoji = "🚀", next = "the_end"))
            ),
            GameEvent(
                id = "the_end", message = "Done", options = emptyList(),
                isEnding = true, endingType = EndingType.FINANCIAL_STABILITY
            )
        )
    )

    /** INVALID for a DB story: an option points at an event that doesn't exist. */
    private fun danglingTargetGraph() = validGraph().copy(
        events = listOf(
            GameEvent(
                id      = "intro",
                message = "Start",
                options = listOf(GameOption(id = "go", text = "Go", emoji = "🚀", next = "missing_event"))
            ),
            GameEvent(
                id = "the_end", message = "Done", options = emptyList(),
                isEnding = true, endingType = EndingType.FINANCIAL_STABILITY
            )
        )
    )

    private fun upsertRequest(
        id: String = "test_story",
        graph: ScenarioGraphDto = validGraph()
    ) = UpsertStoryRequest(
        id          = id,
        title       = "Test story",
        description = "desc",
        characterId = "test_char",
        eraId       = "kz_2024",
        graph       = graph
    )

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
                        adminStoryRoutes(repo)
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

    private fun HttpRequestBuilder.jsonBody(body: Any) {
        contentType(ContentType.Application.Json)
        setBody(json.encodeToString(UpsertStoryRequest.serializer(), body as UpsertStoryRequest))
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @Test
    fun `POST stories - creates a DRAFT`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }

        assertEquals(HttpStatusCode.Created, res.status)
        val detail = json.decodeFromString<StoryDetail>(res.bodyAsText())
        assertEquals(StoryStatus.DRAFT, detail.row.status)
        assertEquals(2, detail.row.eventCount)
        assertEquals(1, detail.row.endingCount)
    }

    @Test
    fun `POST stories - 409 on duplicate id`() = testApplication {
        setupApp()
        val client = testClient()

        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }
        val dup = client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }

        assertEquals(HttpStatusCode.Conflict, dup.status)
    }

    @Test
    fun `POST stories - 400 on invalid id format`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/stories") {
            adminAuth(); jsonBody(upsertRequest(id = "Bad Id!"))
        }

        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `GET stories - requires auth`() = testApplication {
        setupApp()
        val client = testClient()

        assertEquals(HttpStatusCode.Unauthorized, client.get("/api/v1/admin/stories").status)
    }

    @Test
    fun `GET stories - lists created rows and filters by status`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }

        val all = json.decodeFromString<List<StoryRow>>(
            client.get("/api/v1/admin/stories") { adminAuth() }.bodyAsText()
        )
        assertEquals(1, all.size)

        val published = json.decodeFromString<List<StoryRow>>(
            client.get("/api/v1/admin/stories?status=published") { adminAuth() }.bodyAsText()
        )
        assertTrue(published.isEmpty())
    }

    // ── Validation + publish gate ─────────────────────────────────────────────

    @Test
    fun `POST validate - reports dead-end as error`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") {
            adminAuth(); jsonBody(upsertRequest(graph = brokenGraph()))
        }

        val res = client.post("/api/v1/admin/stories/test_story/validate") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val report = json.decodeFromString<StoryValidationReport>(res.bodyAsText())
        assertTrue(report.errors > 0)
    }

    @Test
    fun `POST publish - rejects broken graph with 422`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") {
            adminAuth(); jsonBody(upsertRequest(graph = brokenGraph()))
        }

        val res = client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
    }

    @Test
    fun `POST publish - publishes valid DRAFT`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }

        val res = client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val row = json.decodeFromString<StoryRow>(res.bodyAsText())
        assertEquals(StoryStatus.PUBLISHED, row.status)
        assertNotNull(row.publishedAt)
    }

    @Test
    fun `POST publish - rejects graph without intro (422)`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") {
            adminAuth(); jsonBody(upsertRequest(graph = noIntroGraph()))
        }

        val res = client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
    }

    @Test
    fun `POST publish - rejects unresolved option target (422)`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") {
            adminAuth(); jsonBody(upsertRequest(graph = danglingTargetGraph()))
        }

        val res = client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
    }

    @Test
    fun `PUT stories - editing a PUBLISHED story with broken graph returns 422`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }
        client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        val res = client.put("/api/v1/admin/stories/test_story") {
            adminAuth(); jsonBody(upsertRequest(graph = brokenGraph()))
        }

        assertEquals(HttpStatusCode.UnprocessableEntity, res.status)
    }

    // ── Moderation transitions ────────────────────────────────────────────────

    @Test
    fun `submit then reject stores review note`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }

        val submitted = client.post("/api/v1/admin/stories/test_story/submit") { adminAuth() }
        assertEquals(StoryStatus.PENDING_REVIEW, json.decodeFromString<StoryRow>(submitted.bodyAsText()).status)

        val rejected = client.post("/api/v1/admin/stories/test_story/reject") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"note":"Слишком мало развилок"}""")
        }
        val row = json.decodeFromString<StoryRow>(rejected.bodyAsText())
        assertEquals(StoryStatus.REJECTED, row.status)
        assertEquals("Слишком мало развилок", row.reviewNote)
    }

    @Test
    fun `illegal transition returns 409`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }
        client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        // PUBLISHED → PENDING_REVIEW is not allowed
        val res = client.post("/api/v1/admin/stories/test_story/submit") { adminAuth() }

        assertEquals(HttpStatusCode.Conflict, res.status)
    }

    @Test
    fun `unpublish archives and republish works`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }
        client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }

        val archived = client.post("/api/v1/admin/stories/test_story/unpublish") { adminAuth() }
        assertEquals(StoryStatus.ARCHIVED, json.decodeFromString<StoryRow>(archived.bodyAsText()).status)

        val republished = client.post("/api/v1/admin/stories/test_story/publish") { adminAuth() }
        assertEquals(StoryStatus.PUBLISHED, json.decodeFromString<StoryRow>(republished.bodyAsText()).status)
    }

    // ── Clone built-in ────────────────────────────────────────────────────────

    @Test
    fun `clone built-in era graph creates editable draft`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/stories/clone/somebody/kz_2024") { adminAuth() }

        assertEquals(HttpStatusCode.Created, res.status)
        val detail = json.decodeFromString<StoryDetail>(res.bodyAsText())
        assertEquals(StoryStatus.DRAFT, detail.row.status)
        assertTrue(detail.graph.events.isNotEmpty())
    }

    @Test
    fun `DELETE stories - removes the row`() = testApplication {
        setupApp()
        val client = testClient()
        client.post("/api/v1/admin/stories") { adminAuth(); jsonBody(upsertRequest()) }

        assertEquals(HttpStatusCode.OK, client.delete("/api/v1/admin/stories/test_story") { adminAuth() }.status)
        assertEquals(HttpStatusCode.NotFound, client.get("/api/v1/admin/stories/test_story") { adminAuth() }.status)
    }
}
