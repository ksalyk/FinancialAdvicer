package kz.fearsom.financiallifev2.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Local, [SecureStorage]-backed implementation of [FeatureFlagRepository].
 *
 * Values are persisted as strings using the flag's [FeatureFlag.key].
 * Unknown or missing values fall back to [FeatureFlag.default].
 *
 * Thread-safety: [SecureStorage] calls are synchronous; the [MutableStateFlow]
 * registry is written only from the main thread via [set], matching Compose's
 * single-threaded state model. If you ever call [set] from a background
 * coroutine, wrap it with `withContext(Dispatchers.Main)`.
 */
class LocalFeatureFlagRepository(
    private val storage: SecureStorage
) : FeatureFlagRepository {

    // Lazy registry: each flag gets its own StateFlow, created on first observe()
    private val flows = mutableMapOf<String, MutableStateFlow<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <T> get(flag: FeatureFlag<T>): T {
        val raw = storage.get(flag.key) ?: return flag.default
        return decode(flag, raw)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> set(flag: FeatureFlag<T>, value: T) {
        storage.save(flag.key, encode(flag, value))
        // Push to any active observer
        (flows[flag.key] as? MutableStateFlow<T>)?.value = value
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> observe(flag: FeatureFlag<T>): StateFlow<T> {
        return flows.getOrPut(flag.key) {
            MutableStateFlow(get(flag))
        } as StateFlow<T>
    }

    // ── Codec ─────────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun <T> encode(flag: FeatureFlag<T>, value: T): String = when (flag) {
        is FeatureFlag.TypingAnimationEnabled -> (value as Boolean).toString()
        is FeatureFlag.TypingAnimationPace    -> (value as TypingPace).name
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> decode(flag: FeatureFlag<T>, raw: String): T = when (flag) {
        is FeatureFlag.TypingAnimationEnabled -> (raw.toBooleanStrictOrNull() ?: flag.default) as T
        is FeatureFlag.TypingAnimationPace    -> TypingPace.fromStorageKey(raw) as T
    }
}
