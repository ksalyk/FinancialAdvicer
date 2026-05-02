package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.LocaleRepository
import kz.fearsom.financiallifev2.i18n.Strings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LocaleUiState(
    val currentLocale: String
)

class LocalePresenter(
    private val localeRepository: LocaleRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(
        LocaleUiState(currentLocale = Strings.currentLocale)
    )
    val uiState: StateFlow<LocaleUiState> = _uiState.asStateFlow()

    fun selectLocale(locale: String) {
        scope.launch {
            _uiState.value = LocaleUiState(
                currentLocale = localeRepository.setLocale(locale)
            )
        }
    }
}
