package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.model.PredefinedCharacter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharactersUiState(
    val characters: List<PredefinedCharacter> = emptyList(),
    val selectedCharacterId: String?          = null
)

class CharactersPresenter(
    private val sessionRepo: GameSessionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState: StateFlow<CharactersUiState> = _uiState.asStateFlow()

    init {
        refreshLocalizedData()
    }

    fun refreshLocalizedData() {
        _uiState.update { it.copy(characters = SeedData.predefinedCharacters) }
    }

    fun selectCharacter(characterId: String?) {
        _uiState.update { it.copy(selectedCharacterId = characterId) }
    }
}
