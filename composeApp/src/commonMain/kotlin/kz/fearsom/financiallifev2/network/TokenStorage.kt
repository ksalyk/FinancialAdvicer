package kz.fearsom.financiallifev2.network

/**
 * In-memory token store shared between [buildHttpClient] and [AuthRepository].
 *
 * Both components receive the same singleton instance via Koin, so any token
 * update from AuthRepository is immediately visible to the Ktor Auth plugin.
 *
 * Persistence note: tokens live only for the process lifetime.
 * To survive cold starts, persist via EncryptedSharedPreferences (Android)
 * or Keychain (iOS) using an expect/actual wrapper, then call
 * [AuthRepository.restoreSession] on app start to reload them.
 */
class TokenStorage {
    var accessToken: String  = ""
    var refreshToken: String = ""

    fun isAccessTokenPresent(): Boolean = accessToken.isNotBlank()

    fun update(accessToken: String, refreshToken: String) {
        this.accessToken  = accessToken
        this.refreshToken = refreshToken
    }

    fun clear() {
        accessToken  = ""
        refreshToken = ""
    }
}
