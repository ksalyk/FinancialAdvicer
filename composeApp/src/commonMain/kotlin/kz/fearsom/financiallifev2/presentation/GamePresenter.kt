package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameOption
import kz.fearsom.financiallifev2.model.GameState
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
    val showStats: Boolean = false
)

/**
 * Pure Kotlin presenter — iOS + Android compatible (no Android ViewModel).
 *
 * Wires [GameEngine.state] StateFlow into [GameUiState], injecting a simulated
 * typing delay so choices feel like a real async conversation.
 */
class GamePresenter(
    private val engine: GameEngine,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            // Mirror engine's canonical state into our UI state.
            // currentOptions are gated by isWaitingForChoice so we never show
            // options mid-typing or on game-over screens.
            engine.state.collect { gameState ->
                _uiState.value = _uiState.value.copy(
                    gameState      = gameState,
                    currentOptions = if (gameState?.isWaitingForChoice == true)
                        engine.currentOptions()
                    else
                        emptyList()
                )
            }
        }
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    fun startGame() {
        scope.launch {
            _uiState.value = _uiState.value.copy(isTyping = true)
            delay(800)                // brief pause before first message
            engine.startGame()        // emits initial GameState → collector updates UI
            delay(300)
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }

    /**
     * Handle a player option tap.
     *
     * Flow:
     *  1. Hide options, show typing indicator
     *  2. Wait [typingDelay] so the player sees "Асан is typing…"
     *  3. Call engine.makeChoice — synchronously computes new state (applies
     *     effects, runs monthly tick if needed, injects conditional events),
     *     emits it on [engine.state], collector updates gameState + options
     *  4. Short settling pause, then hide typing indicator
     */
    fun onChoiceSelected(optionId: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isTyping = true, currentOptions = emptyList())
            delay(1400)               // simulate response latency
            engine.makeChoice(optionId)
            delay(500)
            _uiState.value = _uiState.value.copy(isTyping = false)
        }
    }

    fun toggleStats() {
        _uiState.value = _uiState.value.copy(showStats = !_uiState.value.showStats)
    }

    fun restartGame() {
        engine.reset()
        _uiState.value = GameUiState()
        startGame()
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
