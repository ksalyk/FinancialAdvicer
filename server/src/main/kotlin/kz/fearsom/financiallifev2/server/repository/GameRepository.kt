package kz.fearsom.financiallifev2.server.repository

// ── Shared row types ──────────────────────────────────────────────────────────
// Kept in this file so callers import only GameRepository and get the DTOs too.

data class GameSessionRow(
    val userId: String,
    val stateJson: String,
    val currentEventId: String,
    val updatedAt: Long
)

data class GameSnapshotRow(
    val snapshotId: String,
    val userId: String,
    val slotName: String,
    val stateJson: String,
    val savedAt: Long
)

/**
 * Defines all game-session persistence operations.
 *
 * The concrete PostgreSQL-backed implementation is [DatabaseGameRepository].
 * Tests use in-memory fakes implementing this interface.
 */
interface GameRepository {

    // ── Live session ───────────────────────────────────────────────────────────

    suspend fun loadSession(userId: String): GameSessionRow?

    suspend fun upsertSession(
        userId: String,
        stateJson: String,
        currentEventId: String
    )

    suspend fun deleteSession(userId: String)

    // ── Snapshots ──────────────────────────────────────────────────────────────

    suspend fun saveSnapshot(
        userId: String,
        stateJson: String,
        slotName: String = "autosave"
    ): GameSnapshotRow

    suspend fun listSnapshots(userId: String, limit: Int = 20): List<GameSnapshotRow>
    suspend fun loadSnapshot(snapshotId: String): GameSnapshotRow?
    suspend fun deleteSnapshot(snapshotId: String)
}
