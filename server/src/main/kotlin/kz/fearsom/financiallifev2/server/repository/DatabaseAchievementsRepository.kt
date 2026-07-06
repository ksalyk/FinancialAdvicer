package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.server.database.tables.AchievementFeedbackTable
import kz.fearsom.financiallifev2.server.database.tables.UserAchievementsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update

/** PostgreSQL-backed implementation of [AchievementsRepository] via Exposed ORM. */
class DatabaseAchievementsRepository(private val db: Database) : AchievementsRepository {

    override suspend fun listUnlocks(userId: String): List<AchievementUnlockDto> =
        newSuspendedTransaction(db = db) {
            UserAchievementsTable
                .selectAll()
                .where { UserAchievementsTable.userId eq userId }
                .orderBy(UserAchievementsTable.unlockedAt, SortOrder.ASC)
                .map {
                    AchievementUnlockDto(
                        achievementId     = it[UserAchievementsTable.achievementId],
                        unlockedAt        = it[UserAchievementsTable.unlockedAt],
                        sourceCharacterId = it[UserAchievementsTable.sourceCharacterId],
                        sourceEraId       = it[UserAchievementsTable.sourceEraId]
                    )
                }
        }

    override suspend fun unlock(userId: String, unlocks: List<AchievementUnlockDto>): Int {
        if (unlocks.isEmpty()) return 0
        val now = System.currentTimeMillis()

        return newSuspendedTransaction(db = db) {
            val existing = UserAchievementsTable
                .selectAll()
                .where { UserAchievementsTable.userId eq userId }
                .map { it[UserAchievementsTable.achievementId] }
                .toSet()

            // First unlock wins: dedupe within the batch, skip already-persisted ids.
            val fresh = unlocks
                .distinctBy { it.achievementId }
                .filter { it.achievementId !in existing }

            fresh.forEach { dto ->
                UserAchievementsTable.insert {
                    it[UserAchievementsTable.userId]            = userId
                    it[UserAchievementsTable.achievementId]     = dto.achievementId
                    // Never trust a future client clock.
                    it[UserAchievementsTable.unlockedAt]        = dto.unlockedAt.coerceAtMost(now)
                    it[UserAchievementsTable.sourceCharacterId] = dto.sourceCharacterId?.take(50)
                    it[UserAchievementsTable.sourceEraId]       = dto.sourceEraId?.take(50)
                }
            }
            fresh.size
        }
    }

    override suspend fun setFeedback(userId: String, achievementId: String, vote: String?) {
        val now = System.currentTimeMillis()
        newSuspendedTransaction(db = db) {
            val where = (AchievementFeedbackTable.userId eq userId) and
                (AchievementFeedbackTable.achievementId eq achievementId)

            if (vote == null) {
                AchievementFeedbackTable.deleteWhere { where }
                return@newSuspendedTransaction
            }

            val exists = AchievementFeedbackTable
                .selectAll()
                .where { where }
                .count() > 0

            if (exists) {
                AchievementFeedbackTable.update({ where }) {
                    it[AchievementFeedbackTable.vote]    = vote
                    it[AchievementFeedbackTable.votedAt] = now
                }
            } else {
                AchievementFeedbackTable.insert {
                    it[AchievementFeedbackTable.userId]        = userId
                    it[AchievementFeedbackTable.achievementId] = achievementId
                    it[AchievementFeedbackTable.vote]          = vote
                    it[AchievementFeedbackTable.votedAt]       = now
                }
            }
        }
    }

    override suspend fun listFeedback(userId: String): Map<String, String> =
        newSuspendedTransaction(db = db) {
            AchievementFeedbackTable
                .selectAll()
                .where { AchievementFeedbackTable.userId eq userId }
                .associate {
                    it[AchievementFeedbackTable.achievementId] to it[AchievementFeedbackTable.vote]
                }
        }

    // ── Admin operations ──────────────────────────────────────────────────────

    override suspend fun adminGrant(userId: String, achievementId: String): Boolean =
        newSuspendedTransaction(db = db) {
            val exists = UserAchievementsTable
                .selectAll()
                .where {
                    (UserAchievementsTable.userId eq userId) and
                        (UserAchievementsTable.achievementId eq achievementId)
                }
                .count() > 0
            if (exists) return@newSuspendedTransaction false

            UserAchievementsTable.insert {
                it[UserAchievementsTable.userId]            = userId
                it[UserAchievementsTable.achievementId]     = achievementId
                it[UserAchievementsTable.unlockedAt]        = System.currentTimeMillis()
                it[UserAchievementsTable.sourceCharacterId] = null
                it[UserAchievementsTable.sourceEraId]       = null
            }
            true
        }

    override suspend fun adminRevoke(userId: String, achievementId: String): Boolean =
        newSuspendedTransaction(db = db) {
            UserAchievementsTable.deleteWhere {
                (UserAchievementsTable.userId eq userId) and
                    (UserAchievementsTable.achievementId eq achievementId)
            } > 0
        }
}
