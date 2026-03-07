package kz.fearsom.financiallifev2.data

/**
 * Cross-platform secure key-value store.
 *
 * Android actual → EncryptedSharedPreferences (AES-256-GCM + AES-256-SIV)
 * iOS actual     → Keychain (kSecClassGenericPassword, afterFirstUnlock)
 *
 * All methods are synchronous. Call from a non-main dispatcher if needed.
 */
expect class SecureStorage {
    fun save(key: String, value: String)
    fun get(key: String): String?
    fun clear(key: String)
}
