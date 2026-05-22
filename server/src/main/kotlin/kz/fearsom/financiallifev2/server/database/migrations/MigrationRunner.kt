package kz.fearsom.financiallifev2.server.database.migrations

import kz.fearsom.financiallifev2.server.database.migrations.versions.V001_InitialSchema
import kz.fearsom.financiallifev2.server.database.migrations.versions.V002_AddStatisticsIndex
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.slf4j.LoggerFactory

// ── Schema tracking table ──────────────────────────────────────────────────────

/**
 * Append-only log of every attempted migration.
 * [success] = false rows are retained so operators can see partial-failure history.
 */
object SchemaVersionsTable : Table("schema_versions") {
    val version     = integer("version")
    val description = varchar("description", 255)
    val appliedAt   = long("applied_at")
    val success     = bool("success")

    override val primaryKey = PrimaryKey(version)
}

// ── Runner ─────────────────────────────────────────────────────────────────────

/**
 * Discovers, orders, and runs pending [Migration]s against [db].
 *
 * Execution guarantees:
 * - Only migrations whose [version] is not yet recorded as successful are run.
 * - Migrations run in ascending [version] order.
 * - A failure in any migration immediately halts the sequence and rethrows as
 *   [MigrationException] — no auto-rollback. The operator must inspect the DB
 *   and call [down] manually if needed.
 * - Running [runMigrations] when the schema is already up-to-date is a no-op.
 */
class MigrationRunner(private val db: Database) {

    private val log = LoggerFactory.getLogger(MigrationRunner::class.java)

    /** Ordered list of all known migrations. Register new migrations here. */
    private val allMigrations: List<Migration> = listOf(
        V001_InitialSchema,
        V002_AddStatisticsIndex
    ).sortedBy { it.version }

    suspend fun runMigrations() {
        ensureSchemaVersionsTable()

        val applied  = appliedVersions()
        val pending  = allMigrations.filter { it.version !in applied }

        if (pending.isEmpty()) {
            log.info("Schema up-to-date at version {}", MigrationVersion.currentVersion())
            return
        }

        log.info("Applying {} pending migration(s)…", pending.size)

        for (migration in pending) {
            applyMigration(migration)
        }

        log.info(
            "All migrations complete. Schema version: {}",
            MigrationVersion.currentVersion()
        )
    }

    /**
     * Manually rolls back a specific migration version.
     * Call only after operator review of a failed migration state.
     */
    suspend fun rollback(targetVersion: Int) {
        val migration = allMigrations.find { it.version == targetVersion }
            ?: error("No migration registered for version $targetVersion")

        log.warn("Rolling back migration V{:03d}: {}", migration.version, migration.description)
        migration.down(db)
        removeVersionRecord(migration.version)
        log.info("Rollback of V{:03d} complete", migration.version)
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private suspend fun ensureSchemaVersionsTable() {
        newSuspendedTransaction(db = db) {
            SchemaUtils.createMissingTablesAndColumns(SchemaVersionsTable)
        }
    }

    private suspend fun appliedVersions(): Set<Int> =
        newSuspendedTransaction(db = db) {
            SchemaVersionsTable
                .selectAll()
                .where { SchemaVersionsTable.success eq true }
                .map { it[SchemaVersionsTable.version] }
                .toSet()
        }

    private suspend fun applyMigration(migration: Migration) {
        val startedAt = System.currentTimeMillis()
        log.info("→ V{:03d}: {}", migration.version, migration.description)

        try {
            migration.up(db)
            recordMigration(migration.version, migration.description, startedAt, success = true)
            log.info("✓ V{:03d} applied ({}ms)", migration.version, System.currentTimeMillis() - startedAt)
        } catch (e: Exception) {
            recordMigration(migration.version, migration.description, startedAt, success = false)
            log.error(
                "✗ V{:03d} '{}' FAILED after {}ms — halting migrations",
                migration.version, migration.description, System.currentTimeMillis() - startedAt, e
            )
            throw MigrationException(
                "Migration V${"%03d".format(migration.version)} '${migration.description}' failed",
                e
            )
        }
    }

    private suspend fun recordMigration(
        version: Int,
        description: String,
        appliedAt: Long,
        success: Boolean
    ) {
        newSuspendedTransaction(db = db) {
            // Insert on first attempt; update on retry (e.g. a failed + retried migration).
            val exists = SchemaVersionsTable
                .selectAll()
                .where { SchemaVersionsTable.version eq version }
                .count() > 0

            if (exists) {
                SchemaVersionsTable.update({ SchemaVersionsTable.version eq version }) {
                    it[SchemaVersionsTable.appliedAt] = appliedAt
                    it[SchemaVersionsTable.success]   = success
                }
            } else {
                SchemaVersionsTable.insert {
                    it[SchemaVersionsTable.version]     = version
                    it[SchemaVersionsTable.description] = description
                    it[SchemaVersionsTable.appliedAt]   = appliedAt
                    it[SchemaVersionsTable.success]     = success
                }
            }
        }
    }

    private suspend fun removeVersionRecord(version: Int) {
        newSuspendedTransaction(db = db) {
            SchemaVersionsTable
                .deleteWhere { SchemaVersionsTable.version eq version }
        }
    }
}

class MigrationException(message: String, cause: Throwable) : RuntimeException(message, cause)
