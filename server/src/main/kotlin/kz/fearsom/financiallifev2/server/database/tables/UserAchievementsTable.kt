package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.v1.core.Table

/**
 * Per-user unlocked achievements.
 *
 * Achievement definitions live in code (`:shared` AchievementCatalog) — the DB
 * stores only unlock state, so no catalog table / FK on achievementId exists.
 *
 * [achievementId]     — catalog id, e.g. "game.cushion", "scam.ponzi"
 * [unlockedAt]        — epoch millis (client-reported; server clamps to now when in the future)
 * [sourceCharacterId] — character whose playthrough unlocked it (drives "Открыта · <name>")
 * [sourceEraId]       — era of that playthrough
 */
object UserAchievementsTable : Table("user_achievements") {
    val userId            = varchar("user_id", 36).references(UsersTable.id)
    val achievementId     = varchar("achievement_id", 64)
    val unlockedAt        = long("unlocked_at")
    val sourceCharacterId = varchar("source_character_id", 50).nullable()
    val sourceEraId       = varchar("source_era_id", 50).nullable()

    override val primaryKey = PrimaryKey(userId, achievementId)
}

/**
 * 👍/👎 feedback on scam dossiers (and any achievement detail sheet).
 * One vote per user per achievement; re-voting overwrites, null clears (row delete).
 */
object AchievementFeedbackTable : Table("achievement_feedback") {
    val userId        = varchar("user_id", 36).references(UsersTable.id)
    val achievementId = varchar("achievement_id", 64)
    /** "up" | "down" */
    val vote          = varchar("vote", 8)
    val votedAt       = long("voted_at")

    override val primaryKey = PrimaryKey(userId, achievementId)
}
