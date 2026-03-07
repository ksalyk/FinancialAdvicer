package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.sql.Table

/**
 * Persisted refresh tokens.
 *
 * token     — raw UUID string, used as PK. High-entropy random value,
 *             safe to store without additional hashing (128-bit).
 * userId    — FK to users; use to revoke all tokens for a user on logout.
 * expiresAt — epoch millis; checked server-side on every refresh call.
 * createdAt — epoch millis; useful for auditing.
 */
object RefreshTokensTable : Table("refresh_tokens") {
    val token     = varchar("token", 36)                        // UUID string
    val userId    = varchar("user_id", 36).references(UsersTable.id)
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(token)

    // Fast lookup when revoking all tokens for a user (logout all devices).
    val userIdx = index("idx_refresh_tokens_user", isUnique = false, userId)
}
