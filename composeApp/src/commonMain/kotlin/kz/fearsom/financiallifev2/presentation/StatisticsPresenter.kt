package kz.fearsom.financiallifev2.presentation

import io.github.aakira.napier.Napier
import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.network.GameApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "StatisticsPresenter"

data class StatisticsUiState(
    val isLoading: Boolean          = true,
    val stats: PlayerStatistics?    = null,
    val hasAnyGames: Boolean        = false
)

class StatisticsPresenter(
    private val sessionRepo: GameSessionRepository,
    private val scope: CoroutineScope,
    private val gameApiService: GameApiService? = null
) {
    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        // Re-compute whenever local sessions list changes (optimistic update)
        scope.launch {
            sessionRepo.sessions.collect { _ ->
                refreshLocal()
            }
        }
        // Fetch from server on startup (authoritative source)
        scope.launch {
            fetchFromServer()
        }
    }

    /**
     * Pull statistics from the server and update the UI.
     * Falls back to local data silently on any network failure.
     */
    suspend fun fetchFromServer() {
        val api = gameApiService ?: run {
            refreshLocal()
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        api.getPlayerStatistics()
            .onSuccess { dto ->
                Napier.d("Server stats loaded: total=${dto.totalGamesPlayed}", tag = TAG)
                val stats = dto.toPlayerStatistics()
                _uiState.update {
                    it.copy(
                        isLoading   = false,
                        stats       = if (stats.totalGamesPlayed > 0) stats else null,
                        hasAnyGames = stats.totalGamesPlayed > 0
                    )
                }
            }
            .onFailure { e ->
                Napier.w("Server stats unavailable (${e.message}), using local data", tag = TAG)
                refreshLocal()
            }
    }

    /** Recompute stats from in-memory sessions (fallback or offline mode). */
    fun refreshLocal() {
        val stats    = sessionRepo.getPlayerStatistics()
        val hasGames = stats.totalGamesPlayed > 0
        _uiState.update {
            it.copy(
                isLoading   = false,
                stats       = if (hasGames) stats else null,
                hasAnyGames = hasGames
            )
        }
    }

    /** Public refresh — fetches from server, falls back to local. */
    fun refresh() {
        scope.launch { fetchFromServer() }
    }
}

// ── DTO → Model mapping ───────────────────────────────────────────────────────

private fun kz.fearsom.financiallifev2.network.PlayerStatisticsResponse.toPlayerStatistics(): PlayerStatistics {
    val bestEndingEnum = bestEnding?.let {
        runCatching { GameEnding.valueOf(it) }.getOrNull()
    }
    val endingDist = GameEnding.entries.associateWith { ending ->
        endingDistribution[ending.name] ?: 0
    }
    return PlayerStatistics(
        totalGamesPlayed    = totalGamesPlayed,
        gamesCompleted      = gamesCompleted,
        bestEnding          = bestEndingEnum,
        averageCapitalAtEnd = averageCapitalAtEnd,
        mostPlayedEraId     = mostPlayedEraId,
        endingDistribution  = endingDist,
        perCharacter        = perCharacter.map { dto ->
            CharacterStatistics(
                characterId    = dto.characterId,
                characterName  = dto.characterName,
                characterEmoji = dto.characterEmoji,
                timesPlayed    = dto.timesPlayed,
                bestEnding     = dto.bestEnding?.let { runCatching { GameEnding.valueOf(it) }.getOrNull() },
                averageCapital = dto.averageCapital
            )
        },
        perEra = perEra.map { dto ->
            EraStatistics(
                eraId       = dto.eraId,
                eraName     = dto.eraName,
                timesPlayed = dto.timesPlayed,
                bestEnding  = dto.bestEnding?.let { runCatching { GameEnding.valueOf(it) }.getOrNull() }
            )
        }
    )
}
