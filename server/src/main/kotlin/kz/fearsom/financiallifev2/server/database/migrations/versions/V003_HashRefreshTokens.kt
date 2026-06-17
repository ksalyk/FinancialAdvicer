package kz.fearsom.financiallifev2.server.database.migrations.versions

import kz.fearsom.financiallifev2.server.database.migrations.Migration
import kz.fearsom.financiallifev2.server.database.tables.RefreshTokensTable
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction

/**
 * Refresh tokens are now stored SHA-256-hashed (64 hex chars) rather than as the raw
 * UUID (36 chars). Because refresh tokens are disposable, the simplest dialect-safe
 * change (works on both PostgreSQL and the H2 used in tests) is to drop and recreate the
 * table with the wider [RefreshTokensTable.token] column via the Exposed schema DSL —
 * avoiding hand-written, dialect-specific `ALTER COLUMN ... TYPE` SQL.
 *
 * Intended side effect: every existing plaintext token is invalidated, forcing a one-time
 * re-login. The client's refresh-failure path already handles this (logout → prompt login).
 */
object V003_HashRefreshTokens : Migration {
    override val version = 3
    override val description = "Hash refresh tokens at rest (widen token column to SHA-256 hex)"

    override suspend fun up(db: Database) {
        newSuspendedTransaction(db = db) {
            // refresh_tokens is a leaf table (nothing references it), so dropping is safe.
            SchemaUtils.drop(RefreshTokensTable)
            SchemaUtils.createMissingTablesAndColumns(RefreshTokensTable)
        }
    }

    override suspend fun down(db: Database) {
        // Not a true downgrade — the column merely becomes wider again. Recreate the table
        // so the schema stays consistent if an operator rolls back. Existing tokens clear.
        newSuspendedTransaction(db = db) {
            SchemaUtils.drop(RefreshTokensTable)
            SchemaUtils.createMissingTablesAndColumns(RefreshTokensTable)
        }
    }
}
