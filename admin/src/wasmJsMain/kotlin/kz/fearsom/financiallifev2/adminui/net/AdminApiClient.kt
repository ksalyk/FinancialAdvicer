package kz.fearsom.financiallifev2.adminui.net

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.admin.AdminUserDetailRow
import kz.fearsom.financiallifev2.admin.AdminUserListResponse
import kz.fearsom.financiallifev2.admin.CharacterRow
import kz.fearsom.financiallifev2.admin.EraRow
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto

// ── Request bodies ────────────────────────────────────────────────────────────

@Serializable private data class LoginBody(val username: String, val password: String)
@Serializable private data class ResetPasswordBody(val password: String)

// ── Client ────────────────────────────────────────────────────────────────────

/**
 * Thin Ktor HTTP client wrapping the `/api/v1/admin` endpoints.
 *
 * Base URL is derived from the browser's `window.location.origin` so the same
 * artifact works both in production (same-origin) and with a webpack dev proxy.
 *
 * The admin session cookie is httpOnly and sent automatically by the browser on
 * same-origin requests — no manual token management required here.
 */
class AdminApiClient {

    private val baseUrl = "${window.location.origin}/api/v1"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val client = HttpClient(Js) {
        install(ContentNegotiation) { json(json) }
    }

    // ── Auth ──────────────────────────────────────────────────────────────────

    /**
     * Attempts login. Returns true on success; false on wrong credentials.
     * Throws on network/server error.
     */
    suspend fun login(username: String, password: String): Boolean {
        val res = client.post("$baseUrl/admin/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginBody(username, password))
        }
        return res.status == HttpStatusCode.OK
    }

    suspend fun logout() {
        client.post("$baseUrl/admin/logout")
    }

    /**
     * Returns the currently logged-in admin username, or null if the session
     * is not active (401).
     */
    suspend fun getMe(): String? {
        val res = client.get("$baseUrl/admin/me")
        if (res.status != HttpStatusCode.OK) return null
        @Serializable data class MeResponse(val username: String)
        return res.body<MeResponse>().username
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    suspend fun listUsers(
        limit: Int = 50,
        offset: Long = 0,
        search: String? = null
    ): AdminUserListResponse =
        client.get("$baseUrl/admin/users") {
            parameter("limit", limit)
            parameter("offset", offset)
            if (!search.isNullOrBlank()) parameter("search", search)
        }.body()

    suspend fun getUserDetail(id: String): AdminUserDetailRow =
        client.get("$baseUrl/admin/users/$id").body()

    suspend fun resetPassword(id: String, newPassword: String): Boolean {
        val res = client.post("$baseUrl/admin/users/$id/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordBody(newPassword))
        }
        return res.status.isSuccess()
    }

    suspend fun deleteUser(id: String): Boolean {
        val res = client.delete("$baseUrl/admin/users/$id")
        return res.status.isSuccess()
    }

    // ── Characters ────────────────────────────────────────────────────────────

    suspend fun listCharacters(): List<CharacterRow> =
        client.get("$baseUrl/admin/characters").body()

    suspend fun activateCharacter(id: String): Boolean {
        val res = client.post("$baseUrl/admin/characters/$id/activate")
        return res.status.isSuccess()
    }

    suspend fun deactivateCharacter(id: String): Boolean {
        val res = client.post("$baseUrl/admin/characters/$id/deactivate")
        return res.status.isSuccess()
    }

    // ── Eras ──────────────────────────────────────────────────────────────────

    suspend fun listEras(): List<EraRow> =
        client.get("$baseUrl/admin/eras").body()

    suspend fun activateEra(id: String): Boolean {
        val res = client.post("$baseUrl/admin/eras/$id/activate")
        return res.status.isSuccess()
    }

    suspend fun deactivateEra(id: String): Boolean {
        val res = client.post("$baseUrl/admin/eras/$id/deactivate")
        return res.status.isSuccess()
    }

    // ── Scenarios ─────────────────────────────────────────────────────────────

    suspend fun listScenarioCombos(): List<ScenarioComboDto> =
        client.get("$baseUrl/admin/scenarios").body()

    suspend fun getScenarioGraph(characterId: String, eraId: String): ScenarioGraphDto =
        client.get("$baseUrl/admin/scenarios/$characterId/$eraId").body()
}
