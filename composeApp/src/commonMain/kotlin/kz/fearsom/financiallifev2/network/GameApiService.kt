package kz.fearsom.financiallifev2.network

import io.github.aakira.napier.Napier
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.model.GameEnding
import kz.fearsom.financiallifev2.model.GameSession

private const val TAG = "GameApiService"

// ── Request / Response DTOs (mirror server StatisticsRepository DTOs) ─────────

@Serializable
data class RecordSessionRequest(
    val characterId: String,
    val characterName: String,
    val characterEmoji: String,
    val eraId: String,
    val eraName: String,
    val ending: String,               // GameEnding.name
    val finalCapital: Long,
    val finalInvestments: Long = 0L,
    val finalDebt: Long,
    val finalStress: Int,
    val finalKnowledge: Int,
    val finalRiskLevel: Int,
    val gameYear: Int,
    val gameMonth: Int
)

@Serializable
data class CharacterStatsDto(
    val characterId: String,
    val characterName: String,
    val characterEmoji: String,
    val timesPlayed: Int,
    val bestEnding: String?,
    val averageCapital: Long
)

@Serializable
data class EraStatsDto(
    val eraId: String,
    val eraName: String,
    val timesPlayed: Int,
    val bestEnding: String?
)

@Serializable
data class PlayerStatisticsResponse(
    val totalGamesPlayed: Int,
    val gamesCompleted: Int,
    val bestEnding: String?,
    val averageCapitalAtEnd: Long,
    val mostPlayedEraId: String?,
    val endingDistribution: Map<String, Int>,
    val perCharacter: List<CharacterStatsDto>,
    val perEra: List<EraStatsDto>
)

// ── Service ───────────────────────────────────────────────────────────────────

/**
 * HTTP client for game statistics endpoints.
 *
 * Auth is handled transparently by the Ktor Bearer plugin in [HttpClient].
 * All methods return [Result] so callers can decide how to handle failures
 * (fire-and-forget on complete, or show error on statistics screen).
 */
class GameApiService(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {

    /**
     * Sends a completed session record to the server.
     * Fire-and-forget: failures are logged but not rethrown — the game should
     * never block on a network call at the end screen.
     */
    suspend fun recordCompletedSession(session: GameSession, ending: GameEnding): Result<Unit> =
        runCatching {
            val stats = session.currentStats
            httpClient.post("$baseUrl/game/statistics/record") {
                contentType(ContentType.Application.Json)
                setBody(
                    RecordSessionRequest(
                        characterId      = session.characterId,
                        characterName    = session.characterName,
                        characterEmoji   = session.characterEmoji,
                        eraId            = session.eraId,
                        eraName          = session.eraName,
                        ending           = ending.name,
                        finalCapital     = stats.capital,
                        finalInvestments = 0L,
                        finalDebt        = stats.debt,
                        finalStress      = stats.stress,
                        finalKnowledge   = stats.financialKnowledge,
                        finalRiskLevel   = stats.riskLevel,
                        gameYear         = session.currentGameYear,
                        gameMonth        = session.currentGameMonth
                    )
                )
            }
            Napier.i("Session recorded on server: ${session.id} ending=${ending.name}", tag = TAG)
        }.onFailure { e ->
            Napier.w("Failed to record session on server: ${e.message}", tag = TAG)
        }

    /**
     * Fetches aggregated lifetime statistics for the current user.
     * Returns [Result.failure] on network errors so the UI can fall back to
     * locally computed stats.
     */
    suspend fun getPlayerStatistics(): Result<PlayerStatisticsResponse> =
        runCatching {
            httpClient.get("$baseUrl/game/statistics").body<PlayerStatisticsResponse>()
        }.onFailure { e ->
            Napier.w("Failed to fetch statistics from server: ${e.message}", tag = TAG)
        }
}
