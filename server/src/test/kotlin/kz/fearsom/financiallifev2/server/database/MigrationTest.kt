package kz.fearsom.financiallifev2.server.database

import kotlinx.coroutines.runBlocking
import kz.fearsom.financiallifev2.server.database.migrations.DataValidation
import kz.fearsom.financiallifev2.server.database.migrations.MigrationRunner
import kz.fearsom.financiallifev2.server.database.migrations.MigrationVersion
import kz.fearsom.financiallifev2.server.database.migrations.SchemaVersionsTable
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Verifies the full migration pipeline using an isolated in-memory H2 database.
 *
 * Each test gets a fresh H2 instance (distinct JDBC URLs) to guarantee full isolation —
 * unlike [DatabaseTestFixture] which is shared across tests for speed, here we need
 * to test the migration sequence itself, including `schema_versions` state.
 */
class MigrationTest {

    private lateinit var db: Database
    private var dbIndex = 0

    @Before
    fun setup() {
        // Use a unique DB name per test to avoid cross-test contamination.
        db = Database.connect(
            url    = "jdbc:h2:mem:migration_test_${dbIndex++};DB_CLOSE_DELAY=-1;" +
                     "MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
            driver = "org.h2.Driver"
        )
    }

    @After
    fun teardown() {
        // H2 in-memory DB is automatically cleaned up when last connection closes.
    }

    // ── Migration sequence ─────────────────────────────────────────────────────

    @Test
    fun `runMigrations applies all pending migrations on fresh database`() = runBlocking {
        val runner = MigrationRunner(db)
        runner.runMigrations()

        val applied = appliedVersions(db)
        assertEquals(
            MigrationVersion.entries.map { it.version }.toSet(),
            applied,
            "All registered migrations should be recorded as applied"
        )
    }

    @Test
    fun `runMigrations is idempotent - second run is a no-op`() = runBlocking {
        val runner = MigrationRunner(db)
        runner.runMigrations()
        val firstCount = appliedVersions(db).size

        runner.runMigrations()  // second run
        val secondCount = appliedVersions(db).size

        assertEquals(firstCount, secondCount, "Second run must not apply duplicate migrations")
    }

    @Test
    fun `schema_versions records correct version numbers after migration`() = runBlocking {
        MigrationRunner(db).runMigrations()

        val recorded = newSuspendedTransaction(db = db) {
            SchemaVersionsTable.selectAll().map { it[SchemaVersionsTable.version] }.toSet()
        }

        for (version in MigrationVersion.entries) {
            assertTrue(
                version.version in recorded,
                "Version ${version.version} (${version.name}) should be in schema_versions"
            )
        }
    }

    @Test
    fun `schema_versions marks all applied migrations as successful`() = runBlocking {
        MigrationRunner(db).runMigrations()

        val failedMigrations = newSuspendedTransaction(db = db) {
            SchemaVersionsTable.selectAll()
                .where { SchemaVersionsTable.success eq false }
                .count()
        }

        assertEquals(0L, failedMigrations, "No migrations should be recorded as failed")
    }

    @Test
    fun `V001 rollback drops all application tables`() = runBlocking {
        val runner = MigrationRunner(db)
        runner.runMigrations()

        // Roll back only V001 (the one that created all tables).
        runner.rollback(1)

        // After rollback, the users table should no longer exist.
        val tableExists = runCatching {
            transaction(db) { exec("SELECT COUNT(*) FROM users") }
            true
        }.getOrDefault(false)

        assertEquals(false, tableExists, "users table should not exist after V001 rollback")
    }

    @Test
    fun `data validation passes after migrations`() = runBlocking {
        MigrationRunner(db).runMigrations()

        val report = DataValidation.validate(db)

        assertTrue(report.passed, "Validation should pass after clean migration; issues: ${report.issues}")
        assertTrue(report.rowCounts.isNotEmpty())
    }

    @Test
    fun `fresh database has zero rows in all tables`() = runBlocking {
        MigrationRunner(db).runMigrations()

        val report = DataValidation.validate(db)

        assertTrue(report.passed)
        // All counts should be 0 on a freshly migrated empty DB.
        for ((table, count) in report.rowCounts) {
            assertEquals(0L, count, "Table '$table' should be empty after migration")
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private suspend fun appliedVersions(db: Database): Set<Int> =
        newSuspendedTransaction(db = db) {
            SchemaVersionsTable
                .selectAll()
                .where { SchemaVersionsTable.success eq true }
                .map { it[SchemaVersionsTable.version] }
                .toSet()
        }
}
