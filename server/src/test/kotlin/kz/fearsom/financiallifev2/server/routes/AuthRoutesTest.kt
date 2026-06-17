package kz.fearsom.financiallifev2.server.routes

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.admin.AdminUserRow
import kz.fearsom.financiallifev2.server.auth.JwtConfig
import kz.fearsom.financiallifev2.server.auth.PasswordHasher
import kz.fearsom.financiallifev2.server.models.AuthResponse
import kz.fearsom.financiallifev2.server.models.LoginRequest
import kz.fearsom.financiallifev2.server.models.RegisterRequest
import kz.fearsom.financiallifev2.server.models.ServerUser
import kz.fearsom.financiallifev2.server.plugins.configureSecurity
import kz.fearsom.financiallifev2.server.plugins.configureSerialization
import kz.fearsom.financiallifev2.server.plugins.configureStatusPages
import kz.fearsom.financiallifev2.server.repository.UserRepository
import org.junit.Test
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for /api/v1/auth/ endpoints using an in-memory [MockUserRepository].
 *
 * No database required — [MockUserRepository] implements the [UserRepository] interface
 * with simple ConcurrentHashMap storage.
 *
 * The test application is configured identically to production:
 *   - ContentNegotiation (JSON)
 *   - JWT Security plugin
 *   - StatusPages
 *   - authRoutes() with mock repository
 */
class AuthRoutesTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Test app setup ────────────────────────────────────────────────────────

    private fun ApplicationTestBuilder.setupApp(repo: UserRepository) {
        application {
            configureSerialization()
            configureSecurity()
            configureStatusPages()
            routing {
                route("/api/v1") {
                    authRoutes(repo, authLimiter = null, refreshLimiter = null)
                }
            }
        }
    }

    private fun ApplicationTestBuilder.testClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    fun `POST auth_register - success with valid data`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "testuser", password = "password123"))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertTrue(body.success)
        assertTrue(body.accessToken.isNotBlank(), "accessToken should be non-blank on success")
        assertEquals("testuser", body.username)
    }

    @Test
    fun `POST auth_register - fails with username too short`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "ab", password = "password123"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_login_too_short", body.message)
    }

    @Test
    fun `POST auth_register - fails with password too short`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "validuser", password = "123"))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_password_too_short", body.message)
    }

    @Test
    fun `POST auth_register - fails when user already exists`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        // Register first
        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "duplicate", password = "password123"))
        }

        // Second registration with same username
        val response = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "duplicate", password = "password123"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_user_exists", body.message)
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    fun `POST auth_login - success with valid credentials`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "loginuser", password = "password123"))
        }

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "loginuser", password = "password123"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertTrue(body.success)
        assertTrue(body.accessToken.isNotBlank())
        assertEquals("loginuser", body.username)
    }

    @Test
    fun `POST auth_login - fails with non-existent user`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "nonexistent", password = "password123"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_invalid_credentials", body.message)
    }

    @Test
    fun `POST auth_login - fails with wrong password`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "wrongpass", password = "correct123"))
        }

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "wrongpass", password = "incorrect123"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_invalid_credentials", body.message)
    }

    @Test
    fun `POST auth_login - fails with blank fields`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "", password = ""))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertEquals(false, body.success)
        assertEquals("err_auth_fill_fields", body.message)
    }

    // ── /auth/me ──────────────────────────────────────────────────────────────

    @Test
    fun `GET auth_me - success with valid JWT`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val registerResponse = client.post("/api/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "meuser", password = "password123"))
        }
        val registerData = json.decodeFromString<AuthResponse>(registerResponse.bodyAsText())
        assertNotNull(registerData.accessToken)

        val response = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer ${registerData.accessToken}")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<AuthResponse>(response.bodyAsText())
        assertTrue(body.success)
        assertEquals("meuser", body.username)
    }

    @Test
    fun `GET auth_me - fails without Authorization header`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.get("/api/v1/auth/me")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET auth_me - fails with malformed token`() = testApplication {
        val repo = MockUserRepository()
        setupApp(repo)
        val client = testClient()

        val response = client.get("/api/v1/auth/me") {
            header(HttpHeaders.Authorization, "Bearer not.a.real.token")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

// ── In-memory mock ────────────────────────────────────────────────────────────

/**
 * Thread-safe in-memory [UserRepository] for testing.
 *
 * Stores users in a [ConcurrentHashMap]. Passwords are hashed via [PasswordHasher]
 * (bcrypt) to mirror production behaviour so [verifyPassword] works correctly in tests.
 * Refresh tokens are stored as plain UUIDs and expire only when explicitly set.
 */
internal class MockUserRepository : UserRepository {

    private data class StoredToken(val userId: String, val expiresAt: Long)

    private val users  = ConcurrentHashMap<String, ServerUser>()   // id → user
    private val byName = ConcurrentHashMap<String, String>()        // username → id
    private val tokens = ConcurrentHashMap<String, StoredToken>()   // rawToken → entry

    override suspend fun findByUsername(username: String): ServerUser? =
        byName[username.lowercase()]?.let { users[it] }

    override suspend fun findById(id: String): ServerUser? = users[id]

    override suspend fun existsByUsername(username: String): Boolean =
        byName.containsKey(username.lowercase())

    override suspend fun create(username: String, rawPassword: String): ServerUser {
        val id   = UUID.randomUUID().toString()
        val name = username.lowercase().trim()
        val user = ServerUser(
            id           = id,
            username     = name,
            passwordHash = PasswordHasher.hash(rawPassword),
            createdAt    = System.currentTimeMillis()
        )
        users[id]   = user
        byName[name] = id
        return user
    }

    override fun verifyPassword(rawPassword: String, storedHash: String): Boolean =
        PasswordHasher.verify(rawPassword, storedHash)

    override suspend fun createRefreshToken(userId: String): String {
        val raw = UUID.randomUUID().toString()
        tokens[raw] = StoredToken(userId, System.currentTimeMillis() + JwtConfig.REFRESH_TTL_MS)
        return raw
    }

    override suspend fun consumeRefreshToken(rawToken: String): ServerUser? {
        val entry = tokens.remove(rawToken) ?: return null
        if (entry.expiresAt < System.currentTimeMillis()) return null
        return users[entry.userId]
    }

    override suspend fun revokeAllTokens(userId: String) {
        tokens.entries.removeIf { (_, entry) -> entry.userId == userId }
    }

    // ── Admin stubs ───────────────────────────────────────────────────────────

    override suspend fun listUsers(limit: Int, offset: Long, search: String?): List<AdminUserRow> {
        val filtered = if (search.isNullOrBlank()) users.values.toList()
                       else users.values.filter { it.username.contains(search, ignoreCase = true) }
        return filtered
            .sortedByDescending { it.createdAt }
            .drop(offset.toInt())
            .take(limit)
            .map { u -> AdminUserRow(id = u.id, username = u.username, createdAt = u.createdAt, gamesPlayed = 0) }
    }

    override suspend fun countUsers(search: String?): Long =
        if (search.isNullOrBlank()) users.size.toLong()
        else users.values.count { it.username.contains(search, ignoreCase = true) }.toLong()

    override suspend fun updatePassword(userId: String, rawPassword: String): Boolean {
        val existing = users[userId] ?: return false
        users[userId] = existing.copy(passwordHash = PasswordHasher.hash(rawPassword))
        revokeAllTokens(userId)
        return true
    }

    override suspend fun rehashPassword(userId: String, rawPassword: String) {
        val existing = users[userId] ?: return
        users[userId] = existing.copy(passwordHash = PasswordHasher.hash(rawPassword))
    }

    override suspend fun deleteUserCascade(userId: String): Boolean {
        val user = users.remove(userId) ?: return false
        byName.remove(user.username)
        revokeAllTokens(userId)
        return true
    }
}
