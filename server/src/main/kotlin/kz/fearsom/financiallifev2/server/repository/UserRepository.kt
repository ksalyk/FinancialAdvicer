package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.server.auth.JwtConfig
import kz.fearsom.financiallifev2.server.database.tables.RefreshTokensTable
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import kz.fearsom.financiallifev2.server.models.ServerUser
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.security.MessageDigest
import java.util.UUID

class UserRepository {

    // ── User Read ─────────────────────────────────────────────────────────────

    suspend fun findByUsername(username: String): ServerUser? =
        newSuspendedTransaction {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username.lowercase() }
                .singleOrNull()
                ?.toServerUser()
        }

    suspend fun findById(id: String): ServerUser? =
        newSuspendedTransaction {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?.toServerUser()
        }

    suspend fun existsByUsername(username: String): Boolean =
        newSuspendedTransaction {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username.lowercase() }
                .count() > 0
        }

    // ── User Write ────────────────────────────────────────────────────────────

    /**
     * Inserts a new user row.
     * Password is hashed here — callers pass the raw password.
     * Returns the created [ServerUser] (never returns null; throws on conflict).
     */
    suspend fun create(username: String, rawPassword: String): ServerUser {
        val id        = UUID.randomUUID().toString()
        val lowerName = username.lowercase().trim()
        val hash      = sha256Hex(rawPassword)
        val now       = System.currentTimeMillis()

        newSuspendedTransaction {
            UsersTable.insert {
                it[UsersTable.id]           = id
                it[UsersTable.username]     = lowerName
                it[UsersTable.passwordHash] = hash
                it[UsersTable.createdAt]    = now
            }
        }

        return ServerUser(
            id           = id,
            username     = lowerName,
            passwordHash = hash,
            createdAt    = now
        )
    }

    // ── Auth helper ──────────────────────────────────────────────────────────

    fun verifyPassword(rawPassword: String, storedHash: String): Boolean =
        sha256Hex(rawPassword) == storedHash

    // ── Refresh Tokens ───────────────────────────────────────────────────────

    /**
     * Persists a new refresh token and returns the raw UUID to send to the client.
     * Tokens are stored as plain UUIDs (128-bit entropy = cryptographically safe).
     */
    suspend fun createRefreshToken(userId: String): String {
        val raw       = UUID.randomUUID().toString()
        val now       = System.currentTimeMillis()

        newSuspendedTransaction {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.token]     = raw
                it[RefreshTokensTable.userId]    = userId
                it[RefreshTokensTable.expiresAt] = now + JwtConfig.REFRESH_TTL_MS
                it[RefreshTokensTable.createdAt] = now
            }
        }

        return raw
    }

    /**
     * Validates and rotates a refresh token in a single transaction:
     *  1. Looks up the token — returns null if not found.
     *  2. Deletes it immediately (one-time use; new token issued on success).
     *  3. Returns null if the token was expired.
     *  4. Returns the owning [ServerUser] on success.
     */
    suspend fun consumeRefreshToken(rawToken: String): ServerUser? {
        val now = System.currentTimeMillis()

        return newSuspendedTransaction {
            val row = RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.token eq rawToken }
                .singleOrNull()
                ?: return@newSuspendedTransaction null

            val userId    = row[RefreshTokensTable.userId]
            val expiresAt = row[RefreshTokensTable.expiresAt]

            // Check expiry BEFORE deleting. Deleting first breaks idempotent retries:
            // if the client retries after a network timeout, the token is already gone
            // and the server returns null → onTokenRefreshFailed → logout, even though
            // the token was perfectly valid. Expired tokens are still deleted to avoid
            // leaking dead rows.
            if (expiresAt < now) {
                RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq rawToken }
                return@newSuspendedTransaction null
            }

            RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq rawToken }

            UsersTable
                .selectAll()
                .where { UsersTable.id eq userId }
                .singleOrNull()
                ?.toServerUser()
        }
    }

    /**
     * Revokes all refresh tokens for a user.
     * Call on explicit logout or password change to invalidate all sessions.
     */
    suspend fun revokeAllTokens(userId: String) {
        newSuspendedTransaction {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }
    }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun ResultRow.toServerUser() = ServerUser(
        id           = this[UsersTable.id],
        username     = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        createdAt    = this[UsersTable.createdAt]
    )

    // ── Crypto ───────────────────────────────────────────────────────────────

    private fun sha256Hex(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
