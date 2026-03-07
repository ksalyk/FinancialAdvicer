package kz.fearsom.financiallifev2.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.fearsom.financiallifev2.auth.AuthRepository
import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.presentation.AuthPresenter
import kz.fearsom.financiallifev2.presentation.GamePresenter
import kz.fearsom.financiallifev2.ui.screens.ChatScreen
import kz.fearsom.financiallifev2.ui.screens.LoginScreen
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.koinInject

/**
 * Simple state-machine navigation — no external nav library needed.
 * AnimatedContent handles screen transitions with slide animations.
 */
sealed interface AppScreen {
    object Login : AppScreen
    object Game  : AppScreen
}

@Composable
fun AppNavigation() {
    val authRepository: AuthRepository = koinInject()
    val gameEngine: GameEngine         = koinInject()

    val scope = rememberCoroutineScope()

    val authPresenter  = remember { AuthPresenter(authRepository, scope) }
    val gamePresenter  = remember { GamePresenter(gameEngine, scope) }

    val authUiState by authPresenter.uiState.collectAsStateWithLifecycle()
    val gameUiState by gamePresenter.uiState.collectAsStateWithLifecycle()

    // On cold start: try to restore a previously saved session from secure storage.
    // If tokens are present and still valid, the user skips the login screen entirely.
    LaunchedEffect(Unit) {
        authPresenter.restoreSession()
    }

    // Derive active screen from auth state
    val currentScreen: AppScreen by remember {
        derivedStateOf {
            if (authUiState.authState.isLoggedIn) AppScreen.Game else AppScreen.Login
        }
    }

    // Start game when navigating to Game screen
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.Game && gameUiState.gameState == null) {
            gamePresenter.startGame()
        }
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == AppScreen.Game) {
                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(380)) +
                        fadeIn(tween(380)) togetherWith
                        slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(380)) +
                        fadeOut(tween(200))
            } else {
                slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(380)) +
                        fadeIn(tween(380)) togetherWith
                        slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(380)) +
                        fadeOut(tween(200))
            }
        },
        label = "screenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.Login -> LoginScreen(
                isLoading      = authUiState.isLoading,
                error          = authUiState.error,
                isRegisterMode = authUiState.isRegisterMode,
                onLogin        = authPresenter::login,
                onRegister     = authPresenter::register,
                onToggleMode   = authPresenter::toggleMode
            )

            AppScreen.Game -> ChatScreen(
                uiState          = gameUiState,
                onChoiceSelected = gamePresenter::onChoiceSelected,
                onToggleStats    = gamePresenter::toggleStats,
                onRestart        = gamePresenter::restartGame
            )
        }
    }
}
