package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.model.GameEnding
import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/** PostgreSQL-backed implementation of [StatisticsRepository] via Exposed ORM. */
class DatabaseStatisticsRepository : StatisticsRepository {

    /**
     * Persists a single completed game session.
     * Called once when the player reaches an ending (or abandons).
     * Returns the generated record ID.
     */
    override suspend fun recordSession(
        userId: String,
        request: RecordSessionRequest
    ): String {
        val id  = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        newSuspendedTransaction {
            CompletedSessionsTable.insert {
                it[CompletedSessionsTable.id]               = id
                it[CompletedSessionsTable.userId]           = userId
                it[CompletedSessionsTable.characterId]      = request.characterId
                it[CompletedSessionsTable.characterName]    = request.characterName
                it[CompletedSessionsTable.characterEmoji]   = request.characterEmoji
                it[CompletedSessionsTable.eraId]            = request.eraId
                it[CompletedSessionsTable.eraName]          = request.eraName
                it[CompletedSessionsTable.ending]           = request.ending
                it[CompletedSessionsTable.finalCapital]     = request.finalCapital
                it[CompletedSessionsTable.finalInvestments] = request.finalInvestments
                it[CompletedSessionsTable.finalDebt]        = request.finalDebt
                it[CompletedSessionsTable.finalStress]      = request.finalStress
                it[CompletedSessionsTable.finalKnowledge]   = request.finalKnowledge
                it[CompletedSessionsTable.finalRiskLevel]   = request.finalRiskLevel
                it[CompletedSessionsTable.gameYear]         = request.gameYear
                it[CompletedSessionsTable.gameMonth]        = request.gameMonth
                it[CompletedSessionsTable.completedAt]      = now
            }
        }

        return id
    }

    /**
     * Computes aggregated [PlayerStatisticsDto] for a user directly from the DB.
     * All math happens in-process (not SQL) because the data sets are small and
     * the logic mirrors the client-side `GameSessionRepository.getPlayerStatistics()`.
     */
    override suspend fun getPlayerStatistics(userId: String): PlayerStatisticsDto =
        newSuspendedTransaction {
            val rows = CompletedSessionsTable
                .selectAll()
                .where { CompletedSessionsTable.userId eq userId }
                .orderBy(CompletedSessionsTable.completedAt, SortOrder.DESC)
                .toList()

            val validEndings = rows.mapNotNull { row ->
                runCatching { GameEnding.valueOf(row[CompletedSessionsTable.ending]) }.getOrNull()
            }

            val bestEnding = validEndings.maxByOrNull { it.ordinal }?.name

            val avgCapital = if (rows.isNotEmpty())
                rows.sumOf { it[CompletedSessionsTable.finalCapital] } / rows.size
            else 0L

            val endingDistribution = GameEnding.entries.associate { ending ->
                ending.name to rows.count { it[CompletedSessionsTable.ending] == ending.name }
            }

            val mostPlayedEraId = rows
                .groupBy { it[CompletedSessionsTable.eraId] }
                .maxByOrNull { it.value.size }
                ?.key

            val perCharacter = rows
                .groupBy { it[CompletedSessionsTable.characterId] }
                .map { (charId, sessions) ->
                    val charEndings = sessions.mapNotNull { row ->
                        runCatching { GameEnding.valueOf(row[CompletedSessionsTable.ending]) }.getOrNull()
                    }
                    CharacterStatsDto(
                        characterId    = charId,
                        characterName  = sessions.first()[CompletedSessionsTable.characterName],
                        characterEmoji = sessions.first()[CompletedSessionsTable.characterEmoji],
                        timesPlayed    = sessions.size,
                        bestEnding     = charEndings.maxByOrNull { it.ordinal }?.name,
                        averageCapital = sessions.sumOf { it[CompletedSessionsTable.finalCapital] } / sessions.size
                    )
                }

            val perEra = rows
                .groupBy { it[CompletedSessionsTable.eraId] }
                .map { (eraId, sessions) ->
                    val eraEndings = sessions.mapNotNull { row ->
                        runCatching { GameEnding.valueOf(row[CompletedSessionsTable.ending]) }.getOrNull()
                    }
                    EraStatsDto(
                        eraId       = eraId,
                        eraName     = sessions.first()[CompletedSessionsTable.eraName],
                        timesPlayed = sessions.size,
                        bestEnding  = eraEndings.maxByOrNull { it.ordinal }?.name
                    )
                }

            PlayerStatisticsDto(
                totalGamesPlayed    = rows.size,
                gamesCompleted      = rows.size,   // every row in this table is a completed game
                bestEnding          = bestEnding,
                averageCapitalAtEnd = avgCapital,
                mostPlayedEraId     = mostPlayedEraId,
                endingDistribution  = endingDistribution,
                perCharacter        = perCharacter,
                perEra              = perEra
            )
        }

    // ── Targeted deletes (used by Admin API cascade) ──────────────────────────

    override suspend fun deleteByCharacterId(characterId: String): Int =
        newSuspendedTransaction {
            CompletedSessionsTable.deleteWhere {
                CompletedSessionsTable.characterId eq characterId
            }
        }

    override suspend fun deleteByEraId(eraId: String): Int =
        newSuspendedTransaction {
            CompletedSessionsTable.deleteWhere {
                CompletedSessionsTable.eraId eq eraId
            }
        }

    override suspend fun deleteByCharacterIdForUser(userId: String, characterId: String): Int =
        newSuspendedTransaction {
            CompletedSessionsTable.deleteWhere {
                (CompletedSessionsTable.userId eq userId) and
                (CompletedSessionsTable.characterId eq characterId)
            }
        }
}
