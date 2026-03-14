package kz.fearsom.financiallifev2.server.database

import kz.fearsom.financiallifev2.server.database.tables.CharactersTable
import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.ErasTable
import kz.fearsom.financiallifev2.server.database.tables.GameSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameStatesTable
import kz.fearsom.financiallifev2.server.database.tables.RefreshTokensTable
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    fun init() {
        val jdbcUrl  = resolveJdbcUrl()
        val user     = System.getenv("DB_USER")     ?: "postgres"
        val password = System.getenv("DB_PASSWORD") ?: "postgres"

        val config = HikariConfig().apply {
            this.jdbcUrl         = jdbcUrl
            this.username        = user
            this.password        = password
            driverClassName      = "org.postgresql.Driver"
            maximumPoolSize      = 10
            minimumIdle          = 2
            idleTimeout          = 600_000       // 10 min
            connectionTimeout    = 30_000        // 30 sec
            maxLifetime          = 1_800_000     // 30 min
            isAutoCommit         = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }

        Database.connect(HikariDataSource(config))

        transaction {
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

    /**
     * Accepts multiple formats:
     *  - DATABASE_URL  = "postgres://user:pass@host:5432/db"  (Heroku / Railway style)
     *  - DATABASE_URL  = "jdbc:postgresql://host:5432/db"     (explicit JDBC)
     *  - DB_HOST + DB_PORT + DB_NAME env vars (Docker Compose style)
     */
    private fun resolveJdbcUrl(): String {
        val raw = System.getenv("DATABASE_URL")
        if (raw != null) {
            return when {
                raw.startsWith("postgres://")   -> raw.replaceFirst("postgres://", "jdbc:postgresql://")
                raw.startsWith("postgresql://") -> raw.replaceFirst("postgresql://", "jdbc:postgresql://")
                else -> raw   // already jdbc:postgresql://...
            }
        }

        val host = System.getenv("DB_HOST") ?: "localhost"
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "financelifeline"
        return "jdbc:postgresql://$host:$port/$name"
    }
}
