package kz.fearsom.financiallifev2.server.database.migrations.versions

import kz.fearsom.financiallifev2.server.database.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Adds a covering index on `completed_sessions(ending)` for fast ending-distribution queries.
 *
 * This is the recommended pattern for adding DDL that can't be expressed via Exposed table DSL:
 * use raw `exec()` with `IF EXISTS` / `IF NOT EXISTS` guards to keep [up] idempotent.
 */
object V002_AddStatisticsIndex : Migration {
    override val version = 2
    override val description = "Add completed_sessions(ending) index for statistics queries"

    override suspend fun up(db: Database) {
        newSuspendedTransaction(db = db) {
            exec(
                """
                CREATE INDEX IF NOT EXISTS idx_completed_sessions_ending
                ON completed_sessions(ending)
                """.trimIndent()
            )
        }
    }

    override suspend fun down(db: Database) {
        newSuspendedTransaction(db = db) {
            exec("DROP INDEX IF EXISTS idx_completed_sessions_ending")
        }
    }
}
