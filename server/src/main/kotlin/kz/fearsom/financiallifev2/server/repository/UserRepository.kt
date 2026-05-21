package kz.fearsom.financiallifev2.server.repository

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
}
