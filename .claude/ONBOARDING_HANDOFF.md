# Onboarding Flow — Handoff Context

Status: ~60% done. Data layer, DI, i18n, and icon are committed to the working tree.
Remaining: `OnboardingScreen.kt` (the UI), `AppNavigation.kt` wiring, build verification.

## Goal

Implement the 4-page intro onboarding from the design file `.claude/Onboarding.dc.html`
(exported from claude.ai/design; open it in a browser to see the interactive mock, 390×844).
Shown once on first launch, before MainMenu. Pages:

1. **Statistics** — scam-loss stats: World $1.03T / Asia $688B / Kazakhstan 17.5B ₸, "4% recover" callout, sources footnote (GASA 2024 · МВД РК).
2. **Game + education** — "this is a game AND a trainer", embedded fake chat mock (scam call message, Asan advisor tip, wrong/right answer options).
3. **Patterns** — "almost all scams look alike": 6 pattern chips, "90% built on 5–6 tricks" callout, footnote.
4. **CTA** — check-mark hero, "Начать игру" button, "У меня уже есть аккаунт" link.

Chrome: progress dots (clickable) + Skip (jumps to last page, NOT exit) on top; bottom bar with back button (hidden on page 0), CTA ("Далее" → "Начать игру" on last page), account link (visible only on last page). Swipe between pages.

## Already done (in working tree — do not redo)

| File | Change |
|---|---|
| `shared/src/commonMain/kotlin/kz/fearsom/financiallifev2/i18n/StringKeys.kt` | Added `UI_ONBOARD_*` consts (~50 keys) in an "OnboardingScreen" section before MainMenuScreen |
| `shared/.../i18n/translations/ru.kt`, `kk.kt`, `en.kt` | Added all `ui_onboard_*` translations after the LoginScreen block. Emphasis spans are marked `**…**` inside the strings |
| `shared/.../i18n/Strings.kt` | Added `uiOnboard*` getters (inline `StringKeys.*` style, like the Achievements section). Includes `uiOnboardS3Chips: List<String>` |
| `composeApp/src/commonMain/kotlin/kz/fearsom/financiallifev2/data/OnboardingRepository.kt` | New. `isCompleted(): Boolean` / `markCompleted()`, SecureStorage-backed, key `onboarding_completed_v1` |
| `composeApp/.../di/AppModule.kt` | Registered `single { OnboardingRepository(secureStorage = get<SecureStorage>()) }` before the feature-flags block |
| `composeApp/.../ui/icons/LineIcons.kt` | Added `LineIcons.Check` (stroke 2.6f, path M20 6 → 9 17 → l-5-5) before `Send` |

## Remaining work

### 1. Create `composeApp/src/commonMain/kotlin/kz/fearsom/financiallifev2/ui/screens/OnboardingScreen.kt`

```kotlin
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,      // CTA on last page
    onLoginClick: () -> Unit,  // "already have an account" link
)
```

Structure:

```
Box(fillMaxSize, background = colors.backgroundDeep)
├─ top glow: Box 380.dp, Alignment.TopCenter, offset(y = -140.dp),
│    Brush.radialGradient(IndigoPrimary.copy(alpha=.24f) → Transparent)
├─ Column(fillMaxSize)
│  ├─ windowInsetsPadding(WindowInsets.statusBars) on the top row
│  ├─ Top row (padding h=26.dp): dots + Skip
│  ├─ HorizontalPager(state, Modifier.weight(1f))  // androidx.compose.foundation.pager
│  │    each page: Column(verticalScroll(rememberScrollState()), padding 26/16/26/24)
│  └─ Bottom bar
```

- `rememberPagerState(pageCount = { 4 })`, `val isLast = pagerState.currentPage == 3`
- Dots: active = 22×7.dp pill `IndigoPrimary`, inactive = 7×7.dp `colors.borderStrong`; animate width/color (`animateDpAsState`/`animateColorAsState`, tween 300); each clickable → `scope.launch { pagerState.animateScrollToPage(i) }`
- Skip: `Strings.uiOnboardSkip`, 13.sp SemiBold `colors.textSecondary`; alpha→0 + clicks disabled on last page; onClick → animateScrollToPage(3)
- CTA: `if (isLast) onFinish() else animateScrollToPage(page+1)`; label `Strings.uiOnboardNext` / `Strings.uiOnboardStart`
- Back button: alpha→0 + disabled on page 0; → animateScrollToPage(page-1)

**`**bold**` markup helper** (emphasis spans inside localized strings):

```kotlin
private fun boldMarkup(text: String, bold: SpanStyle): AnnotatedString =
    buildAnnotatedString {
        text.split("**").forEachIndexed { i, seg ->
            if (i % 2 == 1) withStyle(bold) { append(seg) } else append(seg)
        }
    }
```

**Design→token mapping** (design is dark-only; use semantic `LocalAppColors` so light theme works):

| Design | Token |
|---|---|
| `#0E1217` screen bg | `colors.backgroundDeep` |
| `#161C24` cards/bubbles/chips bg | `colors.backgroundCard` |
| `#13181F` chat-mock card bg | `colors.backgroundSubCard` |
| `#0B0E12` bottom bar bg | `colors.backgroundPanel` |
| `#232B35` card border | `colors.border` |
| `#2A3340` option/back-btn border, inactive dot | `colors.borderStrong` |
| `#1D242D` bottom-bar top hairline | `colors.divider` |
| `#EAEEF4` headings/values | `colors.textPrimary` |
| `#D6DCE4` body/emphasis spans | `colors.textBody` |
| `#B7C0CD` units/chip text | `colors.textEmphasis` |
| `#8B95A4` subtitles/meta | `colors.textSecondary` |
| `#5C6677` sources footnote | `colors.textHint` |
| `#5C6BE6` accent (labels, dots, CTA, "10" box) | `IndigoPrimary` (theme-independent) |
| `#7C8AF0→#7C5CE6` Asan avatar gradient | `IndigoLight` → `PurpleGradient` |
| `#171D2A`/`#2A3354` Asan bubble | `colors.bubbleAi` / `colors.bubbleAiBorder` |
| `#FF6B5E` coral (bars, −100%, 4%) | `colors.accentNegative` |
| `#FF8A7E` coral card label | `colors.accentNegativeText` |
| `#F0B94A` amber (Asia bar, note dot) | `colors.accentWarning` (label: `accentWarningText`) |
| `#2BC48A` emerald (right option, 90%) | `colors.accentPositive` |

**Per-page specs** (px→dp 1:1; all strings via `Strings.uiOnboard*`):

- Section label: 11.sp Bold, letterSpacing 2.5.sp, `IndigoPrimary`, bottom pad 12
- H1: 27.sp W800 (ExtraBold), lineHeight 31.5.sp, letterSpacing (-0.6).sp; page 4: 28.sp/33.sp centered
- Subtitle: 14.sp, lineHeight 21.sp, `textSecondary`; emphasis span = `textBody` (use boldMarkup)
- **P1 stat card**: Row(`height(IntrinsicSize.Min)`, bg backgroundCard, border 1.dp `colors.border`, RoundedCornerShape(16.dp), padding 15/16, gap 14) → accent bar Box(width 4.dp, fillMaxHeight, radius 4) + Column: label 11.sp Bold ls 2.sp; value `MonoFontFamily` 26.sp Bold `textPrimary` + unit 15.sp Bold `textEmphasis` (baseline row); desc 12.5.sp `textSecondary` lh 17.sp. World=coral, Asia=amber, KZ=coral. Cards gap 11.dp
- **P1 "4%" callout**: Row(bg `accentNegative.copy(.07f)`, border `.copy(.28f)`, radius 14, padding 13/15, gap 12): value Mono 22.sp Bold accentNegative + text 12.5.sp `textBody`. Then sources 10.5.sp `textHint`, top pad 12
- **P2 chat mock**: Column(bg backgroundSubCard, border `colors.border`, radius 20, padding 15, gap 11):
  - date chip centered: bg backgroundCard, border, pill, padding 4/12, 10.5.sp SemiBold textSecondary
  - scam bubble: maxWidth 86%, bg backgroundCard, border, RoundedCornerShape(16,16,16,5 — bottomStart=5), padding 11/13, 12.5.sp textBody lh 18.sp
  - Asan row: avatar 26.dp circle Brush.linearGradient(IndigoLight, PurpleGradient), "А" white Bold 12.sp; bubble bg bubbleAi border bubbleAiBorder RoundedCornerShape(topStart=5, rest 16), padding 10/12, 12.sp textEmphasis; `**никогда**` → Bold textPrimary
  - options (gap 8): wrong = border borderStrong radius 12 padding 11/13, Row SpaceBetween: 12.5.sp Medium textPrimary + badge Mono 11.sp Bold accentNegative; right = border `accentPositive.copy(.45f)` bg `.copy(.08f)`, badge 11.sp Bold accentPositive
  - fact row below card (top pad 16, gap 10): Box 34.dp radius 10 IndigoPrimary with "10" Mono Bold 15.sp White + text 12.5.sp textSecondary, bold prefix textPrimary
- **P3**: FlowRow(@OptIn(ExperimentalLayoutApi::class), gaps 8.dp) of `Strings.uiOnboardS3Chips`: border borderStrong, bg backgroundCard, pill, padding 8/14, 13.sp Medium textEmphasis. Then 90% card: Row(bg `accentPositive.copy(.08f)`, border `.copy(.3f)`, radius 18, padding 17/18, gap 15): Mono 34.sp Bold accentPositive + 12.5.sp textBody (bold span textPrimary). Note row: 6.dp dot accentWarning (top pad 6.dp) + 12.5.sp textSecondary lh 18.sp
- **P4** (centered, fillMaxSize): glow + 96.dp circle Brush.linearGradient(IndigoLight, IndigoPrimary, IndigoDark) with `LineIcons.Check` 44.dp tint White; label; title; subtitle 14.sp textSecondary max width 280.dp; all centered, bottom-ish spacing 26/12/12
- **Bottom bar**: Column(bg backgroundPanel, 1.dp top hairline `colors.divider` (Box height 1), padding 24/12/24/26 + `windowInsetsPadding(WindowInsets.navigationBars)`):
  - account link: height ~20.dp, centered, 13.sp Medium textSecondary, animated alpha (visible = isLast), clickable → onLoginClick
  - Row(gap 12): back Box 54.dp radius 16 bg backgroundCard border borderStrong, `LineIcons.ChevronLeft` tint textEmphasis; CTA Box(weight 1, height 54, radius 16, bg IndigoPrimary): label White Bold 16.sp

Conventions: plain `Box+clickable`, no material Buttons on redesigned screens; `MonoFontFamily`, `IndigoPrimary` etc. from `ui/theme`; no ViewModels.

### 2. Wire into `composeApp/.../ui/navigation/AppNavigation.kt`

- `sealed interface AppScreen`: add `object Onboarding : AppScreen`; in `depth()`: `AppScreen.Onboarding -> 0`
- Inject: `val onboardingRepo: OnboardingRepository = koinInject()`
- Splash-exit effect — replace `backStack = listOf(AppScreen.MainMenu)` with:

```kotlin
backStack = if (onboardingRepo.isCompleted()) listOf(AppScreen.MainMenu)
            else listOf(AppScreen.Onboarding)
```

- In the `when (screen)` add:

```kotlin
AppScreen.Onboarding -> OnboardingScreen(
    onFinish = {
        onboardingRepo.markCompleted()
        navForward = true
        backStack = listOf(AppScreen.MainMenu)
    },
    onLoginClick = {
        onboardingRepo.markCompleted()
        navForward = true
        backStack = listOf(AppScreen.MainMenu, AppScreen.Login)
    }
)
```

### 3. Verify

```bash
./gradlew :composeApp:assembleDebug
```

Locale switching: UI recomposes via `key(settingsUiState.currentLocale)` in AppNavigation, so `Strings.*` getters re-read automatically — nothing extra needed.

## Gotchas

- `Strings` getters must use the inline `get(StringKeys.UI_ONBOARD_*)` style (top-import style causes huge import list; both compile, inline is what's used).
- `FlowRow` needs `@OptIn(ExperimentalLayoutApi::class)` on CMP 1.10.3.
- Don't add a fake status bar / 390×844 frame — that's design-mock chrome only.
- AppNavigation already applies global `padding(bottom = 32.dp)` and `backgroundDeep`.
- Skip ≠ exit: it jumps to page 4 (per design JS `skip: () => goTo(total-1)`).
- System back on onboarding exits the app (single-entry back stack) — acceptable, matches Splash behavior.
- Kotlin 2.3.20 / Compose Multiplatform 1.10.3 / minSdk 26.
