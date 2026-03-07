package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Append-only snapshot history — one row per explicit "save checkpoint".
 *
 * This is separate from [GameSessionsTable] which holds the live mutable state.
 * Use this table for save slots / history / rollback scenarios.
 *
 * [snapshotId]  — UUID, PK
 * [userId]      — FK to users
 * [slotName]    — user-defined label, e.g. "Month 3 checkpoint"
 * [stateJson]   — serialized GameState blob
 * [savedAt]     — epoch millis
 */
object GameStatesTable : Table("game_state_snapshots") {
    val snapshotId = varchar("snapshot_id", 36)                  // UUID
    val userId     = varchar("user_id", 36).references(UsersTable.id)
    val slotName   = varchar("slot_name", 100).default("autosave")
    val stateJson  = text("state_json")
    val savedAt    = long("saved_at")

    override val primaryKey = PrimaryKey(snapshotId)

    // Composite index: fast look-up of all snapshots for a user ordered by time.
    // Exposed index() signature: (customIndexName: String?, isUnique: Boolean, vararg columns)
    val userSavedIdx = index("idx_snapshots_user_saved", isUnique = false, userId, savedAt)
}
