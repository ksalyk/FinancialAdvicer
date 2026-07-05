package kz.fearsom.financiallifev2.server.database.migrations.versions

import kz.fearsom.financiallifev2.server.database.migrations.Migration
import kz.fearsom.financiallifev2.server.database.tables.AchievementFeedbackTable
import kz.fearsom.financiallifev2.server.database.tables.UserAchievementsTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Adds achievement persistence:
 * - `user_achievements`      — per-user unlock state (definitions live in :shared code)
 * - `achievement_feedback`   — 👍/👎 votes on dossier content
 */
object V004_AddAchievements : Migration {
    override val version = 4
    override val description = "Add user_achievements and achievement_feedback tables"

    override suspend fun up(db: Database) {
        newSuspendedTransaction(db = db) {
            SchemaUtils.createMissingTablesAndColumns(
                UserAchievementsTable,
                AchievementFeedbackTable
            )
        }
    }

    override suspend fun down(db: Database) {
        newSuspendedTransaction(db = db) {
            SchemaUtils.drop(
                AchievementFeedbackTable,
                UserAchievementsTable
            )
        }
    }
}
