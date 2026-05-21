package kz.fearsom.financiallifev2.server.database.migrations

import org.jetbrains.exposed.sql.Database

/**
 * Contract for a single schema migration step.
 *
 * Migrations are ordered by [version]. [MigrationRunner] applies them in ascending
 * order and records each result in the `schema_versions` table.
 *
 * Rules:
 * - [up] must be idempotent when using `CREATE TABLE IF NOT EXISTS` / `IF NOT EXISTS` guards.
 * - [down] must fully undo every change made by [up].
 * - Throw any exception from [up] to abort and mark the migration as failed.
 *   Never auto-rollback from [up] — let [MigrationRunner] surface the failure so an operator
 *   can inspect the database before deciding to run [down] manually.
 */
interface Migration {
    /** Monotonically increasing sequence number (1, 2, 3, …). */
    val version: Int

    /** Human-readable summary logged on apply. */
    val description: String

    /** Forward migration — apply schema changes. */
    suspend fun up(db: Database)

    /** Rollback — revert all changes made by [up]. Used only for manual operator rollback. */
    suspend fun down(db: Database)
}
