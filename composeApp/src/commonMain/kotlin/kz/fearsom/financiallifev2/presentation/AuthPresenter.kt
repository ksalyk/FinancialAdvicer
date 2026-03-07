package kz.fearsom.financiallifev2.presentation

import kz.fearsom.financiallifev2.auth.AuthRepository
import kz.fearsom.financiallifev2.auth.AuthState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val authState: AuthState = AuthState(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegisterMode: Boolean = false
)

/**
 * Pure Kotlin presenter — no Android ViewModel, works on iOS + Android via CMP.
 * Lifecycle is managed by the Composable that creates it via [rememberAuthPresenter].
 */
class AuthPresenter(
    private val authRepository: AuthRepository,
    private val scope: CoroutineScope
) {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            authRepository.authState.collect { authState ->
                _uiState.value = _uiState.value.copy(authState = authState)
            }
        }
    }

    fun login(username: String, password: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.login(username, password).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    fun register(username: String, password: String) {
        scope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            authRepository.register(username, password).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(isLoading = false) },
                onFailure = { e -> _uiState.value = _uiState.value.copy(isLoading = false, error = e.message) }
            )
        }
    }

    /** Called once on startup — reads tokens from secure storage and validates with /auth/me. */
    fun restoreSession() {
        scope.launch { authRepository.restoreSessionFromStorage() }
    }

    fun logout() = authRepository.logout()

    fun toggleMode() {
        _uiState.value = _uiState.value.copy(
            isRegisterMode = !_uiState.value.isRegisterMode,
            error = null
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
