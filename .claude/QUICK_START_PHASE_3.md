# 🚀 Quick Start — Phase 3

**Time**: 2-3 days  
**Complexity**: Medium  
**Breaking Changes**: None

---

## TL;DR

Phase 2 is done. Typography system is locked down (Option B: Fintech + Diary).  
Phase 3 is ready to start. Three main tasks:

1. **MainMenu hero** — Promote ActiveSessionCard to top, reduce button grid to 3 items
2. **StatsPanelOverlay reorder** — Freedom % hero + cash flow widget + asset grid
3. **Game-over celebration** — Add 600ms pause + optional VFX

---

## Start Here

### Read These (5 min)
1. `PHASE_2_TYPOGRAPHY_COMPLETE.md` — What Phase 2 delivered
2. `PHASE_3_HIERARCHY_PLAN.md` — Detailed specs for Phase 3 tasks

### Code Files Ready to Modify
- **MainMenuScreen.kt** — Hero card promotion
- **StatsPanelOverlay.kt** — Reorder and new widgets
- **ChatScreen.kt** — Game-over celebration (mostly done, just need VFX)

### No Setup Needed
- ✅ Fonts.kt is ready (will use system fonts until custom fonts bundled)
- ✅ Type.kt is updated
- ✅ ChatScreen is updated
- ✅ AppTopBar and AccentCard are available
- ✅ No dependencies missing

---

## Files You'll Edit in Phase 3

### 3.1 MainMenu Hero
**File**: `composeApp/src/commonMain/kotlin/.../screens/MainMenuScreen.kt`

**What to do**:
```kotlin
// Move this to top of Column:
ActiveSessionCard(
    gameState = gamePresenter.uiState.gameState,
    onPlayClick = { gamePresenter.resumeGame() },
    modifier = Modifier.fillMaxWidth().padding(16.dp)
)

// Replace the 4-button grid with 3-column buttons:
Row(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
) {
    MenuButton("📊 Stats", Modifier.weight(1f), ...)
    MenuButton("⚙️ Settings", Modifier.weight(1f), ...)
    MenuButton("ℹ️ About", Modifier.weight(1f), ...)
}
```

### 3.2 StatsPanelOverlay Reorder
**File**: `composeApp/src/commonMain/kotlin/.../components/StatsPanelOverlay.kt`

**What to do**:
```kotlin
// Change from:
Column { Capital, Investments, Debt, Income, Expenses, Stress }

// To:
Column {
    FreedomHeroCard(freedomPercent)      // ← Hero 1
    MonthlyFlowCard(monthlyFlow)          // ← Hero 2
    Row { Capital, Investments, Debt }    // ← Secondary grid
    Row { Income, Expenses, Stress }      // ← Secondary grid
}
```

### 3.3 Game-Over Celebration
**File**: `composeApp/src/commonMain/kotlin/.../screens/ChatScreen.kt`

**What to do**:
```kotlin
// Add flag:
var showGameOverBar by remember { mutableStateOf(false) }

// Add delay:
LaunchedEffect(uiState.gameState?.isGameOver) {
    if (uiState.gameState?.isGameOver == true) {
        delay(600)  // ← Celebration beat
        showGameOverBar = true
    }
}

// Add optional VFX:
if (uiState.gameState?.isGameOver == true && !showGameOverBar) {
    CelebrationOverlay()  // ← Create this
}
```

---

## Helpful Context

### Brand Voice Decision (Phase 2)
✅ **Option B Won**: Fintech world (dark glass) + diary as card overlay  
✅ **Typography**: Inter (UI) + Caveat (diary) — currently system fonts, custom fonts optional  
✅ **Why**: Scales to future features, leverages existing code, clearer brand

### Components Available (From Phase 1)
- ✅ `AppTopBar(title, subtitle?, onBack, actions)` — Applied to 5 screens
- ✅ `AccentCard` — Reusable card with proper press-state
- ✅ `StaticCard` — Variant for disabled displays
- ✅ `DiaryTextStyle` — Caveat font for diary entries
- ✅ `DiaryHeaderStyle` — Caveat font for headers

### Design System (Complete)
```
Colors:   AppColors, Color.kt, Theme.kt ✅
Shapes:   Shapes.kt ✅
Typography: Fonts.kt, Type.kt ✅ (NEW)
Icons:    Material Symbols ✅
Spacing:  4dp grid (in code) ✅
```

---

## Testing Checklist

After each task:

### 3.1 MainMenu Hero
- [ ] Hero card appears when game active
- [ ] Hero card disappears when no active game
- [ ] Play button on hero card navigates to game
- [ ] 3-button grid still responsive on small screens

### 3.2 StatsPanelOverlay
- [ ] Freedom % is largest text on overlay
- [ ] Monthly flow card shows positive/negative color
- [ ] Asset grid reflows correctly on small screens
- [ ] All numbers calculate correctly

### 3.3 Game-Over
- [ ] 600ms pause before restart bar visible
- [ ] Confetti/sparkles play during pause (if implementing VFX)
- [ ] Restart button still works after pause
- [ ] Works on both light and dark modes

---

## Optional Enhancements (Phase 4)

- Card refactor (migrate 6+ cards to AccentCard) — 2-3 hours
- Custom fonts (Inter + Caveat) — 1 hour setup, zero behavior changes
- Reduce-motion support — 1-2 hours
- Dynamic type (accessibility) — 1-2 hours
- i18n hardening — review strings for format strings

---

## Key Metrics

After Phase 3, you'll have:
- ✅ Hero CTA on main screen (drives Play engagement)
- ✅ Clear financial metrics (freedom % front and center)
- ✅ Celebration moment (players feel achievement)
- ✅ Consistent typography (brand voice finalized)
- ✅ No breaking changes (backward compatible)

---

## Questions?

Refer to:
- `PHASE_3_HIERARCHY_PLAN.md` — Full specs with code examples
- `PHASE_2_TYPOGRAPHY_COMPLETE.md` — What Phase 2 delivered
- `SESSION_SUMMARY_PHASE_2_COMPLETE.md` — High-level overview

---

**Ready to start? Begin with MainMenuScreen.kt for Phase 3.1.**

*Estimated 2-3 days for all three tasks. No setup required — just code and test.*
