package kz.fearsom.financiallifev2.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.achievements.AchievementCatalog
import kz.fearsom.financiallifev2.achievements.AchievementUnlockDto
import kz.fearsom.financiallifev2.data.AchievementsRepository

data class AchievementsUiState(
    /** achievementId → unlock record (definitions come from [AchievementCatalog]). */
    val unlocks: Map<String, AchievementUnlockDto> = emptyMap(),
    /** achievementId → "up" | "down". */
    val votes: Map<String, String> = emptyMap(),
    /** Currently open detail sheet, null = grid only. */
    val selectedId: String? = null
) {
    val unlockedCount: Int get() = unlocks.size
    val totalCount: Int get() = AchievementCatalog.all.size
    val progressPct: Int get() =
        if (totalCount == 0) 0 else unlockedCount * 100 / totalCount

    val gameUnlockedCount: Int get() =
        AchievementCatalog.gameAchievements.count { it.id in unlocks }
    val scamUnlockedCount: Int get() =
        AchievementCatalog.scamAchievements.count { it.id in unlocks }
}

/**
 * Pure Kotlin presenter (no Android ViewModel) — same pattern as the rest of
 * the app: Koin-provided deps + CoroutineScope + StateFlow.
 */
class AchievementsPresenter(
    private val repository: AchievementsRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            combine(repository.unlocks, repository.votes) { unlocks, votes ->
                unlocks to votes
            }.collect { (unlocks, votes) ->
                _uiState.update { it.copy(unlocks = unlocks, votes = votes) }
            }
        }
    }

    /** Pull/push server state; local data renders immediately either way. */
    fun refresh() {
        scope.launch { repository.sync() }
    }

    fun select(achievementId: String) {
        _uiState.update { it.copy(selectedId = achievementId) }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedId = null) }
    }

    /** Design semantics: tapping the active vote clears it. */
    fun vote(vote: String) {
        val id = _uiState.value.selectedId ?: return
        scope.launch { repository.toggleVote(id, vote) }
    }
}
