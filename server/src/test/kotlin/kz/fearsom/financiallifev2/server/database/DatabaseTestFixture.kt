package kz.fearsom.financiallifev2.server.database

import kotlinx.coroutines.runBlocking
import kz.fearsom.financiallifev2.server.database.migrations.MigrationRunner
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Shared test fixture that spins up an in-memory H2 database (PostgreSQL-compatible mode)
 * and runs the full migration pipeline against it.
 *
 * ## Usage
 * ```kotlin
 * class MyTest {
 *     private val db = DatabaseTestFixture.database
 *
 *     @Before fun setup() = DatabaseTestFixture.reset()
 *
 *     @Test fun `something works`() { ... }
 * }
 * ```
 *
 * ## H2 PostgreSQL mode
 * `MODE=PostgreSQL` makes H2 accept most PostgreSQL DDL/DML:
 * - `CREATE INDEX IF NOT EXISTS` (V002 migration)
 * - Case-insensitive identifiers (`DATABASE_TO_LOWER=TRUE`)
 * - Standard null ordering (`DEFAULT_NULL_ORDERING=HIGH`)
 *
 * ## Isolation
 * Call [reset] before each test to clear all data while keeping the schema.
 * Avoids the overhead of dropping/recreating tables per test.
 */
object DatabaseTestFixture {

    val database: Database by lazy {
        val db = Database.connect(
            url    = "jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL;" +
                     "DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver"
        )
        runBlocking { MigrationRunner(db).runMigrations() }
        db
    }

    /**
     * Truncates all application tables so each test starts with a clean slate.
     * The `schema_versions` tracking table is intentionally left intact —
     * re-running migrations after a reset should be a no-op.
     */
    fun reset() {
        transaction(database) {
            exec("SET REFERENTIAL_INTEGRITY FALSE")
            exec("TRUNCATE TABLE completed_sessions")
            exec("TRUNCATE TABLE game_state_snapshots")
            exec("TRUNCATE TABLE game_sessions")
            exec("TRUNCATE TABLE refresh_tokens")
            exec("TRUNCATE TABLE eras")
            exec("TRUNCATE TABLE characters")
            exec("TRUNCATE TABLE users")
            exec("SET REFERENTIAL_INTEGRITY TRUE")
        }
    }
}
