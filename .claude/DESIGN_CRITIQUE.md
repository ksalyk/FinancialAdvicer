# Design Critique & Redesign Plan — Finance LifeLine

**Reviewer:** code-review pass, 2026-04-28
**Scope:** `composeApp/src/commonMain/.../ui/**` — theme, screens, components
**Stage:** post-MVP, pre-polish (single brand identity not yet locked, no design system module)

---

## Overall Impression

The diary/notebook metaphor in `ChatScreen` is the strongest creative idea in the project — cream paper, ruled lines, sticky-note choices, "пишет в дневник…" — and it earns the gamified-finance positioning. Outside chat, however, the rest of the app speaks a different visual language entirely (dark + glassmorphism + neon-radial glow), so the user crosses a cliff edge between the menu and the gameplay. The biggest opportunity isn't more polish on either world — it's deciding which one is the brand and pulling the other 60% of the way toward it. Secondary opportunity: there is no design-system module yet (no spacing tokens, no elevation tokens, no motion tokens, no semantic radii), so every screen reinvents 14 dp / 16 dp / 24 dp / 28 dp / 52 dp from scratch and visual consistency drifts with each new file.

---

## 1. First Impression (2 seconds)

| Screen | Eye lands on | Should land on | Verdict |
|---|---|---|---|
| Splash | Pulsing gold "$" coin | Same | OK — but `$` in a KZT-only game is a brand mismatch (use `₸`, like MainMenu does) |
| Login | Gold logo + headline | Logo, then login form | OK — hierarchy works; particle layer adds cost without much payoff |
| MainMenu | Pulsing `₸` ring | Active session card → Continue CTA | Weak — primary action competes with logo and three other buttons of identical weight |
| EraSelection | Gold-tinted era card | Era card | OK |
| CharacterSelection | Tab switcher | Selected character | OK, but tabs read like nav, not a filter |
| ChatScreen | Diary card body text | Same | Strong — the metaphor lands |
| Stats overlay | Capital tile (top-left) | Net cash flow or freedom % | Weak — six tiles of equal weight, no hero metric |

**Emotional reaction:** menu/login feel like a polished crypto-fintech app; chat feels like a cozy paper journal. They don't share a soul.

---

## 2. Usability

| Finding | Severity | Recommendation |
|---|---|---|
| `ChatScreen` top bar packs Home + Diary icon + Stats + Restart in one row. Restart is destructive and one-tap with no confirmation. | 🔴 Critical | Move Restart behind a `DropdownMenu` (overflow). Add an `AlertDialog` confirmation before discarding a run. |
| `MenuButton` / `EraCard` / `PredefinedCharacterCard` / `BundleCard` set `pressed = true` on click but never reset it — the scale-down is one-shot, not a press animation. | 🟡 Moderate | Drive scale from `MutableInteractionSource.collectIsPressedAsState()` instead of a manual `var pressed`. |
| `LoginScreen` is wrapped in `verticalScroll` but never calls `imePadding()`, and the primary button isn't pinned. On small phones the IME covers "Войти". | 🔴 Critical | Add `Modifier.imePadding()` on the root Column, or split into a fixed footer button + scrollable form. |
| `LoginScreen` leaves `println("ERROR : $error")` inside an `AnimatedVisibility` content lambda — recomposes every frame the error is visible. | 🟡 Moderate (perf, not visual) | Remove. If you want to log, do it in a `LaunchedEffect(error)`. |
| Secondary screens (Era, Characters, Statistics, CharacterSelection) hard-code `padding(top = 52.dp)` to clear the status bar instead of using `WindowInsets.statusBars`. Will break on edge-to-edge cutouts and tablets. | 🟡 Moderate | Wrap content in `Modifier.windowInsetsPadding(WindowInsets.statusBars)` and use a shared `ScreenScaffold` composable. |
| The "back" affordance is a `TextButton("← Назад")` on five screens — small touch target, no `IconButton` semantics, blends with hint-color text. | 🟡 Moderate | Use `IconButton(Icons.AutoMirrored.Filled.ArrowBack)` (Material Symbols), 48 dp target, content description = "Назад". |
| In `CharacterSelectionScreen`, the "👥 Персонажи / 🎭 Бандлы" tabs are custom Boxes — no a11y role, no animated indicator, no swipe support. | 🟡 Moderate | Replace with `PrimaryTabRow` + `Tab`, or `SegmentedButton` (M3). |
| `DiaryActionsPanel` shows option text but the leading `☐` checkbox glyph never toggles; users may try to tap the box. | 🟢 Minor | Either drop the glyph or wire it to a real selected/unselected state. |
| `DiaryGameOverBar` blocks the choice panel when a run ends, but `LazyColumn` still scrolls under it — and there's no celebration moment between the last message and "Начать заново". | 🟡 Moderate | Add a 600 ms beat (confetti for `WEALTH`/`FREEDOM`, dimmed vignette for `BANKRUPTCY`) before the restart bar appears. |
| `StatsPanelOverlay` dismisses on background tap — but the drag-handle implies you can swipe-to-dismiss, which isn't wired. | 🟢 Minor | Either honor the affordance (`Modifier.draggable` + threshold) or replace the handle with a divider. |
| MainMenu "Continue" is rendered as a peer button to "Новая игра"/"Персонажи"/"Статистика", but conceptually it's a different verb (resume a save). | 🟡 Moderate | Promote `ActiveSessionCard` to a hero card with a filled gold button; demote the rest to outlined. |

---

## 3. Visual Hierarchy

**What draws the eye first across screens:**

- *Splash* — pulsing coin (correct).
- *Login* — gold radial glow → logo → headline → form. Correct, but the `drawParticles` layer is so faint (alpha 0.025–0.05) that it's mostly compute for nothing.
- *MainMenu* — the four `MenuButton`s all share identical typography (16 sp / SemiBold) and identical card height. Continue should be visually **first** by weight, not just by position.
- *ChatScreen* — diary card body wins, as intended. But the **date inside the diary header duplicates the date in the top bar**, so two competing date pieces fight at the top of every entry.
- *StatsPanelOverlay* — six 50/50 cards laid out 3×2 with no scale variation. The "Денежный поток" tile (the most actionable number for a finance game) reads with the same emphasis as "Расходы".

**Reading flow problems:**

- `DiaryEntryCard` draws ruled lines at a hard-coded `28.dp` spacing starting from `72.dp`, but `bodyMedium` has 24 sp line height with extra leading — lines slice through descenders rather than underline them. Either align lines to the text baseline grid or drop them.
- `DiaryFinancialEntry` uses `FontFamily.Monospace` for the multi-line report — good — but no column alignment within the report text means amounts don't form a tidy vertical edge.

**Emphasis fixes:**

| Element | Currently | Should be |
|---|---|---|
| MainMenu Continue | Same as siblings | Hero: filled gold button, larger card, "▶️ Продолжить" 18 sp Bold, secondary text 13 sp |
| Stats overlay net cash flow | One of six tiles | Full-width hero with arrow + delta from last month |
| Stats overlay freedom % | Tucked under stats | Move to top of overlay; this is the win condition |
| Era card "Inflation 18%" | Pill in row | Color-coded badge; if inflation > 12% give it a subtle red pulse |
| Character difficulty badge | 9 sp uppercase | 11 sp; 9 sp is below the comfort threshold |

---

## 4. Consistency

| Element | Issue | Recommendation |
|---|---|---|
| **Border radius** | 4 / 6 / 8 / 10 / 12 / 14 / 16 / 20 / 24 / 28 dp all appear, plus asymmetric `RoundedCornerShape(4, 12, 12, 4)` for diary cards and `(2, 10, 10, 10)` for choice notes. `AppShapes` is defined but barely used outside theme glue. | Establish 4 semantic radii: `xs=4`, `sm=8`, `md=14`, `lg=20`, `xl=28`. Replace ad-hoc values. Keep the asymmetric diary/choice shapes as **named** shapes in `AppShapes` (`diaryEntry`, `diaryChoice`). |
| **Spacing** | Vertical rhythm uses 4/6/8/10/12/14/16/18/20/22/24/28/32/40/44/56/72/88 dp interchangeably. | Define `Spacing` object with 4-pt scale: `xs=4, sm=8, md=12, lg=16, xl=24, xxl=32, hero=48`. Replace magic numbers. |
| **Iconography** | 100% emoji (🏠 📓 📊 🔄 ✍️ 💰 📈 😰 🎓 ☐ etc.). Renders inconsistently across iOS/Android/system fonts; not localizable; semantic-zero for TalkBack/VoiceOver; vertical alignment with text drifts. | Adopt **Material Symbols** (extended icon set ships free) for chrome/UI affordances. Reserve emoji exclusively for **content** — diary stickers, character avatars, scene tags, choice flavor. Rule of thumb: an emoji that's also a button is a bug. |
| **Typography** | `FontFamily.Default` everywhere. No display face, no monospace numerics. | Ship a typeface pair: **Inter** (or **Manrope**) for UI body + **Caveat** / **Kalam** / **Indie Flower** for diary handwriting voice. Use `FontFeatureSettings("tnum")` on numeric stat tiles so digits don't dance. |
| **Top bars** | Five screens reimplement "TextButton(← Назад)" + `padding(top = 52.dp)` independently. ChatScreen has its own `Surface` top bar with elevation 6 dp. | Create `AppTopBar(title, onBack, actions)` composable. ChatScreen needs its own variant (`DiaryTopBar`), but share the back-affordance + insets logic. |
| **Card chrome** | `MenuButton`, `EraCard`, `PredefinedCharacterCard`, `BundleCard`, `CharacterRosterCard`, `ActiveSessionCard` all share the same recipe (`clip` + `background(gradient)` + `border(accent.copy(0.4))` + `padding(16.dp)`) but each reimplements it. | Extract `AccentCard(accent: Color, locked: Boolean, onClick: () -> Unit, content: ...)` — also gives one place to fix the `pressed` bug from §2. |
| **Color usage** | Light-mode `BubbleCharacterLight = 0xFFE8F0FF` and `BubblePlayerLight = 0xFFF0F5FF` are 8 lightness points apart — almost indistinguishable. | Increase contrast: bubble character → `#DCE7FF`, bubble player → diary-cream `#FFF8EC` (matches the diary world even in light mode). |
| **Difficulty colors** | EASY=Green, MEDIUM=Gold, HARD=Red, NIGHTMARE=Purple. Gold + Green + Red also encode capital/income/debt elsewhere. Cross-mapping is risky for color-blind users. | Add a shape or icon variant in addition to color: ●○○○ EASY → ●●●● NIGHTMARE, or replace with a 4-step glyph badge. |
| **Decorative glow orbs** | Every secondary screen recreates a 240–320 dp radial-gradient `Box` at a corner. Different colors per screen but same recipe. | Extract `BackgroundGlow(accent, alignment, size)` and centralize. |

---

## 5. Accessibility

- **Color contrast (dark mode):**
  - `TextHint = 0xFF445577` on `BackgroundDeep = 0xFF0A0E1A` → contrast ≈ **4.0** — borderline AA for normal text, fails AAA. Used in many secondary labels.
  - `TextSecondary = 0xFF8899BB` on `BackgroundDeep` → contrast ≈ **6.4** — AA pass.
  - Gold-on-deep CTA (`GoldPrimary` on `BackgroundDeep`) → contrast ≈ **11.5** — strong.
- **Color contrast (light mode):**
  - `TextSecondaryLight = 0xFF556B8A` on `BackgroundLightDeep = 0xFFFAFBFC` → ≈ **5.4** — AA.
  - `TextHintLight = 0xFF8899BB` on `BackgroundLightDeep` → ≈ **2.9** — **fails AA** for normal text. Used in "Демо: demo / demo123" and unlock conditions.
  - `DiaryInkSecondary = 0xFF5C4A30` on `DiaryPage = 0xFFFFF8EC` → ≈ **8.1** — strong.
- **Touch targets:**
  - `IconButton`s with emoji glyphs default to 48 dp — OK.
  - `TextButton("← Назад")` is ~44 dp tall but the hit region is text-width only. Wrap in fixed-size box or use `IconButton`.
  - `TabButton` in CharacterSelection has `vertical = 8.dp` padding → ~36 dp height, **below the 48 dp recommendation**.
  - `MiniStat` tiles in `CompactStatsRow` are decorative and not tappable, so size is fine — but they look tappable.
- **Text readability:**
  - `DifficultyBadge` at **9 sp** is below the 12 sp comfort floor.
  - `MiniStat` value at **10 sp**, label-style chip at **10 sp** — same problem.
  - Diary `bodyMedium` at 14 sp / 26 sp lineHeight is great.
- **Screen reader:**
  - Emoji-as-icon means TalkBack reads "fire emoji" or nothing. Wrap each `IconButton { Text("📊") }` with `Modifier.semantics { contentDescription = "Финансы" }` — or, better, switch to vector icons with `contentDescription`.
- **Animations / motion:**
  - Several infinite animations (logo pulse, particles, glow alpha) ignore `Reduce Motion`. Read it via `AccessibilityManager` or fall back to a static state when the user opts out.

---

## What Works Well

- **Diary metaphor in `ChatScreen`.** Sticky-note choices, scene-tag pills over scene art, ruled paper, "пишет в дневник…" indicator, monospace financial report — this is a strong, defensible identity.
- **`AppColors` semantic abstraction** with separate Dark/Light palettes via `compositionLocalOf` is clean and KMP-safe.
- **Consistent enter animations** (fade + slide-in vertical with 60–80 ms staggered delays) across list screens give the app a coherent "things appear" feel.
- **`StatsPanelOverlay`** uses a real bottom-sheet pattern with backdrop alpha + drag-handle and animates the freedom % bar — the right mechanic, just needs hierarchy work.
- **MainMenu's `ActiveSessionCard`** as a "your run is waiting" affordance is exactly the right pattern; it just needs to be louder.
- **Scene tag system** (`scene_scam`, `scene_crisis`, etc.) cleanly maps to a `DrawableResource` per scene tag — easy to extend.
- **Era cards surface inflation + salary range up front** — concrete economic context that supports the learning goal.

---

## Priority Recommendations

The plan below is ordered by ROI: each later phase depends on the foundations laid in the earlier one.

### Phase 1 — Foundations (1–2 days, mostly mechanical)

The goal here is to stop bleeding consistency on every new screen.

1. **Create a `theme/Tokens.kt`** with three token objects:
   ```kotlin
   object Spacing {
       val xs = 4.dp;  val sm = 8.dp;  val md = 12.dp
       val lg = 16.dp; val xl = 24.dp; val xxl = 32.dp; val hero = 48.dp
   }
   object Radius {
       val xs = 4.dp;  val sm = 8.dp;  val md = 14.dp
       val lg = 20.dp; val xl = 28.dp
       val diaryEntry = RoundedCornerShape(4.dp, 12.dp, 12.dp, 4.dp)
       val diaryChoice = RoundedCornerShape(2.dp, 10.dp, 10.dp, 10.dp)
   }
   object Elevation { val card = 2.dp; val sheet = 16.dp; val topBar = 6.dp }
   object Motion {
       val fast = tween<Float>(150)
       val medium = tween<Float>(280, easing = FastOutSlowInEasing)
       val slow = tween<Float>(450, easing = FastOutSlowInEasing)
       val pressScale = spring<Float>(stiffness = Spring.StiffnessHigh)
   }
   ```
2. **Adopt Material Symbols** (`androidx.compose.material:material-icons-extended` is already on the classpath). Replace every emoji-as-button (top-bar icons, back button, restart, stats toggle, password toggle). Keep emoji for **content**: scene tags, character avatars, sticker choices, reactions.
3. **Extract `AppTopBar(title, subtitle, onBack, actions)`** — used by Era / Characters / CharacterSelection / Statistics. Handles `WindowInsets.statusBars` once. ChatScreen keeps its bespoke `DiaryTopBar`.
4. **Extract `AccentCard(accent, locked, onClick, content)`** — one composable to replace `MenuButton`, `EraCard`, `PredefinedCharacterCard`, `BundleCard`, `CharacterRosterCard`, `ActiveSessionCard`. Driven by `MutableInteractionSource` so press-scale resets correctly.
5. **Fix accessibility floor:** raise `DifficultyBadge`, `MiniStat`, `StatPill` text from 9–10 sp to 11–12 sp. Bump `TextHintLight` from `#8899BB` to `#6B7E9C` (gets to ~4.6 contrast).
6. **Remove `println("ERROR : $error")`** from `LoginScreen`. Add `Modifier.imePadding()` to its root.

### Phase 2 — Pick the brand voice (1 day decision + 2 days execution)

You currently have two visual languages. Decide between:

- **Option A — "Diary world wins."** Cream paper + handwritten display face + warm gold pull through every screen. Background becomes warm parchment in light mode, "burnt parchment" `#1E1A10` in dark mode. Glow orbs replaced by subtle paper-grain texture. Buttons become "stamped" or "stickered" instead of glassmorphic.
- **Option B — "Fintech world wins."** Dark glassmorphism stays for menus + login + stats. ChatScreen keeps its diary card *as a card on top of the dark canvas* (you already do this — the canvas is `colors.backgroundChat`). Diary is then a **content surface**, not the world.

I'd recommend **Option B** for an Android-engineering team that has to maintain it long-term: the dark glass world is easier to scale to dashboards, leaderboards, marketplace, future MMORPG-style screens. Diary stays scoped to chat — its role is to make decision moments feel weighty.

Either way: lock down a **type pair** (e.g., `Inter` + `Caveat` for Option B; `EB Garamond` + `Caveat` for Option A) and load it via `FontFamily(Font(Res.font.…))`. Single `FontFamily.Default` is leaving brand value on the table.

### Phase 3 — Hierarchy & flow polish (2–3 days)

1. **MainMenu** — Promote `ActiveSessionCard` into a hero with a filled gold "Продолжить" CTA inside it. Demote the four `MenuButton`s to outlined-style. If no active session: hide the hero, promote "Новая игра" to filled-gold.
2. **StatsPanelOverlay** — Reorder:
   - Top: Freedom % bar (the win condition).
   - Hero row: Net cash flow (full-width, with delta arrow vs last month).
   - Grid: capital / income / expenses / debt / investments (4-up, smaller).
   - Bottom: stress / knowledge / risk bars.
   Make the panel scrollable — on small phones the current content already overflows.
3. **ChatScreen top bar** — Move Restart into an overflow `DropdownMenu`, gated behind `AlertDialog("Сбросить прогресс?")`. Drop the duplicate date inside `DiaryEntryCard` (top bar already shows month/year).
4. **DiaryEntryCard ruled lines** — Either align lineSpacing to the text leading via `TextMeasurer` (so lines sit under baselines) or drop the lines entirely and lean on a paper-grain background image. Right now the lines visually distract.
5. **Game-over flow** — Add a 600 ms hold + result-themed VFX between the last bot message and `DiaryGameOverBar`:
   - `WEALTH` / `FREEDOM` → confetti or gold sparkle burst.
   - `STABILITY` → green check stamp.
   - `PAYCHECK_TO_PAYCHECK` → faded vignette.
   - `BANKRUPTCY` → red ink-bleed crossing the diary page.

### Phase 4 — Accessibility & i18n hardening (1–2 days)

1. **`Modifier.semantics { contentDescription = … }`** on every emoji-glyph button you didn't migrate to Material Symbols.
2. **Reduce-motion support** — read `AccessibilityManager.isReduceMotionEnabled` (Android) / `UIAccessibility.isReduceMotionEnabled` (iOS expect/actual). When true, swap `infiniteRepeatable` animations to a static target and shorten enter animations to a fade.
3. **Dynamic type** — none of your `fontSize = 14.sp` hardcoded calls scale with system font scaling because they bypass the typography scale. Replace `fontSize = 14.sp` with `style = MaterialTheme.typography.bodyMedium` everywhere.
4. **Localization** — UI is Russian-only (`Назад`, `Выбери эпоху`, etc.) hardcoded inline. Move to `composeApp/src/commonMain/composeResources/values/strings.xml`. KMP `compose-resources` handles this. Critical before Kazakh / English variants.
5. **Color-blind difficulty badges** — add a glyph step (●○○○ → ●●●●) so EASY/HARD don't rely on green/red alone.

### Phase 5 — Motion, sound, polish (open-ended)

1. **Haptics** on choice tap (`HapticFeedbackType.LongPress`) — fits the "decision weight" theme.
2. **Diary page-turn transition** between events (subtle skew + alpha) — sells the metaphor at the moment cost is highest.
3. **Sound design** — pen-on-paper for choices, coin-clink on monthly report, soft alarm on debt-crisis events. Optional, behind a setting.
4. **Empty-state illustrations** — replace `Text("Загрузка…")` and `EmptyStatsPlaceholder` with diary-themed sketches (a blank page, a pen).

---

## Top-3 Highest-ROI Changes

If you want a 1-PR-at-a-time sequence:

1. **`Tokens.kt` + `AppTopBar` + `AccentCard` extraction** (Phase 1, items 1, 3, 4). Doesn't change a single pixel for the user, but every later change becomes 5× cheaper, and visual drift between new screens stops cold. This is the change a senior reviewer would block all polish work on until merged.
2. **Replace emoji-as-icon with Material Symbols** (Phase 1, item 2). Single biggest perceived-quality jump per line of diff. Fixes accessibility, fixes cross-platform rendering, makes the chrome feel professional. Keep emojis where they're content.
3. **MainMenu hero + StatsPanel reorder** (Phase 3, items 1–2). These are the two screens players see most outside chat — re-weighting their hierarchy fixes the "all four buttons feel identical" + "I don't know what my key number is" problems in one go.

---

## Open Questions for the Build

These are decisions I can't make from the code alone — they shape Phase 2:

- Is the audience leaning **mobile-game player** (warm, cozy, playful — Option A) or **fintech-curious adult** (sleek, serious, trustworthy — Option B)? The current design straddles both.
- Will there be future screens that don't fit the diary metaphor — leaderboards, character marketplace, achievements gallery, multiplayer? If yes, Option B is the safer long-term bet.
- Is **Kazakh + English localization** in the roadmap? If yes, fix the hardcoded-string problem in Phase 1 instead of Phase 4 — touching every screen twice is wasteful.
- Is iOS parity equally weighted? Some of the visual-effect calls (`drawBehind`, `Brush.radialGradient`) are CMP-safe, but `EncryptedSharedPreferences`-style platform divergence will start showing up in animation timing. Worth running each screen through the iOS sim early.
