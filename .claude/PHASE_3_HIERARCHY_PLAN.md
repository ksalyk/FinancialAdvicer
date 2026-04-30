# 📋 Phase 3 Plan — Hierarchy Fixes + Polish

**Duration**: 2-3 days  
**Complexity**: Medium (0-1 breaking changes)  
**ROI**: High (fixes perception of two most-visited non-chat screens)

---

## Phase 3.1 — MainMenu Hero Promotion

### Problem
MainMenu has **4 equal-weight buttons** (Play/Statistics/Settings/About) that all compete visually:
```
┌─────────────────────┐
│  [Play] [Stats]     │
│  [Settings] [About] │
└─────────────────────┘
```
But 95% of players tap **Play**. The current layout treats all CTAs as peers.

### Solution
Promote `ActiveSessionCard` (currently hidden in the top bar) to a **featured hero position**:
```
┌──────────────────────────────────────┐
│   🎮 CONTINUE YOUR JOURNEY           │  ← AccentCard (hero)
│   Asan • 8 months in • 12.3M assets  │
│                [PLAY] >              │
├──────────────────────────────────────┤
│  [Statistics] [Settings] [About]    │  ← Secondary actions
└──────────────────────────────────────┘
```

### Files to Modify
- **`MainMenuScreen.kt`**
  - Extract `ActiveSessionCard` from `DiaryTopBar` (promote it)
  - Move to top of Column with full width
  - Reduce button grid to 3 horizontal items (Stats/Settings/About)
  - Keep new-game fallback for when no active session exists

### Specific Changes
```kotlin
// MainMenuScreen layout
Column(modifier = Modifier.fillMaxSize()) {
    // Hero: Active session (or call-to-action to start)
    if (gamePresenter.uiState.hasActiveSession) {
        ActiveSessionCard(
            gameState = gamePresenter.uiState.gameState,
            onPlayClick = { gamePresenter.resumeGame() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    } else {
        NewGameHeroCard(  // ← Create this: "Start a New Journey"
            onNewGameClick = { navigateToCharacterSelect() }
        )
    }
    
    Spacer(Modifier.weight(1f))
    
    // Secondary actions: 3-column grid
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MenuButton("📊 Stats", modifier = Modifier.weight(1f), onClicked = onStatsClick)
        MenuButton("⚙️ Settings", modifier = Modifier.weight(1f), onClicked = onSettingsClick)
        MenuButton("ℹ️ About", modifier = Modifier.weight(1f), onClicked = onAboutClick)
    }
}
```

---

## Phase 3.2 — StatsPanelOverlay Reorder

### Problem
Current StatsPanelOverlay stacks stats as a dense grid below the close button:
```
┌─ Stats Panel ─────┐
│ [X]               │
│                   │
│ 💰 Capital: 4.2M  │ ← Equal weight
│ 📈 Investments    │
│ 🎯 Freedom: 42%   │
│ 📉 Debt: 1.2M     │
└───────────────────┘
```

The **freedom percentage** (the core metric) is buried in the middle. **Net monthly cash flow** (the most actionable metric) isn't surfaced at all.

### Solution
Reorder to pyramid hierarchy — **freedom % as hero, cash flow as full-width widget**:
```
┌─ Financial Overview ──┐
│ [X]                   │
│ ┌─────────────────┐   │
│ │ 🎯 Financial   │   │ ← Freedom % hero
│ │    Freedom: 42%│   │    (large, centered)
│ └─────────────────┘   │
│ ┌─────────────────┐   │
│ │ 💸 Monthly Flow │   │ ← Cash flow widget
│ │ +180,000 ₸/mo  │   │    (full-width)
│ └─────────────────┘   │
│ Capital  Invest  Debt │ ← 3-column grid
│ 4.2M     1.8M    1.2M │
└───────────────────────┘
```

### Files to Modify
- **`StatsPanelOverlay.kt`**
  - Move freedom % to top in oversized display
  - Create new `MonthlyFlowCard` composable below
  - Restructure remaining stats (capital/investments/debt/income/expenses) as 2-row grid

### Specific Changes
```kotlin
// StatsPanelOverlay content structure
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
) {
    // Hero 1: Freedom %
    FreedomHeroCard(
        freedomPercent = playerState.freedomPercent,
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(Modifier.height(16.dp))
    
    // Hero 2: Monthly cash flow
    MonthlyFlowCard(
        monthlyNetCashFlow = playerState.calculateMonthlyFlow(),
        trend = calculateFlowTrend(playerState),
        modifier = Modifier.fillMaxWidth()
    )
    
    Spacer(Modifier.height(16.dp))
    
    // Secondary: Asset grid (2 rows × 3 cols)
    Text("Financial Assets", style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(8.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatWidget("Capital", playerState.capital, Modifier.weight(1f))
        StatWidget("Investments", playerState.investments, Modifier.weight(1f))
        StatWidget("Debt", playerState.debt, Modifier.weight(1f))
    }
    
    Spacer(Modifier.height(12.dp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatWidget("Income", playerState.income, Modifier.weight(1f))
        StatWidget("Expenses", playerState.expenses, Modifier.weight(1f))
        StatWidget("Stress", playerState.stress, Modifier.weight(1f))
    }
}
```

---

## Phase 3.3 — Game-Over Celebration Beat

### Problem
When a player reaches financial freedom, the game immediately:
1. Shows the final diary message
2. Instantly shows the "Game Over" restart bar

There's **no celebration moment**. Players don't feel their achievement.

### Solution
Add a **600ms pause** with optional VFX before showing the restart bar:

```kotlin
// DiaryGameOverBar (in ChatScreen)
if (uiState.gameState?.isGameOver == true) {
    AnimatedVisibility(
        visible = showGameOverBar,  // ← Control this with delayed flag
        enter = slideInUp(tween(400)) + fadeIn(tween(400))
    ) {
        DiaryGameOverBar(...)
    }
}

LaunchedEffect(uiState.gameState?.isGameOver) {
    if (uiState.gameState?.isGameOver == true) {
        delay(600)  // ← Celebration beat
        showGameOverBar = true
    }
}

// Optional: Add confetti/sparkle animation during the 600ms
if (uiState.gameState?.isGameOver == true && !showGameOverBar) {
    CelebrationOverlay()  // ← New composable with particles/sparkles
}
```

### Files to Modify
- **`ChatScreen.kt`**
  - Add `showGameOverBar` state flag
  - Add LaunchedEffect to delay visibility
  - Optional: Create `CelebrationOverlay()` composable for confetti/particle effects

### Specific Changes
```kotlin
@Composable
fun ChatScreen(...) {
    var showGameOverBar by remember { mutableStateOf(false) }
    
    LaunchedEffect(uiState.gameState?.isGameOver) {
        if (uiState.gameState?.isGameOver == true) {
            showGameOverBar = false
            delay(600)
            showGameOverBar = true
        }
    }
    
    // In Column:
    if (uiState.gameState?.isGameOver == true && !showGameOverBar) {
        // Show celebration animation
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            CelebrationOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // ... rest of chat messages ...
    
    // Restart bar (shown after celebration)
    AnimatedVisibility(
        visible = showGameOverBar,
        enter = slideInUp(tween(400)) + fadeIn(tween(400))
    ) {
        DiaryGameOverBar(...)
    }
}
```

---

## Phase 3.4 — Card Refactor (AccentCard Migration)

### Status
**AccentCard** composable created in Phase 1.4 at `components/Cards.kt`.

### Problem
~6 card types use duplicated styling logic:
- `ActiveSessionCard`
- `EraCard`
- `CharacterCard`
- `DiaryChoiceCard` (partially)
- `StatWidget`
- Custom buttons that should be cards

### Solution
Refactor these to use `AccentCard` with parameter overrides:

```kotlin
// BEFORE:
ActiveSessionCard(
    modifier = Modifier
        .fillMaxWidth()
        .background(colors.surfaceVariant)
        .border(...)
        .padding(...)
)

// AFTER:
AccentCard(
    onClick = onPlayClick,
    modifier = Modifier.fillMaxWidth(),
    gradient = AppGradient.GoldToDark,
    borderColor = colors.primary,
    content = {
        // Session card content
    }
)
```

### Files to Modify (Low Priority — Optional for Phase 3)
- `ActiveSessionCard` → wrap in AccentCard
- `EraCard` → wrap in AccentCard
- `CharacterCard` → wrap in AccentCard
- etc.

**Note**: Not critical for Phase 3 MVP. Can be deferred to Phase 4 (Polish). The AccentCard is ready, but migrating 6 components is 2-3 hours of refactor work.

---

## Recommended Phase 3 Scope (MVP)

### Must-Do (1-2 days)
✅ **3.1** MainMenu hero (promote ActiveSessionCard)  
✅ **3.2** StatsPanelOverlay reorder (freedom % + cash flow hero)  
✅ **3.3** Game-over celebration beat (600ms delay + optional VFX)  

### Nice-to-Have (0.5-1 day if time)
🟡 **3.4** Card refactor (AccentCard migration) — can defer to Phase 4

---

## Success Criteria

After Phase 3:
- ✅ MainMenu prioritizes Play (hero CTA)
- ✅ StatsPanelOverlay surfaces freedom % + cash flow upfront
- ✅ Game-over moment feels celebratory, not abrupt
- ✅ All text uses Inter (UI) / Caveat (diary) appropriately
- ✅ No accessibility regressions (touch targets, contrast, reduce-motion)

---

## Testing Checklist

- [ ] MainMenu: Hero card appears when session exists
- [ ] MainMenu: New-game fallback appears when no session
- [ ] StatsPanelOverlay: Freedom % is the most prominent element
- [ ] StatsPanelOverlay: Monthly flow calculated correctly
- [ ] Game-over: 600ms pause before restart bar appears
- [ ] Game-over: Optional VFX (confetti) plays during pause
- [ ] Typography: Diary text uses new DiaryTextStyle
- [ ] Responsive: 2-row grid reflows on small screens
- [ ] Accessibility: All interactive elements have min 48dp touch targets
- [ ] i18n: Russian labels remain correct (no broken UTF-8)

---

## Files Ready for Phase 3 Implementation
1. ✅ `Fonts.kt` — Typography system (Phase 2 delivered)
2. ✅ `Type.kt` — Updated typography (Phase 2 delivered)
3. ✅ `Cards.kt` — AccentCard component (Phase 1.4 delivered)
4. 📝 `ChatScreen.kt` — Restart confirmation (Phase 3.3 delivered)
5. 🔄 **Awaiting input**: MainMenuScreen, StatsPanelOverlay refactor
