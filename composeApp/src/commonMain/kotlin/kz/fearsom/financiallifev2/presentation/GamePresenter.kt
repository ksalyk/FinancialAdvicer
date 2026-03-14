package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val gameState: GameState? = null,
    val currentOptions: List<GameOption> = emptyList(),
    val isTyping: Boolean = false,
    val showStats: Boolean = false,
    // Dynamic character display info
    val characterName: String  = "Асан",
    val characterEmoji: String = "👨‍💻",
    val characterTitle: String = "Инженер-программист"
)

/**
 * Pure Kotlin presenter — iOS + Android compatible (no Android ViewModel).
 *
 * Session-aware: supports both the legacy default start (Асан) and the
 * new session-based flow (character-selection → ChatScreen).
 */
class GamePresenter(
    private val engine: GameEngine,
    private val sessionRepo: GameSessionRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    // Tracks which session is currently being played (for save-on-back)
    private var activeSessionId: String? = null

    init {
        scope.launch {
            engine.state.collect { gameState ->
                _uiState.value = _uiState.value.copy(
                    gameState      = gameState,
                    currentOptions = if (gameState?.isWaitingForChoice == true)
                        engine.currentOptions()
                    else
                        emptyList()
                )
                // Auto-save progress to repository whenever game state changes
                gameState?.let { state ->
                    activeSessionId?.let { id ->
                        sessionRepo.saveGameState(id, state)
                        // Complete the session if it reached a game-over ending
                        val endingType = state.endingType
                        if (state.gameOver && endingType != null) {
                            sessionRepo.completeSession(id, endingType.toGameEnding())
                        }
                    }
                }
            }
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Start a brand-new game for [session] created by the new-game flow.
     * Initialises the engine with the character's stats from the era's start year.
     */
    fun startNewGame(sessionId: String) {
        val session = sessionRepo.getSession(sessionId) ?: run {
            startDefaultGame(); return
        }
        activeSessionId = sessionId
        // Set character info synchronously so the first render already shows the correct character
        _uiState.value = _uiState.value.copy(
            characterName  = session.characterName,
            characterEmoji = session.characterEmoji,
            characterTitle = session.characterTitle
        )
        scope.launch {
            _uiState.value = _uiState.value.copy(isTyping = true)
            delay(800)
            val initialState = session.initialStats.toPlayerState(
                year  = session.currentGameYear,
                month = session.currentGameMonth
            )
            engine.startGame(initialState, session.characterName)
            delay(300)
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }

    /**
     * Continue an existing [session] — restores the saved engine state if available,
     * otherwise restarts from the session's initial stats.
     */
    fun continueGame(sessionId: String) {
        val session = sessionRepo.getSession(sessionId) ?: run {
            startDefaultGame(); return
        }
        activeSessionId = sessionId
        val savedState = sessionRepo.getSavedGameState(sessionId)
        // Set character info synchronously so the first render already shows the correct character
        _uiState.value = _uiState.value.copy(
            characterName  = session.characterName,
            characterEmoji = session.characterEmoji,
            characterTitle = session.characterTitle
        )
        scope.launch {
            _uiState.value = _uiState.value.copy(isTyping = true)
            delay(800)
            if (savedState != null) {
                engine.loadState(savedState, session.characterName)
            } else {
                val initialState = session.initialStats.toPlayerState(
                    year  = session.currentGameYear,
                    month = session.currentGameMonth
                )
                engine.startGame(initialState, session.characterName)
            }
            delay(300)
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }

    /** Legacy default start (Асан the junior dev) — used when no session is selected. */
    fun startDefaultGame() {
        activeSessionId = null
        scope.launch {
            _uiState.value = _uiState.value.copy(
                isTyping       = true,
                characterName  = "Асан",
                characterEmoji = "👨‍💻",
                characterTitle = "Инженер-программист"
            )
            delay(800)
            engine.startGame()
            delay(300)
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }

    fun onChoiceSelected(optionId: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isTyping = true, currentOptions = emptyList())
            delay(1400)
            engine.makeChoice(optionId)
            delay(500)
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }

    fun toggleStats() {
        _uiState.value = _uiState.value.copy(showStats = !_uiState.value.showStats)
    }

    /** Restart from the beginning of the same session (same character + era). */
    fun restartGame() {
        engine.reset()
        _uiState.value = _uiState.value.copy(
            gameState      = null,
            currentOptions = emptyList(),
            isTyping       = false
        )
        val sessionId = activeSessionId
        if (sessionId != null) {
            startNewGame(sessionId)
        } else {
            startDefaultGame()
        }
    }

    /** Save the current engine state before navigating away (back to MainMenu). */
    fun saveAndPause() {
        val sessionId = activeSessionId ?: return
        val state = _uiState.value.gameState ?: return
        sessionRepo.saveGameState(sessionId, state)
    }

    fun getEndingTitle(type: EndingType?): String = when (type) {
        EndingType.BANKRUPTCY            -> "💔 Банкротство"
        EndingType.PAYCHECK_TO_PAYCHECK  -> "😰 От зарплаты до зарплаты"
        EndingType.FINANCIAL_STABILITY   -> "😊 Финансовая стабильность"
        EndingType.FINANCIAL_FREEDOM     -> "🎯 Финансовая свобода!"
        EndingType.WEALTH                -> "🤑 Богатство"
        null                             -> "🏁 Игра окончена"
    }
}
