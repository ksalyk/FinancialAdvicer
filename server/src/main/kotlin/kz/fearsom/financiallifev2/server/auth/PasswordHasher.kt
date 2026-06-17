package kz.fearsom.financiallifev2.server.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import java.security.MessageDigest

/**
 * Password hashing for user accounts.
 *
 * New hashes: bcrypt (cost [COST]) computed over the SHA-256 hex of the password.
 * The inner SHA-256 step yields a fixed 64-char ASCII input, which:
 *   - sidesteps bcrypt's 72-byte truncation limit (important for multi-byte Cyrillic
 *     passphrases — 72 bytes is only ~36 Cyrillic characters), and
 *   - produces a standard 60-char bcrypt string that fits the existing
 *     `users.password_hash` VARCHAR(64) column with no schema change.
 *
 * Legacy hashes: bare unsalted SHA-256 hex (the pre-bcrypt scheme). [verify] still
 * accepts them so existing accounts keep working; [needsRehash] flags such a hash for
 * transparent upgrade on the next successful login (see AuthRoutes).
 */
object PasswordHasher {

    /** bcrypt work factor (2^COST rounds). ~250 ms/verify on modern server hardware. */
    private const val COST = 12

    /** Pre-computed hash of a constant string, used to equalise login timing for unknown users. */
    private val DUMMY_HASH: String = hash("__constant_time_dummy_password__")

    /** Produces a current-scheme hash (bcrypt over the SHA-256 hex of [rawPassword]). */
    fun hash(rawPassword: String): String =
        BCrypt.withDefaults().hashToString(COST, sha256Hex(rawPassword).toCharArray())

    /** Verifies [rawPassword] against [storedHash], transparently supporting legacy SHA-256 hashes. */
    fun verify(rawPassword: String, storedHash: String): Boolean =
        if (isBcrypt(storedHash)) {
            BCrypt.verifyer()
                .verify(sha256Hex(rawPassword).toCharArray(), storedHash)
                .verified
        } else {
            // Legacy unsalted SHA-256 hex — constant-time comparison.
            MessageDigest.isEqual(
                sha256Hex(rawPassword).toByteArray(Charsets.US_ASCII),
                storedHash.toByteArray(Charsets.US_ASCII)
            )
        }

    /** True when [storedHash] is not a current-scheme bcrypt hash and should be upgraded. */
    fun needsRehash(storedHash: String): Boolean = !isBcrypt(storedHash)

    /**
     * Runs a throwaway bcrypt verify against a fixed dummy hash. Call when the username
     * does not exist so the response time matches the real-user path — an attacker then
     * cannot distinguish "no such user" from "wrong password" by timing.
     */
    fun verifyDummy(rawPassword: String) {
        BCrypt.verifyer().verify(sha256Hex(rawPassword).toCharArray(), DUMMY_HASH)
    }

    private fun isBcrypt(hash: String): Boolean =
        hash.startsWith("\$2a\$") || hash.startsWith("\$2b\$") || hash.startsWith("\$2y\$")
}
