# Typography System: Font Installation Guide

**Status**: Fonts.kt updated with proper `Font()` constructors. Fonts need to be manually downloaded and placed in the project.

**Directory**: `composeApp/src/commonMain/composeResources/font/`

---

## Step 1: Download Manrope

**Source**: [Google Fonts - Manrope](https://fonts.google.com/download?family=Manrope)

1. Go to https://fonts.google.com/download?family=Manrope
2. Extract the ZIP file
3. You'll see files like `Manrope-Regular.ttf`, `Manrope-Medium.ttf`, `Manrope-SemiBold.ttf`, `Manrope-Bold.ttf`

**Files to copy** (rename as shown):

| Source File | → | Target Name |
|---|---|---|
| Manrope-Regular.ttf | → | `manrope_regular.ttf` |
| Manrope-Medium.ttf | → | `manrope_medium.ttf` |
| Manrope-SemiBold.ttf | → | `manrope_semibold.ttf` |
| Manrope-Bold.ttf | → | `manrope_bold.ttf` |

---

## Step 2: Download Kalam

**Source**: [Google Fonts - Kalam](https://fonts.google.com/download?family=Kalam)

1. Go to https://fonts.google.com/download?family=Kalam
2. Extract the ZIP file
3. You'll see files like `Kalam-Regular.ttf`, `Kalam-Bold.ttf`

**Files to copy** (rename as shown):

| Source File | → | Target Name |
|---|---|---|
| Kalam-Regular.ttf | → | `kalam_regular.ttf` |
| Kalam-Bold.ttf | → | `kalam_bold.ttf` |

---

## Step 3: Place Fonts in Project

Copy all renamed files to:
```
composeApp/src/commonMain/composeResources/font/
```

**Final directory structure:**
```
composeApp/src/commonMain/composeResources/
├── drawable/
│   └── (existing scene SVGs)
└── font/
    ├── manrope_regular.ttf      ← 400 weight
    ├── manrope_medium.ttf       ← 500 weight
    ├── manrope_semibold.ttf     ← 600 weight
    ├── manrope_bold.ttf         ← 700 weight
    ├── kalam_regular.ttf        ← 400 weight
    └── kalam_bold.ttf           ← 700 weight
```

---

## Step 4: Gradle Resource Generation

Once fonts are in place, Compose Multiplatform's Gradle plugin will automatically generate resource references under:
```
financiallifev2.composeapp.generated.resources.Res.font.*
```

These are already imported in `Fonts.kt`:
```kotlin
import financiallifev2.composeapp.generated.resources.manrope_regular
import financiallifev2.composeapp.generated.resources.manrope_medium
import financiallifev2.composeapp.generated.resources.manrope_semibold
import financiallifev2.composeapp.generated.resources.manrope_bold
import financiallifev2.composeapp.generated.resources.kalam_regular
import financiallifev2.composeapp.generated.resources.kalam_bold
```

---

## Step 5: Build and Verify

Run:
```bash
./gradlew clean build
```

If the build succeeds without resource-not-found errors, fonts are correctly bundled.

**Test on emulator/simulator:**
- Launch the app
- Navigate to game screens (MainMenu, ChatScreen, StatsPanelOverlay)
- Verify typography uses Manrope (UI text) and Kalam (diary text)
- Check that text is rendered smoothly (no fallbacks to system font)

---

## Font Usage in Code

Already configured in `Fonts.kt`:

### Manrope (Professional UI)
- All app text: buttons, labels, stats, headers, body text
- Used by `AppTypography` Material Design 3 scale
- Referenced via `ManropeFontFamily`

### Kalam (Handwritten Diary)
- Character dialogue in ChatScreen
- Diary entries and headers
- Scene labels in game narrative
- "пишет в дневник..." indicator
- Referenced via `KalamFontFamily` and `DiaryTextStyle`/`DiaryHeaderStyle`

---

## Troubleshooting

### Build error: "Cannot find resource Res.font.manrope_regular"
- **Cause**: Font files not placed in `composeApp/src/commonMain/composeResources/font/`
- **Fix**: Run `./gradlew clean` and place fonts, then rebuild

### Fonts still use system default
- **Cause**: Gradle cache not cleared
- **Fix**: Run `./gradlew clean` and rebuild

### Font file names don't match
- **Cause**: Downloaded fonts have different names (e.g., `Manrope-Regular.ttf`)
- **Fix**: Rename files to lowercase with underscores: `manrope_regular.ttf`

---

## Code Changes Made (2026-04-28)

### Fonts.kt Updates

**Added imports:**
```kotlin
import androidx.compose.ui.text.font.Font
import financiallifev2.composeapp.generated.resources.Res
import financiallifev2.composeapp.generated.resources.manrope_*
import financiallifev2.composeapp.generated.resources.kalam_*
```

**ManropeFontFamily (before):**
```kotlin
val ManropeFontFamily: FontFamily = FontFamily.Default  // TODO: placeholder
```

**ManropeFontFamily (after):**
```kotlin
val ManropeFontFamily: FontFamily = FontFamily(
    Font(Res.font.manrope_regular, FontWeight.Normal),
    Font(Res.font.manrope_medium, FontWeight.Medium),
    Font(Res.font.manrope_semibold, FontWeight.SemiBold),
    Font(Res.font.manrope_bold, FontWeight.Bold)
)
```

**KalamFontFamily (before):**
```kotlin
val KalamFontFamily: FontFamily = FontFamily.Default  // TODO: placeholder
```

**KalamFontFamily (after):**
```kotlin
val KalamFontFamily: FontFamily = FontFamily(
    Font(Res.font.kalam_regular, FontWeight.Normal),
    Font(Res.font.kalam_bold, FontWeight.Bold)
)
```

All `AppTypography` scale references and `DiaryTextStyle`/`DiaryHeaderStyle` already reference the correct font families.

---

## Next Phase: Diary Text Application

Once fonts are bundled and verified, next task is to apply `KalamFontFamily` to diary-specific text in:
- `ChatScreen.kt`: Character message text, choice labels
- Character names and "пишет в дневник..." indicator
- Scene headers in game narrative

This will complete the visual distinction between fintech UI (Manrope) and diary flavor (Kalam).
