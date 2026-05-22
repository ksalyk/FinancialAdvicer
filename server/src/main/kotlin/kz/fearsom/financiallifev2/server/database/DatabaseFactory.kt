package kz.fearsom.financiallifev2.server.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {

    /**
     * Connects to PostgreSQL and returns the [Database] instance.
     *
     * Schema creation/migration is now handled exclusively by [MigrationRunner] — the old
     * `SchemaUtils.createMissingTablesAndColumns` call has been removed from here so the
     * migration framework is the single source of truth for DDL changes.
     */
    fun init(): Database {
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

        return Database.connect(HikariDataSource(config))
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
