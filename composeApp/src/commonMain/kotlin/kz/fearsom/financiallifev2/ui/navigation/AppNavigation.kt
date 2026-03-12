package kz.fearsom.financiallifev2.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kz.fearsom.financiallifev2.auth.AuthRepository
import kz.fearsom.financiallifev2.data.GameSessionRepository
import kz.fearsom.financiallifev2.engine.GameEngine
import kz.fearsom.financiallifev2.presentation.*
import kz.fearsom.financiallifev2.ui.screens.*
import org.koin.compose.koinInject

// ── Screen definitions ────────────────────────────────────────────────────────

sealed interface AppScreen {
    object Login                                         : AppScreen
    object MainMenu                                      : AppScreen
    object EraSelection                                  : AppScreen
    data class CharacterSelection(val eraId: String)     : AppScreen
    object Characters                                    : AppScreen
    data class CharacterDetail(val characterId: String)  : AppScreen
    object Statistics                                    : AppScreen
    data class Game(val sessionId: String)               : AppScreen
}

// Navigation depth — drives slide direction.
private fun AppScreen.depth(): Int = when (this) {
    AppScreen.Login                  -> 0
    AppScreen.MainMenu               -> 1
    AppScreen.EraSelection           -> 2
    is AppScreen.CharacterSelection  -> 3
    AppScreen.Characters             -> 2
    is AppScreen.CharacterDetail     -> 3
    AppScreen.Statistics             -> 2
    is AppScreen.Game                -> 4
}

// ── Navigation host ───────────────────────────────────────────────────────────

@Composable
fun AppNavigation() {
    val authRepository : AuthRepository         = koinInject()
    val gameEngine     : GameEngine             = koinInject()
    val sessionRepo    : GameSessionRepository  = koinInject()

    val scope = rememberCoroutineScope()

    // Shared presenters — lifetime bound to this Composable's composition
    val authPresenter     = remember { AuthPresenter(authRepository, scope) }
    val gamePresenter     = remember { GamePresenter(gameEngine, sessionRepo, scope) }
    val mainMenuPresenter = remember { MainMenuPresenter(sessionRepo, scope) }
    val newGamePresenter  = remember { NewGamePresenter(sessionRepo, scope) }
    val charsPresenter    = remember { CharactersPresenter(sessionRepo, scope) }
    val statsPresenter    = remember { StatisticsPresenter(sessionRepo, scope) }

    val authUiState     by authPresenter.uiState.collectAsStateWithLifecycle()
    val gameUiState     by gamePresenter.uiState.collectAsStateWithLifecycle()
    val mainMenuUiState by mainMenuPresenter.uiState.collectAsStateWithLifecycle()
    val newGameUiState  by newGamePresenter.uiState.collectAsStateWithLifecycle()
    val charsUiState    by charsPresenter.uiState.collectAsStateWithLifecycle()
    val statsUiState    by statsPresenter.uiState.collectAsStateWithLifecycle()

    // ── Back stack ────────────────────────────────────────────────────────────
    var backStack  by remember { mutableStateOf(listOf<AppScreen>(AppScreen.Login)) }
    var navForward by remember { mutableStateOf(true) }

    fun navigate(screen: AppScreen) {
        navForward = screen.depth() >= (backStack.lastOrNull()?.depth() ?: 0)
        backStack  = backStack + screen
    }

    fun goBack() {
        if (backStack.size > 1) {
            navForward = false
            backStack  = backStack.dropLast(1)
        }
    }

    // ── Cold start ────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) { authPresenter.restoreSession() }

    // ── Auth state ────────────────────────────────────────────────────────────
    LaunchedEffect(authUiState.authState.isLoggedIn) {
        if (authUiState.authState.isLoggedIn) {
            if (backStack.lastOrNull() == AppScreen.Login ||
                backStack.lastOrNull() is AppScreen.Login) {
                navForward = true
                backStack  = listOf(AppScreen.MainMenu)
            }
        } else {
            navForward = false
            backStack  = listOf(AppScreen.Login)
        }
    }

    val currentScreen = backStack.lastOrNull() ?: AppScreen.Login

    AnimatedContent(
        targetState  = currentScreen,
        transitionSpec = {
            val dir = if (navForward) 1 else -1
            slideInHorizontally(initialOffsetX = { it * dir }, animationSpec = tween(320)) +
                    fadeIn(tween(320)) togetherWith
                    slideOutHorizontally(targetOffsetX = { -it * dir }, animationSpec = tween(320)) +
                    fadeOut(tween(200))
        },
        label = "appNav"
    ) { screen ->
        when (screen) {

            // ── Login ─────────────────────────────────────────────────────────
            AppScreen.Login -> LoginScreen(
                isLoading      = authUiState.isLoading,
                error          = authUiState.error,
                isRegisterMode = authUiState.isRegisterMode,
                onLogin        = authPresenter::login,
                onRegister     = authPresenter::register,
                onToggleMode   = authPresenter::toggleMode
            )

            // ── Main Menu ─────────────────────────────────────────────────────
            AppScreen.MainMenu -> MainMenuScreen(
                uiState      = mainMenuUiState,
                onContinue   = {
                    val activeId = mainMenuUiState.activeSession?.id
                    if (activeId != null) {
                        gamePresenter.continueGame(activeId)
                        navigate(AppScreen.Game(activeId))
                    }
                },
                onNewGame    = { navigate(AppScreen.EraSelection) },
                onCharacters = { navigate(AppScreen.Characters) },
                onStatistics = { navigate(AppScreen.Statistics) },
                onLogout     = {
                    gamePresenter.saveAndPause()
                    authPresenter.logout()
                }
            )

            // ── Era Selection ─────────────────────────────────────────────────
            AppScreen.EraSelection -> EraSelectionScreen(
                uiState       = newGameUiState,
                onEraSelected = { eraId ->
                    newGamePresenter.selectEra(eraId)
                    navigate(AppScreen.CharacterSelection(eraId))
                },
                onBack = ::goBack
            )

            // ── Character Selection ───────────────────────────────────────────
            is AppScreen.CharacterSelection -> CharacterSelectionScreen(
                uiState            = newGameUiState,
                onSelectPredefined = { charId ->
                    val sessionId = newGamePresenter.startWithPredefined(charId)
                    if (sessionId != null) {
                        gamePresenter.startNewGame(sessionId)
                        navForward = true
                        backStack  = listOf(AppScreen.MainMenu, AppScreen.Game(sessionId))
                    }
                },
                onSelectBundle = { bundleId ->
                    val sessionId = newGamePresenter.startWithBundle(bundleId)
                    if (sessionId != null) {
                        gamePresenter.startNewGame(sessionId)
                        navForward = true
                        backStack  = listOf(AppScreen.MainMenu, AppScreen.Game(sessionId))
                    }
                },
                onBack = ::goBack
            )

            // ── Characters ────────────────────────────────────────────────────
            AppScreen.Characters -> CharactersScreen(
                uiState          = charsUiState,
                onCharacterClick = { charId -> navigate(AppScreen.CharacterDetail(charId)) },
                onBack           = ::goBack
            )

            // ── Character Detail ──────────────────────────────────────────────
            is AppScreen.CharacterDetail -> CharacterDetailScreen(
                characterId = screen.characterId,
                onBack      = ::goBack,
                onStartGame = { charId ->
                    val sessionId = newGamePresenter.quickStartWithCharacter(charId)
                    if (sessionId != null) {
                        gamePresenter.startNewGame(sessionId)
                        navForward = true
                        backStack  = listOf(AppScreen.MainMenu, AppScreen.Game(sessionId))
                    }
                }
            )

            // ── Statistics ────────────────────────────────────────────────────
            AppScreen.Statistics -> StatisticsScreen(
                uiState = statsUiState,
                onBack  = ::goBack
            )

            // ── Game (Chat) ───────────────────────────────────────────────────
            is AppScreen.Game -> ChatScreen(
                uiState          = gameUiState,
                onChoiceSelected = gamePresenter::onChoiceSelected,
                onToggleStats    = gamePresenter::toggleStats,
                onRestart        = gamePresenter::restartGame,
                onNavigateToMenu = {
                    gamePresenter.saveAndPause()
                    navForward = false
                    backStack  = listOf(AppScreen.MainMenu)
                }
            )
        }
    }
}
