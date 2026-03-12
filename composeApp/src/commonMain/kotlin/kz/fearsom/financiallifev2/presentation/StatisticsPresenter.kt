package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.model.PlayerStatistics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatisticsUiState(
    val isLoading: Boolean          = true,
    val stats: PlayerStatistics?    = null,
    val hasAnyGames: Boolean        = false
)

class StatisticsPresenter(
    private val sessionRepo: GameSessionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            sessionRepo.sessions.collect { _ ->
                refresh()
            }
        }
    }

    fun refresh() {
        val stats      = sessionRepo.getPlayerStatistics()
        val hasGames   = stats.totalGamesPlayed > 0
        _uiState.update {
            it.copy(
                isLoading   = false,
                stats       = if (hasGames) stats else null,
                hasAnyGames = hasGames
            )
        }
    }
}
