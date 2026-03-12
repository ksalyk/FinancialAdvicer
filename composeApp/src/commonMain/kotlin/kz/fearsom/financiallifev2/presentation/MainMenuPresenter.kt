package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.model.GameSession
import kz.fearsom.financiallifev2.model.QuickStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainMenuUiState(
    val isLoading: Boolean        = true,
    val canContinue: Boolean      = false,
    val activeSession: GameSession? = null,
    val quickStats: QuickStats?   = null
)

class MainMenuPresenter(
    private val sessionRepo: GameSessionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(MainMenuUiState())
    val uiState: StateFlow<MainMenuUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            sessionRepo.sessions.collect { _ ->
                refresh()
            }
        }
    }

    fun refresh() {
        val active = sessionRepo.getActiveSession()
        val stats  = sessionRepo.getQuickStats()
        _uiState.update {
            it.copy(
                isLoading     = false,
                canContinue   = active != null,
                activeSession = active,
                quickStats    = stats
            )
        }
    }
}
