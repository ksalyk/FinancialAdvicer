package kz.fearsom.financiallifev2.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Contract for reading and writing feature flags.
 *
 * ## Design intent
 * - [get] / [set] are synchronous — safe to call during composition setup
 *   without a coroutine context.
 * - [observe] returns a [StateFlow] so composables and presenters can react
 *   to runtime changes without polling.
 *
 * ## Swap strategy for remote flags
 * Provide a `RemoteFeatureFlagRepository` that:
 *   1. Delegates [get] / [observe] to the local repo as the fallback.
 *   2. Fetches a server config on launch and emits updated values via the
 *      same [StateFlow]s, which all observers receive automatically.
 *   3. Caches the last server response locally so the app works offline.
 *
 * Bind the remote impl in the Koin module to switch transparently.
 */
interface FeatureFlagRepository {

    /** Read the current value of [flag] synchronously. */
    fun <T> get(flag: FeatureFlag<T>): T

    /** Persist a new [value] for [flag]. */
    fun <T> set(flag: FeatureFlag<T>, value: T)

    /**
     * Observe [flag] as a hot [StateFlow].
     * Emits the current value immediately and again on every [set] call.
     */
    fun <T> observe(flag: FeatureFlag<T>): StateFlow<T>
}
