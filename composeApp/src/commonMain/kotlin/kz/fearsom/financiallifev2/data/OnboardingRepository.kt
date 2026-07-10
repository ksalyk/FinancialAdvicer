package kz.fearsom.financiallifev2.data

/**
 * One-shot "has the intro onboarding been shown" flag.
 *
 * Deliberately not a [FeatureFlag]: feature flags describe togglable behaviour
 * with a remote-config future, while this is app-lifecycle state the user
 * completes exactly once. Kept as its own tiny repository so the storage key
 * and versioning live in one place.
 *
 * Bump [KEY] (e.g. `…_v2`) to re-show onboarding after a major content change.
 */
class OnboardingRepository(
    private val secureStorage: SecureStorage
) {
    /** True once the user has finished (or exited) the intro flow. */
    fun isCompleted(): Boolean = secureStorage.get(KEY) == "true"

    fun markCompleted() = secureStorage.save(KEY, "true")

    private companion object {
        const val KEY = "onboarding_completed_v1"
    }
}
