package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Tracks the active in-progress game for each user.
 *
 * One row per user (upserted on each save).
 * [stateJson]       — full serialized GameState (kotlinx.serialization JSON)
 * [currentEventId]  — denormalized for quick look-ups without deserializing the blob
 * [updatedAt]       — epoch millis of last write
 */
object GameSessionsTable : Table("game_sessions") {
    val userId         = varchar("user_id", 36).references(UsersTable.id)
    val stateJson      = text("state_json")
    val currentEventId = varchar("current_event_id", 100)
    val updatedAt      = long("updated_at")

    override val primaryKey = PrimaryKey(userId)
}
