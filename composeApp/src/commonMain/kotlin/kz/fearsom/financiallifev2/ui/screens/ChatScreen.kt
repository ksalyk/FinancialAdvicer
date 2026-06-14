package kz.fearsom.financiallifev2.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kz.fearsom.financiallifev2.data.TypingPace
import kz.fearsom.financiallifev2.model.ChatMessage
import kz.fearsom.financiallifev2.model.MessageSender
import kz.fearsom.financiallifev2.presentation.GameUiState
import kz.fearsom.financiallifev2.ui.components.chat.AnimatedMessageEntry
import kz.fearsom.financiallifev2.ui.components.chat.DiaryActionsPanel
import kz.fearsom.financiallifev2.ui.components.chat.DiaryGameOverBar
import kz.fearsom.financiallifev2.ui.components.chat.DiaryMessageItem
import kz.fearsom.financiallifev2.ui.components.chat.DiaryTopBar
import kz.fearsom.financiallifev2.ui.components.chat.DiaryWritingIndicator
import kz.fearsom.financiallifev2.ui.components.chat.SkipButtonOverlay
import kz.fearsom.financiallifev2.ui.components.core.StatsPanelOverlay
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.LocalAppColors

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    uiState: GameUiState,
    typingAnimationEnabled: Boolean = true,
    typingAnimationPace: TypingPace = TypingPace.NORMAL,
    onChoiceSelected: (String) -> Unit,
    onToggleStats: () -> Unit,
    onRestart: () -> Unit,
    onNavigateToMenu: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val listState = rememberLazyListState()
    val messages = uiState.gameState?.messages ?: emptyList()
    val playerState = uiState.gameState?.playerState

    // ID of the latest CHARACTER message — the only one that gets animated.
    // All earlier messages render instantly (scroll restore, history, etc.)
    val latestCharacterMessageId = remember(messages) {
        messages.lastOrNull { it.sender == MessageSender.CHARACTER }?.id
    }

    // ── Per-message revealed-length states ────────────────────────────────────
    // Stored at screen scope so they survive LazyColumn item recycling.
    // Each entry is an independent MutableState so only the specific item
    // recomposes on each character reveal (SnapshotStateMap would invalidate ALL readers).
    val displayedLengths = remember { HashMap<String, MutableState<Int>>() }

    // Return (or lazily create) the state for a given message.
    // The latest CHARACTER message is initialised to 0 so the item renders empty
    // from the very first composition frame — no full-text flash before animation.
    // All other messages default to full length so they appear instantly.
    fun lengthStateFor(msg: ChatMessage): MutableState<Int> =
        displayedLengths.getOrPut(msg.id) {
            val initial =
                if (typingAnimationEnabled && msg.id == latestCharacterMessageId) 0
                else msg.text.length
            mutableStateOf(initial)
        }

    // Whether any animation is currently running — drives skip button visibility.
    var isAnimating by remember { mutableStateOf(false) }
    var scrollBlocked by remember { mutableStateOf(false) }

    // ── Screen-level typing animation ─────────────────────────────────────────
    // Owned here, NOT inside the item composable, so LazyColumn recycling,
    // recomposition, and lambda re-capture can never interrupt it.
    // Keyed on latestCharacterMessageId + skipRequestedForMessageId so:
    //   • it restarts (new message) when a new message arrives
    //   • it restarts (skip path) only for the message the user actually skipped
    var skipRequestedForMessageId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(latestCharacterMessageId, typingAnimationEnabled, skipRequestedForMessageId) {
        if (!typingAnimationEnabled || latestCharacterMessageId == null) {
            isAnimating = false
            scrollBlocked = false
            return@LaunchedEffect
        }
        val msg = messages.lastOrNull { it.id == latestCharacterMessageId }
            ?: return@LaunchedEffect
        val target = msg.text.length

        // Skip pressed → jump to end immediately and stop.
        if (skipRequestedForMessageId == msg.id) {
            displayedLengths.getOrPut(msg.id) { mutableStateOf(target) }.value = target
            isAnimating = false
            scrollBlocked = false
            return@LaunchedEffect
        }

        // Normal animation: reveal character by character.
        // getOrPut preserves the existing state object so the item's `by` delegate
        // keeps its subscription — replacing the object (= would break recomposition.
        val state = displayedLengths.getOrPut(msg.id) { mutableStateOf(0) }
        state.value = 0  // reset in-place, never replace the object
        isAnimating = true
        for (i in 0 until target) {
            delay(typingAnimationPace.charDelayMs)
            state.value = i + 1
        }
        isAnimating = false
        scrollBlocked = false
    }

    LaunchedEffect(uiState.isTyping, isAnimating) {
        if (!uiState.isTyping && !isAnimating) {
            scrollBlocked = false
        }
    }

    // Tracks message IDs that have entered the viewport at least once.
    // Used only for the slide-in entry animation suppression on scroll-back.
    val seenMessageIds = remember { HashSet<String>() }

    // Scroll to new message when user selects an option or messages arrive
    LaunchedEffect(messages.size, scrollBlocked) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Ensure we're at the latest CHARACTER message when animation starts
    LaunchedEffect(latestCharacterMessageId) {
        if (latestCharacterMessageId != null && messages.isNotEmpty()) {
            delay(32) // Wait one frame for layout
            val index = messages.indexOfFirst { it.id == latestCharacterMessageId }
            if (index >= 0) {
                listState.scrollToItem(index)
            }
        }
    }

    // If typing animation is disabled, scroll to end of message after it renders
    // (since message appears instantly without character-by-character animation)
    if (!typingAnimationEnabled) {
        LaunchedEffect(latestCharacterMessageId, messages.size) {
            if (latestCharacterMessageId != null && messages.isNotEmpty()) {
                delay(100) // Wait for layout to measure the full message height
                listState.animateScrollToItem(messages.size - 1, Int.MAX_VALUE)
            }
        }
    }

    // When action panel appears, adjust scroll to keep message visible above it.
    // The panel is approximately 160-200dp tall (label + 1-2 options + padding + nav insets).
    // Use an offset that accounts for panel height so message bottom sits just above panel top.
    // 250dp offset ensures the message is comfortably visible above the action panel.
    LaunchedEffect(uiState.currentOptions.isNotEmpty(), messages.size) {
        if (uiState.currentOptions.isNotEmpty() && messages.isNotEmpty()) {
            // Wait for panel animation to fully complete and layout to settle
            delay(500)
            // 250dp offset positions the last message such that its bottom sits
            // approximately 250 pixels from the viewport top (accounting for panel taking bottom space)
            listState.scrollToItem(messages.size - 1, 250)
        }
    }

    // Track-scroll to bottom while the typing animation reveals characters.
    // Each character grows the card height, pushing content past the viewport edge.
    // Use 250dp offset to keep bottom of message visible during animation.
    // Keyed on both isAnimating AND messages.size so the effect always captures the
    // correct last-item index even if a message arrives while animation is running.
    LaunchedEffect(isAnimating, messages.size) {
        if (!isAnimating) return@LaunchedEffect
        while (isAnimating) {
            listState.scrollToItem(messages.size - 1, 250)
            delay(50) // 20 fps — sufficient for text-growth tracking
        }
    }

    // Continue scrolling to keep message visible while action panel slides in.
    // The panel animation takes ~240ms (slideInVertically + fadeIn).
    // We track for 300ms to cover animation completion + layout settlement.
    val isPanelAppearing = uiState.currentOptions.isNotEmpty() && !isAnimating
    LaunchedEffect(isPanelAppearing, messages.size) {
        if (!isPanelAppearing || messages.isEmpty()) return@LaunchedEffect
        var elapsed = 0
        while (elapsed < 300) {
            listState.scrollToItem(messages.size - 1, 250)
            delay(50)
            elapsed += 50
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundChat)
    ) {
        // Top gradient vignette
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            colors.backgroundDeep,
                            Color.Transparent
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            DiaryTopBar(
                playerState = playerState,
                characterName = uiState.characterName,
                characterEmoji = uiState.characterEmoji,
                characterTitle = uiState.characterTitle,
                onStatsClick = onToggleStats,
                onRestartClick = onRestart,
                onMenuClick = onNavigateToMenu
            )

            Box(modifier = Modifier.weight(1f)) {
                // Block user gesture scrolls while typing animation is running
                // But allow programmatic scrolls (auto-scroll during animation)
                val scrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            // Block only user gestures (Drag, Wheel), allow SideChannel (programmatic)
                            if (!isAnimating && !scrollBlocked) return Offset.Zero
                            return if (source == NestedScrollSource.SideEffect) Offset.Zero else available
                        }
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollConnection),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isFirstTime = message.id !in seenMessageIds
                        val isLatestChar = typingAnimationEnabled &&
                                message.id == latestCharacterMessageId
                        // Read this message's revealed length from the screen-scoped
                        // map.  lengthStateFor creates an entry with full length if not
                        // present, so old messages always render instantly.
                        val displayedLength by lengthStateFor(message)
                        SideEffect { seenMessageIds.add(message.id) }
                        AnimatedMessageEntry(animate = isFirstTime) {
                            DiaryMessageItem(
                                message = message,
                                playerState = playerState,
                                isLatestChar = isLatestChar,
                                displayedLength = displayedLength
                            )
                        }
                    }
                    stickyHeader {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (uiState.isTyping) {
                        item(key = "typing") {
                            DiaryWritingIndicator()
                        }
                    }
                }

                // ── Skip pill — extracted to break ColumnScope implicit receiver ─
                SkipButtonOverlay(
                    visible = isAnimating,
                    onSkip = {
                        skipRequestedForMessageId = latestCharacterMessageId
                        scrollBlocked = false
                    }
                )
            }

            val isVisible = uiState.currentOptions.isNotEmpty() && !uiState.isTyping
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                if (isVisible && !isAnimating) {
                    DiaryActionsPanel(options = uiState.currentOptions, onSelected = {
                        scrollBlocked = true
                        onChoiceSelected(it)
                    })
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .size(32.dp),
                        color = GoldPrimary
                    )
                }
            }

            if (uiState.gameState?.gameOver == true) {
                DiaryGameOverBar(uiState.gameState.endingType, onRestart)
            }
        }

        if (uiState.showStats && playerState != null) {
            StatsPanelOverlay(
                playerState = playerState,
                characterName = uiState.characterName,
                characterEmoji = uiState.characterEmoji,
                onDismiss = onToggleStats
            )
        }
    }
}