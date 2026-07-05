package kz.fearsom.financiallifev2.server.repository

import kotlinx.coroutines.runBlocking
import kz.fearsom.financiallifev2.achievements.AchievementCatalog
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.server.database.DatabaseTestFixture
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests against the H2 (PostgreSQL-mode) fixture — also proves
 * the V004 migration creates working tables.
 */
class DatabaseAchievementsRepositoryTest {

    private val db = DatabaseTestFixture.database

    @Before
    fun reset() {
        DatabaseTestFixture.reset()
        transaction(db) {
            UsersTable.insert {
                it[id] = USER_ID
                it[username] = "ach_user"
                it[passwordHash] = "0".repeat(64)
                it[createdAt] = 1L
            }
        }
    }

    @Test
    fun `unlock is idempotent and first unlock wins`() = runBlocking {
        val repo = DatabaseAchievementsRepository(db)

        val first = repo.unlock(USER_ID, listOf(
            AchievementUnlockDto(AchievementCatalog.FIRST_STEPS, unlockedAt = 1_000L, sourceCharacterId = "daniyar")
        ))
        assertEquals(1, first)

        // Same id again with different metadata — ignored.
        val replay = repo.unlock(USER_ID, listOf(
            AchievementUnlockDto(AchievementCatalog.FIRST_STEPS, unlockedAt = 9_999L, sourceCharacterId = "aigul")
        ))
        assertEquals(0, replay)

        val stored = repo.listUnlocks(USER_ID).single()
        assertEquals(1_000L, stored.unlockedAt)
        assertEquals("daniyar", stored.sourceCharacterId)
    }

    @Test
    fun `unlock dedupes ids inside a single batch`() = runBlocking {
        val repo = DatabaseAchievementsRepository(db)

        val inserted = repo.unlock(USER_ID, listOf(
            AchievementUnlockDto(AchievementCatalog.CUSHION, unlockedAt = 100L),
            AchievementUnlockDto(AchievementCatalog.CUSHION, unlockedAt = 200L),
            AchievementUnlockDto(AchievementCatalog.SCAM_PONZI, unlockedAt = 300L)
        ))

        assertEquals(2, inserted)
        assertEquals(2, repo.listUnlocks(USER_ID).size)
    }

    @Test
    fun `future client timestamps are clamped to server time`() = runBlocking {
        val repo = DatabaseAchievementsRepository(db)
        val farFuture = System.currentTimeMillis() + 86_400_000L

        repo.unlock(USER_ID, listOf(
            AchievementUnlockDto(AchievementCatalog.NEW_LIFE, unlockedAt = farFuture)
        ))

        val stored = repo.listUnlocks(USER_ID).single()
        assertTrue(stored.unlockedAt <= System.currentTimeMillis())
    }

    @Test
    fun `feedback upsert overwrite and clear`() = runBlocking {
        val repo = DatabaseAchievementsRepository(db)
        val id = AchievementCatalog.SCAM_PONZI

        repo.setFeedback(USER_ID, id, "up")
        assertEquals(mapOf(id to "up"), repo.listFeedback(USER_ID))

        repo.setFeedback(USER_ID, id, "down")
        assertEquals(mapOf(id to "down"), repo.listFeedback(USER_ID))

        repo.setFeedback(USER_ID, id, null)
        assertTrue(repo.listFeedback(USER_ID).isEmpty())
    }

    @Test
    fun `unlocks are scoped per user`() = runBlocking {
        val otherUser = "user-ach-other"
        transaction(db) {
            UsersTable.insert {
                it[id] = otherUser
                it[username] = "ach_user_2"
                it[passwordHash] = "0".repeat(64)
                it[createdAt] = 1L
            }
        }

        val repo = DatabaseAchievementsRepository(db)
        repo.unlock(USER_ID, listOf(AchievementUnlockDto(AchievementCatalog.FIRST_STEPS, 1L)))

        assertTrue(repo.listUnlocks(otherUser).isEmpty())
    }

    companion object {
        private const val USER_ID = "user-ach-001"
    }
}
