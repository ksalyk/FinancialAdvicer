package kz.fearsom.financiallifev2.server.auth

import java.security.MessageDigest

/**
 * SHA-256 hex helper shared by the auth layer.
 *
 * Used for two distinct purposes, both appropriate for SHA-256:
 *  - hashing high-entropy refresh tokens at rest (see DatabaseUserRepository) — a fast
 *    hash is fine because a 122-bit random UUID is not brute-forceable;
 *  - the fixed-length inner pre-hash inside [PasswordHasher] (NOT used as the password
 *    hash on its own — bcrypt wraps it).
 */
internal fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
