package kz.fearsom.financiallifev2.adminui.net

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.js.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.admin.AchievementAdminRow
import kz.fearsom.financiallifev2.admin.AdminUserDetailRow
import kz.fearsom.financiallifev2.admin.AdminUserListResponse
import kz.fearsom.financiallifev2.admin.CharacterRow
import kz.fearsom.financiallifev2.admin.EraRow
import kz.fearsom.financiallifev2.admin.ScenarioComboDto
import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.admin.StoryDetail
import kz.fearsom.financiallifev2.admin.StoryReviewRequest
import kz.fearsom.financiallifev2.admin.StoryRow
import kz.fearsom.financiallifev2.admin.StoryStatus
import kz.fearsom.financiallifev2.admin.StoryValidationReport
import kz.fearsom.financiallifev2.admin.UpsertAchievementRequest
import kz.fearsom.financiallifev2.admin.UpsertCharacterRequest
import kz.fearsom.financiallifev2.admin.UpsertEraRequest
import kz.fearsom.financiallifev2.admin.UpsertStoryRequest
import kz.fearsom.financiallifev2.admin.UserAchievementsAdminResponse

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

    /** Throws with the server's error payload (e.g. "Password too short") on failure. */
    suspend fun resetPassword(id: String, newPassword: String) {
        val res = client.post("$baseUrl/admin/users/$id/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(ResetPasswordBody(newPassword))
        }
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun deleteUser(id: String) {
        val res = client.delete("$baseUrl/admin/users/$id")
        if (!res.status.isSuccess()) res.failure()
    }

    // ── Characters ────────────────────────────────────────────────────────────

    suspend fun listCharacters(): List<CharacterRow> =
        client.get("$baseUrl/admin/characters").body()

    suspend fun getCharacter(id: String): CharacterRow =
        client.get("$baseUrl/admin/characters/$id").body()

    /** Create or update a character (server upserts by id). Returns the persisted row. */
    suspend fun upsertCharacter(req: UpsertCharacterRequest): CharacterRow {
        val res = client.post("$baseUrl/admin/characters") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    /** Hard-delete a character + cascade its stats across all users. */
    suspend fun deleteCharacter(id: String) {
        val res = client.delete("$baseUrl/admin/characters/$id")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun activateCharacter(id: String) {
        val res = client.post("$baseUrl/admin/characters/$id/activate")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun deactivateCharacter(id: String) {
        val res = client.post("$baseUrl/admin/characters/$id/deactivate")
        if (!res.status.isSuccess()) res.failure()
    }

    // ── Eras ──────────────────────────────────────────────────────────────────

    suspend fun listEras(): List<EraRow> =
        client.get("$baseUrl/admin/eras").body()

    suspend fun getEra(id: String): EraRow =
        client.get("$baseUrl/admin/eras/$id").body()

    /** Create or update an era (server upserts by id). Returns the persisted row. */
    suspend fun upsertEra(req: UpsertEraRequest): EraRow {
        val res = client.post("$baseUrl/admin/eras") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    /** Hard-delete an era + cascade its stats across all users. */
    suspend fun deleteEra(id: String) {
        val res = client.delete("$baseUrl/admin/eras/$id")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun activateEra(id: String) {
        val res = client.post("$baseUrl/admin/eras/$id/activate")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun deactivateEra(id: String) {
        val res = client.post("$baseUrl/admin/eras/$id/deactivate")
        if (!res.status.isSuccess()) res.failure()
    }

    // ── Scenarios (built-in, read-only) ───────────────────────────────────────

    suspend fun listScenarioCombos(): List<ScenarioComboDto> =
        client.get("$baseUrl/admin/scenarios").body()

    suspend fun getScenarioGraph(characterId: String, eraId: String): ScenarioGraphDto =
        client.get("$baseUrl/admin/scenarios/$characterId/$eraId").body()

    // ── Stories (DB-backed CRUD + moderation) ─────────────────────────────────

    suspend fun listStories(status: StoryStatus? = null): List<StoryRow> =
        client.get("$baseUrl/admin/stories") {
            status?.let { parameter("status", it.name) }
        }.body()

    suspend fun getStory(id: String): StoryDetail =
        client.get("$baseUrl/admin/stories/$id").body()

    suspend fun createStory(req: UpsertStoryRequest): StoryDetail {
        val res = client.post("$baseUrl/admin/stories") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    suspend fun updateStory(req: UpsertStoryRequest): StoryDetail {
        val res = client.put("$baseUrl/admin/stories/${req.id}") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    suspend fun deleteStory(id: String) {
        val res = client.delete("$baseUrl/admin/stories/$id")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun validateStory(id: String): StoryValidationReport {
        val res = client.post("$baseUrl/admin/stories/$id/validate")
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    // NB: body must be a concrete type at the callsite — wasmJs serialization
    // has no reflection, so a generic `Any` body would fail at runtime.
    private suspend fun storyAction(id: String, action: String): StoryRow {
        val res = client.post("$baseUrl/admin/stories/$id/$action")
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    suspend fun submitStory(id: String): StoryRow    = storyAction(id, "submit")
    suspend fun approveStory(id: String): StoryRow   = storyAction(id, "approve")
    suspend fun publishStory(id: String): StoryRow   = storyAction(id, "publish")
    suspend fun unpublishStory(id: String): StoryRow = storyAction(id, "unpublish")

    suspend fun rejectStory(id: String, note: String?): StoryRow {
        val res = client.post("$baseUrl/admin/stories/$id/reject") {
            contentType(ContentType.Application.Json)
            setBody(StoryReviewRequest(note))
        }
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    /** Snapshot a built-in code graph into an editable DRAFT story. */
    suspend fun cloneBuiltIn(characterId: String, eraId: String): StoryDetail {
        val res = client.post("$baseUrl/admin/stories/clone/$characterId/$eraId")
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    // ── Achievements ──────────────────────────────────────────────────────────

    suspend fun listAchievements(): List<AchievementAdminRow> =
        client.get("$baseUrl/admin/achievements").body()

    suspend fun upsertAchievement(req: UpsertAchievementRequest): AchievementAdminRow {
        val res = client.post("$baseUrl/admin/achievements") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
        if (!res.status.isSuccess()) res.failure()
        return res.body()
    }

    suspend fun activateAchievement(id: String) {
        val res = client.post("$baseUrl/admin/achievements/$id/activate")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun deactivateAchievement(id: String) {
        val res = client.post("$baseUrl/admin/achievements/$id/deactivate")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun deleteAchievement(id: String) {
        val res = client.delete("$baseUrl/admin/achievements/$id")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun listUserAchievements(userId: String): List<AchievementUnlockDto> =
        client.get("$baseUrl/admin/users/$userId/achievements")
            .body<UserAchievementsAdminResponse>().unlocks

    suspend fun grantAchievement(userId: String, achievementId: String) {
        val res = client.post("$baseUrl/admin/users/$userId/achievements/$achievementId")
        if (!res.status.isSuccess()) res.failure()
    }

    suspend fun revokeAchievement(userId: String, achievementId: String) {
        val res = client.delete("$baseUrl/admin/users/$userId/achievements/$achievementId")
        if (!res.status.isSuccess()) res.failure()
    }

    // ── Internals ───────────────────────────────────────────────────────────────

    /**
     * Raises an [IllegalStateException] carrying the server's error payload.
     * We read the raw text (not a typed body) so a non-2xx response with an
     * unexpected shape never triggers a secondary deserialization failure that
     * would mask the real error.
     */
    private suspend fun HttpResponse.failure(): Nothing {
        val text = runCatching { bodyAsText() }.getOrDefault("")
        error("HTTP ${status.value}: ${text.ifBlank { status.description }}")
    }
}
