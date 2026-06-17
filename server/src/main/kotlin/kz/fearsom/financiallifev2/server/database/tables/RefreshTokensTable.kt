package kz.fearsom.financiallifev2.server.database.tables

import org.jetbrains.exposed.v1.core.Table

/**
 * Persisted refresh tokens.
 *
 * token     — SHA-256 hex (64 chars) of the raw refresh token, used as PK. The raw token
 *             (a ~122-bit random UUID) is returned to the client but never stored, so a DB
 *             leak cannot be replayed to impersonate sessions.
 * userId    — FK to users; use to revoke all tokens for a user on logout.
 * expiresAt — epoch millis; checked server-side on every refresh call.
 * createdAt — epoch millis; useful for auditing.
 */
object RefreshTokensTable : Table("refresh_tokens") {
    val token     = varchar("token", 64)                        // SHA-256 hex of raw token
    val userId    = varchar("user_id", 36).references(UsersTable.id)
    val expiresAt = long("expires_at")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(token)

    // Fast lookup when revoking all tokens for a user (logout all devices).
    val userIdx = index("idx_refresh_tokens_user", isUnique = false, userId)
}
