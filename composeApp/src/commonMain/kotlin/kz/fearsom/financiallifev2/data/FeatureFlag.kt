package kz.fearsom.financiallifev2.data

/**
 * Type-safe feature flag descriptor.
 *
 * Each flag carries its storage key and a compile-time default value.
 * The sealed class hierarchy enforces that every caller uses the same
 * key string and default — no stringly-typed look-ups at the call site.
 *
 * Adding a new flag: add a `data object` here. No other plumbing needed.
 *
 * Future: a RemoteFeatureFlagRepository can overlay server-fetched values
 * on top of the local defaults by delegating to LocalFeatureFlagRepository
 * for unknown keys or when the network is unavailable.
 */
sealed class FeatureFlag<T>(
    val key: String,
    val default: T
) {
    /** Animate character messages with a typing reveal effect. */
    data object TypingAnimationEnabled : FeatureFlag<Boolean>(
        key     = "ff_typing_animation_enabled",
        default = true
    )

    /** Speed at which characters are revealed during typing animation. */
    data object TypingAnimationPace : FeatureFlag<TypingPace>(
        key     = "ff_typing_animation_pace",
        default = TypingPace.NORMAL
    )
}

/**
 * Controls how fast each character appears during the typing animation.
 *
 * [charDelayMs] — milliseconds between each character reveal.
 * Tune these values to feel natural on the target device class.
 */
enum class TypingPace(val charDelayMs: Long) {
    SLOW(55L),
    NORMAL(28L),
    FAST(10L);

    companion object {
        fun fromStorageKey(key: String): TypingPace =
            entries.firstOrNull { it.name == key } ?: NORMAL
    }
}
