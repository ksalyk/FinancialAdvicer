package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.admin.AdminUserRow
import kz.fearsom.financiallifev2.server.auth.JwtConfig
import kz.fearsom.financiallifev2.server.auth.PasswordHasher
import kz.fearsom.financiallifev2.server.auth.sha256Hex
import kz.fearsom.financiallifev2.server.database.tables.CompletedSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameSessionsTable
import kz.fearsom.financiallifev2.server.database.tables.GameStatesTable
import kz.fearsom.financiallifev2.server.database.tables.RefreshTokensTable
import kz.fearsom.financiallifev2.server.database.tables.UsersTable
import kz.fearsom.financiallifev2.server.models.ServerUser
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.like
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.newSuspendedTransaction
import java.util.UUID

/** PostgreSQL-backed implementation of [UserRepository] via Exposed ORM. */
class DatabaseUserRepository(private val db: Database) : UserRepository {

    // ── User Read ─────────────────────────────────────────────────────────────

    override suspend fun findByUsername(username: String): ServerUser? =
        newSuspendedTransaction(db = db) {
            UsersTable
                .selectAll()
                .where { UsersTable.username eq username.lowercase() }
                .singleOrNull()
                ?.toServerUser()
        }

    override suspend fun findById(id: String): ServerUser? =
        newSuspendedTransaction(db = db) {
            UsersTable
                .selectAll()
                .where { UsersTable.id eq id }
                .singleOrNull()
                ?.toServerUser()
        }

    override suspend fun existsByUsername(username: String): Boolean =
        newSuspendedTransaction(db = db) {
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
    override suspend fun create(username: String, rawPassword: String): ServerUser {
        val id        = UUID.randomUUID().toString()
        val lowerName = username.lowercase().trim()
        val hash      = PasswordHasher.hash(rawPassword)
        val now       = System.currentTimeMillis()

        newSuspendedTransaction(db = db) {
            UsersTable.insert {
                it[UsersTable.id]           = id
                it[UsersTable.username]     = lowerName
                it[UsersTable.passwordHash] = hash
                it[UsersTable.createdAt]    = now
            }
        }

        return ServerUser(id = id, username = lowerName, passwordHash = hash, createdAt = now)
    }

    // ── Auth helper ──────────────────────────────────────────────────────────

    override fun verifyPassword(rawPassword: String, storedHash: String): Boolean =
        PasswordHasher.verify(rawPassword, storedHash)

    // ── Refresh Tokens ───────────────────────────────────────────────────────

    /**
     * Persists a new refresh token and returns the raw UUID to send to the client.
     * Only the SHA-256 hash is stored, so a leaked DB dump never exposes usable tokens.
     * The raw UUID has ~122 bits of entropy, so a fast unsalted hash is sufficient here
     * (unlike passwords, which are low-entropy and use bcrypt).
     */
    override suspend fun createRefreshToken(userId: String): String {
        val raw = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        newSuspendedTransaction(db = db) {
            RefreshTokensTable.insert {
                it[RefreshTokensTable.token]     = sha256Hex(raw)
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
    override suspend fun consumeRefreshToken(rawToken: String): ServerUser? {
        val now = System.currentTimeMillis()
        val tokenHash = sha256Hex(rawToken)

        return newSuspendedTransaction(db = db) {
            val row = RefreshTokensTable
                .selectAll()
                .where { RefreshTokensTable.token eq tokenHash }
                .singleOrNull()
                ?: return@newSuspendedTransaction null

            val userId    = row[RefreshTokensTable.userId]
            val expiresAt = row[RefreshTokensTable.expiresAt]

            // Check expiry BEFORE deleting. Deleting first breaks idempotent retries:
            // if the client retries after a network timeout, the token is already gone
            // and the server returns null → onTokenRefreshFailed → logout.
            if (expiresAt < now) {
                RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq tokenHash }
                return@newSuspendedTransaction null
            }

            RefreshTokensTable.deleteWhere { RefreshTokensTable.token eq tokenHash }

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
    override suspend fun revokeAllTokens(userId: String) {
        newSuspendedTransaction(db = db) {
            RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
        }
    }

    // ── Admin operations ─────────────────────────────────────────────────────

    override suspend fun listUsers(limit: Int, offset: Long, search: String?): List<AdminUserRow> =
        newSuspendedTransaction(db = db) {
            val query = UsersTable.selectAll()
            if (!search.isNullOrBlank()) {
                query.where { UsersTable.username like "%${search.lowercase()}%" }
            }
            val userRows = query
                .orderBy(UsersTable.createdAt, SortOrder.DESC)
                .limit(limit)
                .offset(offset)
                .map { it.toServerUser() }

            if (userRows.isEmpty()) return@newSuspendedTransaction emptyList()

            // Batch-count completed games per user in one query — no N+1.
            val userIds = userRows.map { it.id }
            val gameCounts: Map<String, Int> = CompletedSessionsTable
                .selectAll()
                .where { CompletedSessionsTable.userId inList userIds }
                .groupBy { it[CompletedSessionsTable.userId] }
                .mapValues { (_, rows) -> rows.size }

            userRows.map { u ->
                AdminUserRow(
                    id          = u.id,
                    username    = u.username,
                    createdAt   = u.createdAt,
                    gamesPlayed = gameCounts[u.id] ?: 0
                )
            }
        }

    override suspend fun countUsers(search: String?): Long =
        newSuspendedTransaction(db = db) {
            val query = UsersTable.selectAll()
            if (!search.isNullOrBlank()) {
                query.where { UsersTable.username like "%${search.lowercase()}%" }
            }
            query.count()
        }

    override suspend fun updatePassword(userId: String, rawPassword: String): Boolean {
        val hash = PasswordHasher.hash(rawPassword)
        return newSuspendedTransaction(db = db) {
            val updated = UsersTable.update({ UsersTable.id eq userId }) {
                it[passwordHash] = hash
            }
            if (updated > 0) {
                // Revoke all tokens so existing sessions must re-auth.
                RefreshTokensTable.deleteWhere { RefreshTokensTable.userId eq userId }
                true
            } else false
        }
    }

    /**
     * Re-hashes the password under the current scheme WITHOUT revoking tokens.
     * Used for transparent upgrade of legacy SHA-256 hashes on successful login.
     */
    override suspend fun rehashPassword(userId: String, rawPassword: String) {
        val hash = PasswordHasher.hash(rawPassword)
        newSuspendedTransaction(db = db) {
            UsersTable.update({ UsersTable.id eq userId }) {
                it[passwordHash] = hash
            }
        }
    }

    override suspend fun deleteUserCascade(userId: String): Boolean =
        newSuspendedTransaction(db = db) {
            // Delete child rows first (no DB-level FK cascades), then the user row.
            RefreshTokensTable.deleteWhere    { RefreshTokensTable.userId    eq userId }
            GameStatesTable.deleteWhere       { GameStatesTable.userId       eq userId }
            GameSessionsTable.deleteWhere     { GameSessionsTable.userId     eq userId }
            CompletedSessionsTable.deleteWhere { CompletedSessionsTable.userId eq userId }
            val deleted = UsersTable.deleteWhere { UsersTable.id eq userId }
            deleted > 0
        }

    // ── Mapping ──────────────────────────────────────────────────────────────

    private fun ResultRow.toServerUser() = ServerUser(
        id           = this[UsersTable.id],
        username     = this[UsersTable.username],
        passwordHash = this[UsersTable.passwordHash],
        createdAt    = this[UsersTable.createdAt]
    )
}
