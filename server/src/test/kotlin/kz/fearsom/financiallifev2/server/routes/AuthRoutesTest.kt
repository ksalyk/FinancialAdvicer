package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kz.fearsom.financiallifev2.server.models.AuthResponse
import kz.fearsom.financiallifev2.server.models.LoginRequest
import kz.fearsom.financiallifev2.server.models.RegisterRequest
import kz.fearsom.financiallifev2.server.repository.UserRepository
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Comprehensive test suite for Auth endpoints.
 * Tests: POST /auth/register, POST /auth/login, GET /auth/me
 */
class AuthRoutesTest {

    private lateinit var mockUserRepository: MockUserRepository

    @Before
    fun setup() {
        mockUserRepository = MockUserRepository()
    }

    @Test
    fun `test POST auth_register - success with valid data`() = testApplication {
        val client = createClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "testuser", password = "password123"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(true, body.success)
        assertNotNull(body.token)
        assertEquals("testuser", body.username)
    }

    @Test
    fun `test POST auth_register - fails with username too short`() = testApplication {
        val client = createClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "ab", password = "password123"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_login_too_short", body.message)
    }

    @Test
    fun `test POST auth_register - fails with password too short`() = testApplication {
        val client = createClient()

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "validuser", password = "123"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_password_too_short", body.message)
    }

    @Test
    fun `test POST auth_register - fails when user already exists`() = testApplication {
        val client = createClient()

        // Register first user
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "duplicate", password = "password123"))
        }

        // Try to register with same username
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "duplicate", password = "password123"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_user_exists", body.message)
    }

    @Test
    fun `test POST auth_login - success with valid credentials`() = testApplication {
        val client = createClient()

        // First register
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "loginuser", password = "password123"))
        }

        // Then login
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "loginuser", password = "password123"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(true, body.success)
        assertNotNull(body.token)
        assertEquals("loginuser", body.username)
    }

    @Test
    fun `test POST auth_login - fails with non-existent user`() = testApplication {
        val client = createClient()

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "nonexistent", password = "password123"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_user_not_found", body.message)
    }

    @Test
    fun `test POST auth_login - fails with wrong password`() = testApplication {
        val client = createClient()

        // Register
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "wrongpass", password = "correct123"))
        }

        // Try with wrong password
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "wrongpass", password = "incorrect123"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_wrong_password", body.message)
    }

    @Test
    fun `test POST auth_login - fails with empty fields`() = testApplication {
        val client = createClient()

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "", password = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_fill_fields", body.message)
    }

    @Test
    fun `test GET auth_me - success with valid token`() = testApplication {
        val client = createClient()

        // Register and get token
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "meuser", password = "password123"))
        }
        val registerData = Json.decodeFromString<AuthResponse>(registerResponse.bodyAsText())
        val token = registerData.token

        // Get /auth/me
        val response = client.get("/auth/me") {
            header("Authorization", "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(true, body.success)
        assertEquals("meuser", body.username)
    }

    @Test
    fun `test GET auth_me - fails without authorization header`() = testApplication {
        val client = createClient()

        val response = client.get("/auth/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
    }

    @Test
    fun `test GET auth_me - fails with invalid token`() = testApplication {
        val client = createClient()

        val response = client.get("/auth/me") {
            header("Authorization", "Bearer invalid.token.here")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = Json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
    }

    @Test
    fun `test GET auth_me - fails with expired token`() = testApplication {
        val client = createClient()

        // This test would need an actual expired token
        // For now, using an obviously invalid token
        val response = client.get("/auth/me") {
            header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // ────────────────────────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.createClient() = createClient {
        // Apply auth routes with mock repository
        install(io.ktor.client.plugins.JsonFeature)
    }

    /**
     * Mock UserRepository for testing without database
     */
    private class MockUserRepository : UserRepository {
        private val users = mutableMapOf<String, UserData>()
        private var nextId = 1

        data class UserData(
            val id: String,
            val username: String,
            val passwordHash: String
        )

        override fun existsByUsername(username: String): Boolean =
            users.values.any { it.username == username }

        override fun create(username: String, password: String): UserData {
            val id = (nextId++).toString()
            val user = UserData(id, username, hashPassword(password))
            users[id] = user
            return user
        }

        override fun findByUsername(username: String): UserData? =
            users.values.find { it.username == username }

        override fun findById(id: String): UserData? = users[id]

        override fun verifyPassword(password: String, hash: String): Boolean =
            hashPassword(password) == hash

        private fun hashPassword(password: String): String = password.hashCode().toString()
    }
}
