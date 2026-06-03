package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.server.plugins.configureAdminSession
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for POST /admin/login, POST /admin/logout, GET /admin/me.
 *
 * Auth is cookie-session based. Login sets an httpOnly `admin_session` cookie;
 * subsequent requests carry it. Tests extract the Set-Cookie header manually
 * and pass it as a Cookie header on following calls.
 *
 * Credentials come from env vars with defaults:
 *   ADMIN_USERNAME = "admin"
 *   ADMIN_PASSWORD = "dev-admin-password"
 */
class AdminAuthRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── App setup ─────────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp() {
        application {
            configureAdminSession()
            configureSerialization()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    adminAuthRoutes()
                }
            }
        }
    }

    private fun ApplicationTestBuilder.testClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    /** Logs in and returns the raw Set-Cookie header value, or null on failure. */
    private suspend fun loginAndGetCookie(
        client: io.ktor.client.HttpClient,
        username: String = "admin",
        password: String = "dev-admin-password"
    ): String? {
        val res = client.post("/api/v1/admin/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"$username","password":"$password"}""")
        }
        return if (res.status == HttpStatusCode.OK)
            res.headers[HttpHeaders.SetCookie]
        else null
    }

    /** Extracts just the `name=value` portion from a Set-Cookie header. */
    private fun cookieHeader(setCookieValue: String) =
        setCookieValue.substringBefore(";")

    // ── POST /admin/login ─────────────────────────────────────────────────────

    @Test
    fun `POST admin_login - success with correct credentials`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","password":"dev-admin-password"}""")
        }

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("true"))
        assertNotNull(res.headers[HttpHeaders.SetCookie], "Session cookie should be set on login")
    }

    @Test
    fun `POST admin_login - fails with wrong password`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"admin","password":"wrong"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `POST admin_login - fails with wrong username`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.post("/api/v1/admin/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"username":"notadmin","password":"dev-admin-password"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    // ── GET /admin/me ─────────────────────────────────────────────────────────

    @Test
    fun `GET admin_me - returns 401 when not logged in`() = testApplication {
        setupApp()
        val client = testClient()

        val res = client.get("/api/v1/admin/me")

        assertEquals(HttpStatusCode.Unauthorized, res.status)
    }

    @Test
    fun `GET admin_me - returns username after successful login`() = testApplication {
        setupApp()
        val client = testClient()

        val cookie = loginAndGetCookie(client)
        assertNotNull(cookie, "Login should succeed and return a cookie")

        val res = client.get("/api/v1/admin/me") {
            header(HttpHeaders.Cookie, cookieHeader(cookie))
        }

        assertEquals(HttpStatusCode.OK, res.status)
        assertTrue(res.bodyAsText().contains("admin"))
    }

    // ── POST /admin/logout ────────────────────────────────────────────────────

    @Test
    fun `POST admin_logout - clears session`() = testApplication {
        setupApp()
        val client = testClient()

        val loginCookie = loginAndGetCookie(client)
        assertNotNull(loginCookie)

        // Verify session is active
        val beforeLogout = client.get("/api/v1/admin/me") {
            header(HttpHeaders.Cookie, cookieHeader(loginCookie))
        }
        assertEquals(HttpStatusCode.OK, beforeLogout.status)

        // Logout (session cookie included so server can clear it)
        val logoutRes = client.post("/api/v1/admin/logout") {
            header(HttpHeaders.Cookie, cookieHeader(loginCookie))
        }
        assertEquals(HttpStatusCode.OK, logoutRes.status)

        // The logout response's Set-Cookie should clear the session.
        // Using the cleared cookie on a /me call should return 401.
        val clearedCookie = logoutRes.headers[HttpHeaders.SetCookie]
        val cookieToSend = if (clearedCookie != null) cookieHeader(clearedCookie) else cookieHeader(loginCookie)

        val afterLogout = client.get("/api/v1/admin/me") {
            header(HttpHeaders.Cookie, cookieToSend)
        }
        assertEquals(HttpStatusCode.Unauthorized, afterLogout.status)
    }
}
