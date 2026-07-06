package kz.fearsom.financiallifev2.server.database.migrations.versions

import kz.fearsom.financiallifev2.server.database.migrations.Migration
import kz.fearsom.financiallifev2.server.database.tables.AchievementsCatalogTable
import kz.fearsom.financiallifev2.server.database.tables.StoriesTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Adds content-management persistence:
 * - `stories`              — DB-backed scenario graphs with a moderation lifecycle
 *                            (DRAFT → PENDING_REVIEW → PUBLISHED / REJECTED / ARCHIVED)
 * - `achievements_catalog` — achievement definitions (seeded from :shared code
 *                            catalog; admin-editable, CUSTOM entries creatable)
 */
object V005_AddStoriesAndAchievementCatalog : Migration {
    override val version = 5
    override val description = "Add stories and achievements_catalog tables"

    override suspend fun up(db: Database) {
        newSuspendedTransaction(db = db) {
            SchemaUtils.createMissingTablesAndColumns(
                StoriesTable,
                AchievementsCatalogTable
            )
            exec("CREATE INDEX IF NOT EXISTS idx_stories_status ON stories (status)")
            exec("CREATE INDEX IF NOT EXISTS idx_stories_character_era ON stories (character_id, era_id)")
        }
    }

    override suspend fun down(db: Database) {
        newSuspendedTransaction(db = db) {
            SchemaUtils.drop(
                AchievementsCatalogTable,
                StoriesTable
            )
        }
    }
}
