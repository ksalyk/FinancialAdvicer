package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.admin.AdminUserDetailRow
import kz.fearsom.financiallifev2.admin.AdminUserListResponse
import kz.fearsom.financiallifev2.server.plugins.configureAdminSession
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the user admin endpoints:
 *   GET    /admin/users
 *   GET    /admin/users/{id}
 *   POST   /admin/users/{id}/reset-password
 *   DELETE /admin/users/{id}
 *
 * Auth is done via ADMIN_KEY Bearer token (simpler than session cookies for
 * these tests; both paths are exercised by AdminAuthRoutesTest).
 *
 * All state lives in a fresh [MockUserRepository] per test.
 */
class AdminUserRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── App setup ─────────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp(
        userRepo: MockUserRepository = MockUserRepository(),
        statsRepo: MockStatisticsRepository = MockStatisticsRepository()
    ) {
        application {
            configureAdminSession()
            configureSerialization()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    adminUserRoutes(userRepo, statsRepo)
                }
            }
        }
    }

    private fun ApplicationTestBuilder.testClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    /** Sets the ADMIN_KEY Bearer token for every request. */
    private fun HttpRequestBuilder.adminAuth() =
        header(HttpHeaders.Authorization, "Bearer dev-admin-key")

    // ── GET /admin/users ──────────────────────────────────────────────────────

    @Test
    fun `GET admin_users - returns empty list when no users exist`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/users") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = json.decodeFromString<AdminUserListResponse>(res.bodyAsText())
        assertEquals(0L, body.total)
        assertTrue(body.items.isEmpty())
    }

    @Test
    fun `GET admin_users - returns 401 without auth`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/users")

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `GET admin_users - lists created users`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        repo.create("alice", "password1")
        repo.create("bob", "password2")

        val res = client.get("/api/v1/admin/users") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = json.decodeFromString<AdminUserListResponse>(res.bodyAsText())
        assertEquals(2L, body.total)
        assertEquals(2, body.items.size)
    }

    @Test
    fun `GET admin_users - search filters by username`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        repo.create("alice", "pass")
        repo.create("bobby", "pass")
        repo.create("carol", "pass")

        val res = client.get("/api/v1/admin/users?search=bob") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = json.decodeFromString<AdminUserListResponse>(res.bodyAsText())
        assertEquals(1, body.items.size)
        assertEquals("bobby", body.items.first().username)
    }

    @Test
    fun `GET admin_users - respects limit and offset`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        repeat(5) { repo.create("user$it", "pass") }

        val res = client.get("/api/v1/admin/users?limit=2&offset=0") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = json.decodeFromString<AdminUserListResponse>(res.bodyAsText())
        assertEquals(5L, body.total)
        assertEquals(2, body.items.size)
    }

    // ── GET /admin/users/{id} ─────────────────────────────────────────────────

    @Test
    fun `GET admin_users_id - returns detail for existing user`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        val user = repo.create("detailuser", "pass")

        val res = client.get("/api/v1/admin/users/${user.id}") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        val body = json.decodeFromString<AdminUserDetailRow>(res.bodyAsText())
        assertEquals(user.id, body.id)
        assertEquals("detailuser", body.username)
    }

    @Test
    fun `GET admin_users_id - returns 404 for unknown id`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/users/nonexistent-id") { adminAuth() }

        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    // ── POST /admin/users/{id}/reset-password ─────────────────────────────────

    @Test
    fun `POST admin_users_reset-password - updates password`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        val user = repo.create("pwuser", "oldpassword")
        val oldHash = repo.findById(user.id)!!.passwordHash

        val res = client.post("/api/v1/admin/users/${user.id}/reset-password") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"password":"newpassword123"}""")
        }

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("true"))
        val newHash = repo.findById(user.id)!!.passwordHash
        assertFalse(newHash == oldHash, "Password hash should change after reset")
    }

    @Test
    fun `POST admin_users_reset-password - rejects password shorter than 6 chars`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        val user = repo.create("shortpw", "pass")

        val res = client.post("/api/v1/admin/users/${user.id}/reset-password") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"password":"abc"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, res.status)
    }

    @Test
    fun `POST admin_users_reset-password - returns 404 for unknown user`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/users/ghost/reset-password") {
            adminAuth()
            contentType(ContentType.Application.Json)
            setBody("""{"password":"newpassword"}""")
        }

        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    // ── DELETE /admin/users/{id} ──────────────────────────────────────────────

    @Test
    fun `DELETE admin_users_id - deletes existing user`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        val user = repo.create("todelete", "pass")

        val res = client.delete("/api/v1/admin/users/${user.id}") { adminAuth() }

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("true"))
        assertEquals(null, repo.findById(user.id), "User should be gone after cascade delete")
    }

    @Test
    fun `DELETE admin_users_id - returns 404 for unknown user`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.delete("/api/v1/admin/users/nobody") { adminAuth() }

        assertEquals(HttpStatusCode.NotFound, res.status)
    }

    @Test
    fun `DELETE admin_users_id - returns 401 without auth`() = testApplication {
        val repo = MockUserRepository()
        setupApp(userRepo = repo)
        val client = testClient()

        val user = repo.create("victim", "pass")

        val res = client.delete("/api/v1/admin/users/${user.id}")

        assertEquals(HttpStatusCode.Unauthorized, res.status)
        // User should still exist
        assertEquals("victim", repo.findById(user.id)?.username)
    }
}
