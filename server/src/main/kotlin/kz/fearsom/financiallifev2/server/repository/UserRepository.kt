package kz.fearsom.financiallifev2.server.repository

import kz.fearsom.financiallifev2.admin.AdminUserRow
import kz.fearsom.financiallifev2.server.models.ServerUser

/**
 * Defines all user-account and token operations required by the auth layer.
 *
 * The concrete PostgreSQL-backed implementation is [DatabaseUserRepository].
 * Test code can implement this interface with an in-memory fake without touching
 * any database — see `MockUserRepository` in `AuthRoutesTest`.
 */
interface UserRepository {

    // ── User reads ─────────────────────────────────────────────────────────────

    suspend fun findByUsername(username: String): ServerUser?
    suspend fun findById(id: String): ServerUser?
    suspend fun existsByUsername(username: String): Boolean

    // ── User writes ────────────────────────────────────────────────────────────

    /** Hashes [rawPassword] internally — callers pass the plain password. */
    suspend fun create(username: String, rawPassword: String): ServerUser

    // ── Auth helpers ───────────────────────────────────────────────────────────

    /** Compares [rawPassword] against the stored hash. Pure function — no suspend needed. */
    fun verifyPassword(rawPassword: String, storedHash: String): Boolean

    // ── Refresh tokens ─────────────────────────────────────────────────────────

    suspend fun createRefreshToken(userId: String): String
    suspend fun consumeRefreshToken(rawToken: String): ServerUser?
    suspend fun revokeAllTokens(userId: String)

    // ── Admin operations ───────────────────────────────────────────────────────

    /** Paginated user list with optional username search. Never includes passwordHash. */
    suspend fun listUsers(limit: Int, offset: Long, search: String?): List<AdminUserRow>

    /** Total user count, optionally filtered by [search]. */
    suspend fun countUsers(search: String?): Long

    /**
     * Hashes [rawPassword] and updates the user's stored hash,
     * then revokes all existing refresh tokens for the user.
     * Returns false if [userId] not found.
     */
    suspend fun updatePassword(userId: String, rawPassword: String): Boolean

    /**
     * Hard-deletes a user and all their data in one transaction:
     * refresh_tokens, game_state_snapshots, game_sessions, completed_sessions, users.
     * Returns false if [userId] not found.
     */
    suspend fun deleteUserCascade(userId: String): Boolean
}
