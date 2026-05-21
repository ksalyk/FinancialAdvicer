package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.server.database.tables.GameSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameStatesTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/** PostgreSQL-backed implementation of [GameRepository] via Exposed ORM. */
class DatabaseGameRepository : GameRepository {

    // ══════════════════════════════════════════════════════════════════════════
    //  Live session (GameSessionsTable) — upserted on every auto-save
    // ══════════════════════════════════════════════════════════════════════════

    override suspend fun loadSession(userId: String): GameSessionRow? =
        newSuspendedTransaction {
            GameSessionsTable
                .selectAll()
                .where { GameSessionsTable.userId eq userId }
                .singleOrNull()
                ?.toSessionRow()
        }

    override suspend fun upsertSession(
        userId: String,
        stateJson: String,
        currentEventId: String
    ) {
        val now = System.currentTimeMillis()
        newSuspendedTransaction {
            val exists = GameSessionsTable
                .selectAll()
                .where { GameSessionsTable.userId eq userId }
                .count() > 0

            if (exists) {
                GameSessionsTable.update({ GameSessionsTable.userId eq userId }) {
                    it[GameSessionsTable.stateJson]      = stateJson
                    it[GameSessionsTable.currentEventId] = currentEventId
                    it[GameSessionsTable.updatedAt]      = now
                }
            } else {
                GameSessionsTable.insert {
                    it[GameSessionsTable.userId]         = userId
                    it[GameSessionsTable.stateJson]      = stateJson
                    it[GameSessionsTable.currentEventId] = currentEventId
                    it[GameSessionsTable.updatedAt]      = now
                }
            }
        }
    }

    override suspend fun deleteSession(userId: String) {
        newSuspendedTransaction {
            GameSessionsTable.deleteWhere { GameSessionsTable.userId eq userId }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  Snapshots (GameStatesTable) — explicit checkpoint saves
    // ══════════════════════════════════════════════════════════════════════════

    override suspend fun saveSnapshot(
        userId: String,
        stateJson: String,
        slotName: String
    ): GameSnapshotRow {
        val snapshotId = UUID.randomUUID().toString()
        val now        = System.currentTimeMillis()

        newSuspendedTransaction {
            GameStatesTable.insert {
                it[GameStatesTable.snapshotId] = snapshotId
                it[GameStatesTable.userId]     = userId
                it[GameStatesTable.slotName]   = slotName
                it[GameStatesTable.stateJson]  = stateJson
                it[GameStatesTable.savedAt]    = now
            }
        }

        return GameSnapshotRow(snapshotId, userId, slotName, stateJson, now)
    }

    /** Returns snapshots for a user ordered by most-recent first. */
    override suspend fun listSnapshots(userId: String, limit: Int): List<GameSnapshotRow> =
        newSuspendedTransaction {
            GameStatesTable
                .selectAll()
                .where { GameStatesTable.userId eq userId }
                .orderBy(GameStatesTable.savedAt, SortOrder.DESC)
                .limit(limit)
                .map { it.toSnapshotRow() }
        }

    override suspend fun loadSnapshot(snapshotId: String): GameSnapshotRow? =
        newSuspendedTransaction {
            GameStatesTable
                .selectAll()
                .where { GameStatesTable.snapshotId eq snapshotId }
                .singleOrNull()
                ?.toSnapshotRow()
        }

    override suspend fun deleteSnapshot(snapshotId: String) {
        newSuspendedTransaction {
            GameStatesTable.deleteWhere { GameStatesTable.snapshotId eq snapshotId }
        }
    }

    // ── Mapping ───────────────────────────────────────────────────────────────

    private fun ResultRow.toSessionRow() = GameSessionRow(
        userId         = this[GameSessionsTable.userId],
        stateJson      = this[GameSessionsTable.stateJson],
        currentEventId = this[GameSessionsTable.currentEventId],
        updatedAt      = this[GameSessionsTable.updatedAt]
    )

    private fun ResultRow.toSnapshotRow() = GameSnapshotRow(
        snapshotId = this[GameStatesTable.snapshotId],
        userId     = this[GameStatesTable.userId],
        slotName   = this[GameStatesTable.slotName],
        stateJson  = this[GameStatesTable.stateJson],
        savedAt    = this[GameStatesTable.savedAt]
    )
}
