package kz.fearsom.financiallifev2.server.database.migrations.versions

import kz.fearsom.financiallifev2.server.database.migrations.Migration
import kz.fearsom.financiallifev2.server.database.tables.CharactersTable
import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.ErasTable
import kz.fearsom.financiallifev2.server.database.tables.GameSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameStatesTable
import kz.fearsom.financiallifev2.server.database.tables.RefreshTokensTable
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Initial schema: creates all core tables and indexes.
 *
 * [up]   — idempotent via `SchemaUtils.createMissingTablesAndColumns`.
 * [down] — drops all tables in FK-safe reverse order (children before parents).
 *          Intended only for test teardown and manual operator rollback.
 */
object V001_InitialSchema : Migration {
    override val version = 1
    override val description =
        "Initial schema: users, refresh_tokens, game_sessions, game_state_snapshots, " +
        "completed_sessions, characters, eras"

    override suspend fun up(db: Database) {
        newSuspendedTransaction(db = db) {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                RefreshTokensTable,
                GameSessionsTable,
                GameStatesTable,
                CompletedSessionsTable,
                CharactersTable,
                ErasTable
            )
        }
    }

    override suspend fun down(db: Database) {
        newSuspendedTransaction(db = db) {
            // Drop children (FK dependants) before parents to avoid constraint violations.
            SchemaUtils.drop(
                CompletedSessionsTable,  // references users
                GameStatesTable,         // references users
                GameSessionsTable,       // references users
                RefreshTokensTable,      // references users
                CharactersTable,
                ErasTable,
                UsersTable               // parent — drop last
            )
        }
    }
}
