package kz.fearsom.financiallifev2.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kz.fearsom.financiallifev2.i18n.Strings

// ─── UI State ─────────────────────────────────────────────────────────────────

data class AsanMessage(
    val id: String,
    val text: String,
    val fromUser: Boolean,
    /** When true the UI renders the red-flag chips row under this message. */
    val showRedFlags: Boolean = false
)

data class AsanUiState(
    val messages: List<AsanMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0
)

/**
 * UI-stub presenter for the Asan AI advisor screen (redesign 2026-07).
 *
 * Deliberately local: canned replies, no backend, no real audio capture — the
 * voice composer is a visual state only. When the advisor service lands, swap
 * the reply logic for an API call and register this presenter in Koin like the
 * others (see di/AppModule.kt).
 */
class AsanPresenter(private val scope: CoroutineScope) {

    private val _uiState = MutableStateFlow(AsanUiState())
    val uiState: StateFlow<AsanUiState> = _uiState.asStateFlow()

    private var nextId = 0
    private var recordingJob: Job? = null

    private fun newId(): String = "asan-${nextId++}"

    /** Seeds the opening analysis message on first entry. */
    fun openConversation() {
        if (_uiState.value.messages.isNotEmpty()) return
        scope.launch {
            _uiState.update { it.copy(isTyping = true) }
            delay(700)
            _uiState.update {
                it.copy(
                    isTyping = false,
                    messages = it.messages + AsanMessage(
                        id = newId(),
                        text = Strings.uiAsanGreeting,
                        fromUser = false,
                        showRedFlags = true
                    )
                )
            }
        }
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _uiState.update {
            it.copy(messages = it.messages + AsanMessage(newId(), trimmed, fromUser = true))
        }
        scope.launch {
            _uiState.update { it.copy(isTyping = true) }
            delay(900)
            _uiState.update {
                it.copy(
                    isTyping = false,
                    messages = it.messages + AsanMessage(
                        id = newId(),
                        text = Strings.uiAsanReplyGeneric,
                        fromUser = false
                    )
                )
            }
        }
    }

    /**
     * Visual-only voice state: toggles the recording composer and runs the
     * timer. Stopping simply returns to idle — no audio is captured (stub).
     */
    fun toggleRecording() {
        val recording = !_uiState.value.isRecording
        recordingJob?.cancel()
        recordingJob = null
        _uiState.update { it.copy(isRecording = recording, recordingSeconds = 0) }
        if (recording) {
            recordingJob = scope.launch {
                while (isActive && _uiState.value.isRecording) {
                    delay(1000)
                    _uiState.update { it.copy(recordingSeconds = it.recordingSeconds + 1) }
                }
            }
        }
    }

    fun cancelRecording() {
        recordingJob?.cancel()
        recordingJob = null
        _uiState.update { it.copy(isRecording = false, recordingSeconds = 0) }
    }
}
