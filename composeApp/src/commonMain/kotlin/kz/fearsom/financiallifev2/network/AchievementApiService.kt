package kz.fearsom.financiallifev2.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kz.fearsom.financiallifev2.achievements.AchievementDefinition
import kz.fearsom.financiallifev2.achievements.AchievementFeedbackRequest
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.achievements.UnlockAchievementsRequest
import kz.fearsom.financiallifev2.achievements.UnlockAchievementsResponse
import kz.fearsom.financiallifev2.achievements.UserAchievementsResponse
import kz.fearsom.financiallifev2.admin.AchievementCatalogResponse

private const val TAG = "AchievementApiService"

/**
 * HTTP client for /achievements endpoints. DTOs live in :shared
 * (kz.fearsom.financiallifev2.achievements) and are reused by the server.
 *
 * Auth is handled by the Ktor Bearer plugin; every method no-ops/fails softly
 * without a token so guest mode stays fully local.
 */
class AchievementApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val tokenStorage: TokenStorage? = null
) {

    private fun hasToken() = tokenStorage?.isAccessTokenPresent() == true

    /**
     * Public active-catalog fetch (no auth) — overlays admin catalog edits onto
     * the compile-time catalog. Served from /game/catalog/achievements.
     */
    suspend fun getCatalog(): Result<List<AchievementDefinition>> =
        runCatching {
            httpClient.get("$baseUrl/game/catalog/achievements")
                .body<AchievementCatalogResponse>().definitions
        }.onFailure { e ->
            Napier.w("Failed to fetch achievement catalog: ${e.message}", tag = TAG)
        }

    /** Fetches the authoritative unlock list for the current user. */
    suspend fun getUnlocks(): Result<List<AchievementUnlockDto>> =
        runCatching {
            if (!hasToken()) throw IllegalStateException("Authentication required")
            httpClient.get("$baseUrl/achievements").body<UserAchievementsResponse>().unlocks
        }.onFailure { e ->
            Napier.w("Failed to fetch achievements: ${e.message}", tag = TAG)
        }

    /**
     * Pushes locally earned unlocks. Idempotent server-side; returns the full
     * merged unlock list so the caller can reconcile in one round-trip.
     */
    suspend fun pushUnlocks(unlocks: List<AchievementUnlockDto>): Result<List<AchievementUnlockDto>> =
        runCatching {
            if (!hasToken()) throw IllegalStateException("Authentication required")
            httpClient.post("$baseUrl/achievements/unlock") {
                contentType(ContentType.Application.Json)
                setBody(UnlockAchievementsRequest(unlocks))
            }.body<UnlockAchievementsResponse>().unlocks
        }.onFailure { e ->
            Napier.w("Failed to push achievement unlocks: ${e.message}", tag = TAG)
        }

    /** Fire-and-forget vote sync. [vote] = "up" | "down" | null (clear). */
    suspend fun sendFeedback(achievementId: String, vote: String?): Result<Unit> =
        runCatching {
            if (!hasToken()) {
                Napier.d("Skipping feedback sync without auth token", tag = TAG)
                return@runCatching
            }
            httpClient.post("$baseUrl/achievements/$achievementId/feedback") {
                contentType(ContentType.Application.Json)
                setBody(AchievementFeedbackRequest(vote))
            }
            Unit
        }.onFailure { e ->
            Napier.w("Failed to send achievement feedback: ${e.message}", tag = TAG)
        }
}
