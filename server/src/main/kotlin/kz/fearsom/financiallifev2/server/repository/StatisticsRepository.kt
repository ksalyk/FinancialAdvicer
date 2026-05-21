package kz.fearsom.financiallifev2.server.repository

import kotlinx.serialization.Serializable

// ── Request / Response DTOs ───────────────────────────────────────────────────

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
data class PlayerStatisticsDto(
    val totalGamesPlayed: Int,
    val gamesCompleted: Int,
    val bestEnding: String?,
    val averageCapitalAtEnd: Long,
    val mostPlayedEraId: String?,
    val endingDistribution: Map<String, Int>,
    val perCharacter: List<CharacterStatsDto>,
    val perEra: List<EraStatsDto>
)

/**
 * Defines statistics persistence and aggregation operations.
 *
 * The concrete PostgreSQL-backed implementation is [DatabaseStatisticsRepository].
 */
interface StatisticsRepository {

    /** Records a single completed game session. Returns the generated record ID. */
    suspend fun recordSession(userId: String, request: RecordSessionRequest): String

    /** Returns aggregated lifetime stats for [userId]. */
    suspend fun getPlayerStatistics(userId: String): PlayerStatisticsDto

    // ── Targeted deletes (used by Admin API cascade) ──────────────────────────

    suspend fun deleteByCharacterId(characterId: String): Int
    suspend fun deleteByEraId(eraId: String): Int
    suspend fun deleteByCharacterIdForUser(userId: String, characterId: String): Int
}
