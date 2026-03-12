package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.data.SeedData
import kz.fearsom.financiallifev2.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NewGameUiState(
    val eras: List<Era>                            = emptyList(),
    val selectedEraId: String?                     = null,
    val availableCharacters: List<PredefinedCharacter> = emptyList(),
    val availableBundles: List<CharacterBundle>    = emptyList(),
    val isCreatingSession: Boolean                 = false
)

/**
 * Manages the two-step "New Game" flow:
 *  Step 1 — EraSelectionScreen: user picks an era
 *  Step 2 — CharacterSelectionScreen: user picks a predefined character or bundle
 *
 * [startWithPredefined] / [startWithBundle] return the new [GameSession.id] so the
 * Composable can trigger navigation to ChatScreen.
 */
class NewGamePresenter(
    private val sessionRepo: GameSessionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(NewGameUiState())
    val uiState: StateFlow<NewGameUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(eras = SeedData.eras) }
    }

    /** Called from EraSelectionScreen. Loads characters/bundles for that era. */
    fun selectEra(eraId: String) {
        val characters = SeedData.predefinedCharacters.filter { eraId in it.compatibleEraIds }
        val bundles    = SeedData.characterBundles.filter { eraId in it.compatibleEraIds }
        _uiState.update {
            it.copy(
                selectedEraId       = eraId,
                availableCharacters = characters,
                availableBundles    = bundles
            )
        }
    }

    /** Starts a new game from CharacterSelectionScreen with a predefined character. */
    fun startWithPredefined(characterId: String): String? {
        val eraId     = _uiState.value.selectedEraId ?: return null
        val era       = SeedData.eras.find { it.id == eraId } ?: return null
        val character = SeedData.predefinedCharacters.find { it.id == characterId } ?: return null

        val session = sessionRepo.createSession(
            era            = era,
            characterType  = CharacterType.PREDEFINED,
            characterId    = character.id,
            characterName  = character.name,
            characterEmoji = character.emoji,
            characterTitle = character.profession,
            initialStats   = character.initialStats
        )
        return session.id
    }

    /** Starts a new game from CharacterSelectionScreen with a bundle. */
    fun startWithBundle(bundleId: String): String? {
        val eraId  = _uiState.value.selectedEraId ?: return null
        val era    = SeedData.eras.find { it.id == eraId } ?: return null
        val bundle = SeedData.characterBundles.find { it.id == bundleId } ?: return null

        val session = sessionRepo.createSession(
            era            = era,
            characterType  = CharacterType.CUSTOM_BUNDLE,
            characterId    = bundle.id,
            characterName  = bundle.label,
            characterEmoji = bundle.emoji,
            characterTitle = bundle.profession,
            initialStats   = bundle.stats
        )
        return session.id
    }

    /** Quick-start from CharactersScreen — skips era selection, uses first compatible era. */
    fun quickStartWithCharacter(characterId: String): String? {
        val character = SeedData.predefinedCharacters.find { it.id == characterId } ?: return null
        val eraId     = character.compatibleEraIds.firstOrNull() ?: return null
        selectEra(eraId)
        return startWithPredefined(characterId)
    }
}
