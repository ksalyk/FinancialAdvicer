package kz.fearsom.financiallifev2.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore

/**
 * Android Keystore-backed secure storage.
 *
 * - AES-256-GCM via AndroidKeyStore provider (hardware-backed on supported devices).
 * - Values are stored as Base64(IV[12] || ciphertext) in a private SharedPreferences file.
 * - Zero external dependencies — no security-crypto needed.
 * - Requires API 23+; our minSdk is 26 so no compat shim needed.
 */
actual class SecureStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    // ── Android Keystore ─────────────────────────────────────────────────────

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).also { it.load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) {
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                .apply { init(spec) }
                .generateKey()
        }
        return ks.getKey(KEY_ALIAS, null) as SecretKey
    }

    // ── SecureStorage API ─────────────────────────────────────────────────────

    actual fun save(key: String, value: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv        = cipher.iv                                // 12 bytes for GCM
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload   = Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        prefs.edit().putString(key, payload).apply()
    }

    actual fun get(key: String): String? {
        val payload = prefs.getString(key, null) ?: return null
        return try {
            val combined  = Base64.decode(payload, Base64.NO_WRAP)
            val iv        = combined.copyOfRange(0, GCM_IV_LENGTH)
            val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
            val cipher    = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (_: Exception) {
            null   // corrupted or key rotated — treat as missing
        }
    }

    actual fun clear(key: String) {
        prefs.edit().remove(key).apply()
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS         = "fl_secure_key"
        private const val PREFS_FILE        = "fl_secure_prefs"
        private const val TRANSFORMATION    = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH     = 12
        private const val GCM_TAG_BITS      = 128
    }
}
