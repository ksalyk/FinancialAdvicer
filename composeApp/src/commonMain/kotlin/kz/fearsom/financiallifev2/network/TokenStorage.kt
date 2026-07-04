package kz.fearsom.financiallifev2.network

import kotlin.concurrent.Volatile

/**
 * In-memory token store shared between [buildHttpClient] and [AuthRepository].
 *
 * Both components receive the same singleton instance via Koin, so any token
 * update from AuthRepository is immediately visible to the Ktor Auth plugin.
 *
 * Thread-safety: written from Ktor's refresh coroutine (IO dispatcher) and read
 * from UI/main. The pair is held in a single @Volatile immutable snapshot so a
 * reader can never observe a fresh accessToken paired with a stale refreshToken
 * (torn read across two separately-published fields).
 *
 * Persistence note: tokens live only for the process lifetime.
 * To survive cold starts, persist via EncryptedSharedPreferences (Android)
 * or Keychain (iOS) using an expect/actual wrapper, then call
 * [AuthRepository.restoreSession] on app start to reload them.
 */
class TokenStorage {

    private data class Tokens(val access: String, val refresh: String)

    @Volatile
    private var tokens = Tokens("", "")

    val accessToken: String  get() = tokens.access
    val refreshToken: String get() = tokens.refresh

    fun isAccessTokenPresent(): Boolean = tokens.access.isNotBlank()

    fun update(accessToken: String, refreshToken: String) {
        tokens = Tokens(accessToken, refreshToken)
    }

    fun clear() {
        tokens = Tokens("", "")
    }
}
