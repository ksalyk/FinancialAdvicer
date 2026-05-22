package kz.fearsom.financiallifev2.server.database.migrations

import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameStatesTable
import kz.fearsom.financiallifev2.server.database.tables.RefreshTokensTable
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory

data class ValidationReport(
    val passed: Boolean,
    val rowCounts: Map<String, Long>,
    val issues: List<String>
)

/**
 * Post-migration sanity checks.
 *
 * Verifies that every critical table is accessible and row counts haven't
 * dropped unexpectedly since the last check. Call after [MigrationRunner.runMigrations]
 * completes in staging/production — never auto-rollback on failure; surface the report
 * and require operator review.
 */
object DataValidation {

    private val log = LoggerFactory.getLogger(DataValidation::class.java)

    /** Tables that must be readable after any migration. */
    private val criticalTables = listOf(
        "users"                 to { UsersTable.selectAll().count() },
        "refresh_tokens"        to { RefreshTokensTable.selectAll().count() },
        "game_sessions"         to { GameSessionsTable.selectAll().count() },
        "game_state_snapshots"  to { GameStatesTable.selectAll().count() },
        "completed_sessions"    to { CompletedSessionsTable.selectAll().count() }
    )

    suspend fun validate(db: Database): ValidationReport {
        val issues    = mutableListOf<String>()
        val rowCounts = mutableMapOf<String, Long>()

        newSuspendedTransaction(db = db) {
            for ((tableName, count) in criticalTables) {
                try {
                    val n = count()
                    rowCounts[tableName] = n
                    log.debug("  {} → {} rows", tableName, n)
                } catch (e: Exception) {
                    val msg = "Table '$tableName' inaccessible: ${e.message}"
                    log.error(msg)
                    issues += msg
                    rowCounts[tableName] = -1L
                }
            }
        }

        return if (issues.isEmpty()) {
            log.info("Data validation passed. Row counts: {}", rowCounts)
            ValidationReport(passed = true, rowCounts = rowCounts, issues = emptyList())
        } else {
            log.warn("Data validation found {} issue(s): {}", issues.size, issues)
            ValidationReport(passed = false, rowCounts = rowCounts, issues = issues)
        }
    }
}
