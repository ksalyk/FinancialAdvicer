package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Persistent user store.
 *
 * id           — UUID string PK, generated server-side at creation time
 * username     — unique, case-insensitive (stored lowercase)
 * passwordHash — SHA-256 hex of the raw password
 * createdAt    — epoch millis
 */
object UsersTable : Table("users") {
    val id           = varchar("id", 36)                    // UUID as string
    val username     = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 64)         // SHA-256 = 64 hex chars
    val createdAt    = long("created_at")

    override val primaryKey = PrimaryKey(id)
}
