package kz.fearsom.financiallifev2.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.data.CatalogRepository
import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.model.CharacterBundle
import kz.fearsom.financiallifev2.model.CharacterType
import kz.fearsom.financiallifev2.model.Era
import kz.fearsom.financiallifev2.model.GameSession
import kz.fearsom.financiallifev2.model.PredefinedCharacter

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
 * Eras/characters/bundles come from [CatalogRepository], which overlays the
 * admin-managed catalog (active rows + metadata) onto in-code SeedData. So an
 * admin deactivating/renaming/re-assigning a character or era is reflected here.
 *
 * [startWithPredefined] / [startWithBundle] return the new [GameSession.id] so the
 * Composable can trigger navigation to ChatScreen.
 */
class NewGamePresenter(
    private val sessionRepo: GameSessionRepository,
    private val catalogRepo: CatalogRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(NewGameUiState())
    val uiState: StateFlow<NewGameUiState> = _uiState.asStateFlow()

    init {
        refreshLocalizedData()
        scope.launch { catalogRepo.refresh() }
        scope.launch { catalogRepo.catalog.collect { refreshLocalizedData() } }
    }

    /**
     * Re-pulls the catalog so admin changes appear without an app relaunch.
     * Call when entering the New Game flow. The catalog collector recomputes UI
     * state if anything changed; an unchanged catalog is a no-op (StateFlow dedups).
     */
    fun refresh() {
        scope.launch { catalogRepo.refresh() }
    }

    fun refreshLocalizedData() {
        val selectedEraId = _uiState.value.selectedEraId
        _uiState.update {
            it.copy(
                eras = catalogRepo.eras(),
                availableCharacters = selectedEraId
                    ?.let { eraId -> catalogRepo.predefinedCharacters().filter { c -> eraId in c.compatibleEraIds } }
                    ?: it.availableCharacters,
                availableBundles = selectedEraId
                    ?.let { eraId -> catalogRepo.characterBundles().filter { b -> eraId in b.compatibleEraIds } }
                    ?: it.availableBundles
            )
        }
    }

    /** Called from EraSelectionScreen. Loads characters/bundles for that era. */
    fun selectEra(eraId: String) {
        val characters = catalogRepo.predefinedCharacters().filter { eraId in it.compatibleEraIds }
        val bundles    = catalogRepo.characterBundles().filter { eraId in it.compatibleEraIds }
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
        val era       = catalogRepo.eras().find { it.id == eraId } ?: return null
        val character = catalogRepo.predefinedCharacters().find { it.id == characterId } ?: return null

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
        val era    = catalogRepo.eras().find { it.id == eraId } ?: return null
        val bundle = catalogRepo.characterBundles().find { it.id == bundleId } ?: return null

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
        val character = catalogRepo.predefinedCharacters().find { it.id == characterId } ?: return null
        val eraId     = character.compatibleEraIds.firstOrNull() ?: return null
        selectEra(eraId)
        return startWithPredefined(characterId)
    }
}
