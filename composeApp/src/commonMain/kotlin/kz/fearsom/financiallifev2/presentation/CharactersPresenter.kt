package kz.fearsom.financiallifev2.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.data.CatalogRepository
import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.model.PredefinedCharacter

data class CharactersUiState(
    val characters: List<PredefinedCharacter> = emptyList(),
    val selectedCharacterId: String?          = null
)

class CharactersPresenter(
    private val sessionRepo: GameSessionRepository,
    private val catalogRepo: CatalogRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState: StateFlow<CharactersUiState> = _uiState.asStateFlow()

    init {
        refreshLocalizedData()                                  // immediate SeedData fallback
        scope.launch { catalogRepo.refresh() }                 // pull admin catalog
        scope.launch { catalogRepo.catalog.collect { refreshLocalizedData() } } // re-render on arrival
    }

    /** Recomputes the list from the catalog overlay (also called on locale change). */
    fun refreshLocalizedData() {
        _uiState.update { it.copy(characters = catalogRepo.predefinedCharacters()) }
    }

    /**
     * Re-pulls the catalog so admin changes appear without an app relaunch.
     * Call when entering the Characters screen. Unchanged catalog = no-op.
     */
    fun refresh() {
        scope.launch { catalogRepo.refresh() }
    }

    fun selectCharacter(characterId: String?) {
        _uiState.update { it.copy(selectedCharacterId = characterId) }
    }
}
