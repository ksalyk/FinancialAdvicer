package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto

/**
 * Persistence contract for per-user achievement unlocks + dossier feedback.
 *
 * Definitions are code-owned (`:shared` AchievementCatalog); only unlock state
 * and votes live in the database.
 */
interface AchievementsRepository {

    /** All unlocks for [userId], oldest first. */
    suspend fun listUnlocks(userId: String): List<AchievementUnlockDto>

    /**
     * Idempotent batch insert: already-unlocked ids are ignored (first unlock
     * wins — re-earning an achievement in another playthrough never overwrites
     * the original unlock record).
     *
     * @return number of newly inserted rows.
     */
    suspend fun unlock(userId: String, unlocks: List<AchievementUnlockDto>): Int

    /**
     * Upserts a vote ("up"/"down") or clears it (null).
     */
    suspend fun setFeedback(userId: String, achievementId: String, vote: String?)

    /** achievementId → vote for [userId]. */
    suspend fun listFeedback(userId: String): Map<String, String>
}
