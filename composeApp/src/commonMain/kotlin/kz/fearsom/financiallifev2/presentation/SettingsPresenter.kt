package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.FeatureFlag
import kz.fearsom.financiallifev2.data.FeatureFlagRepository
import kz.fearsom.financiallifev2.data.LocaleRepository
import kz.fearsom.financiallifev2.data.TypingPace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val currentLocale: String        = LocaleRepository.DEFAULT_LOCALE,
    val typingAnimationEnabled: Boolean = FeatureFlag.TypingAnimationEnabled.default,
    val typingAnimationPace: TypingPace = FeatureFlag.TypingAnimationPace.default
)

/**
 * Presenter for the Settings screen.
 *
 * Owns both locale and feature-flag state so SettingsScreen only needs one
 * state object. Previously locale was driven directly from LocalePresenter —
 * migrating here consolidates the settings surface.
 */
class SettingsPresenter(
    private val localeRepo: LocaleRepository,
    private val featureFlags: FeatureFlagRepository,
    @Suppress("UNUSED_PARAMETER") scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            currentLocale           = localeRepo.restoreLocale(),
            typingAnimationEnabled  = featureFlags.get(FeatureFlag.TypingAnimationEnabled),
            typingAnimationPace     = featureFlags.get(FeatureFlag.TypingAnimationPace)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    // ── Locale ────────────────────────────────────────────────────────────────

    fun selectLocale(locale: String) {
        val normalized = localeRepo.setLocale(locale)
        _uiState.value = _uiState.value.copy(currentLocale = normalized)
    }

    // ── Feature flags ─────────────────────────────────────────────────────────

    fun setTypingAnimationEnabled(enabled: Boolean) {
        featureFlags.set(FeatureFlag.TypingAnimationEnabled, enabled)
        _uiState.value = _uiState.value.copy(typingAnimationEnabled = enabled)
    }

    fun setTypingAnimationPace(pace: TypingPace) {
        featureFlags.set(FeatureFlag.TypingAnimationPace, pace)
        _uiState.value = _uiState.value.copy(typingAnimationPace = pace)
    }
}
