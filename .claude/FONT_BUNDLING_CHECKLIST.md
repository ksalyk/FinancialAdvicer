# Font Bundling Checklist
**Goal**: Get Manrope + Kalam fonts rendering in the app  
**Timeline**: ~30 minutes total  
**Status**: Ready to execute

---

## 📋 Step 1: Download Fonts (5 minutes)

### Download Manrope
- [ ] Open browser → https://fonts.google.com/download?family=Manrope
- [ ] Click download (usually auto-starts)
- [ ] Extract `Manrope.zip` to your Downloads folder
- [ ] Look for these 4 files:
  - [ ] `Manrope-Regular.ttf`
  - [ ] `Manrope-Medium.ttf`
  - [ ] `Manrope-SemiBold.ttf`
  - [ ] `Manrope-Bold.ttf`

### Download Kalam
- [ ] Open browser → https://fonts.google.com/download?family=Kalam
- [ ] Click download (usually auto-starts)
- [ ] Extract `Kalam.zip` to your Downloads folder
- [ ] Look for these 2 files:
  - [ ] `Kalam-Regular.ttf`
  - [ ] `Kalam-Bold.ttf`

### Rename All Files (critical!)
Rename each file to use lowercase + underscores. Use Finder or command line:

```bash
# Navigate to where you extracted the fonts
cd ~/Downloads

# Rename Manrope files
mv Manrope-Regular.ttf manrope_regular.ttf
mv Manrope-Medium.ttf manrope_medium.ttf
mv Manrope-SemiBold.ttf manrope_semibold.ttf
mv Manrope-Bold.ttf manrope_bold.ttf

# Rename Kalam files
mv Kalam-Regular.ttf kalam_regular.ttf
mv Kalam-Bold.ttf kalam_bold.ttf
```

Or manually in Finder (slower but works):
1. Right-click each file → Rename
2. Use exact names above

---

## 📂 Step 2: Place Fonts in Project (2 minutes)

### Copy to Project Directory
```bash
# Copy all 6 fonts to the project
cp ~/Downloads/manrope_*.ttf ~/AndroidStudioProjects/FinancialLifeV2/composeApp/src/commonMain/composeResources/font/
cp ~/Downloads/kalam_*.ttf ~/AndroidStudioProjects/FinancialLifeV2/composeApp/src/commonMain/composeResources/font/
```

### Or via Finder (slower):
1. Open Finder
2. Navigate to: `FinancialLifeV2/composeApp/src/commonMain/composeResources/font/`
3. Drag all 6 renamed TTF files into this folder

### Verify Files Are in Place
```bash
ls -la ~/AndroidStudioProjects/FinancialLifeV2/composeApp/src/commonMain/composeResources/font/
```

Should show:
```
manrope_regular.ttf
manrope_medium.ttf
manrope_semibold.ttf
manrope_bold.ttf
kalam_regular.ttf
kalam_bold.ttf
```

---

## 🔨 Step 3: Build Project (10-20 minutes)

### Clean Gradle Cache
```bash
cd ~/AndroidStudioProjects/FinancialLifeV2
./gradlew clean
```
Wait for completion (~30 seconds). You'll see:
```
BUILD SUCCESSFUL in XXs
```

### Build Project
```bash
./gradlew build
```

Watch the output. Should see:
- ✅ `generateCommonMainResources` task runs
- ✅ No errors about missing `Res.font.*`
- ✅ Final message: `BUILD SUCCESSFUL`

### If Build Fails ❌
Check the error message:
- **"Cannot find Res.font.manrope_regular"** → Font file not placed correctly
- **"Failed to resolve"** → Filename spelling issue (use lowercase + underscores)
- **Other errors** → Try `./gradlew clean` again and rebuild

**Troubleshooting**:
```bash
# Check files are actually there
ls -la composeApp/src/commonMain/composeResources/font/

# If files look correct but build still fails
./gradlew clean --build-cache --configure-on-demand
./gradlew build
```

---

## 🧪 Step 4: Test on Device (10 minutes)

### Launch Android Emulator
```bash
# If you have an emulator set up
./gradlew :composeApp:installDebug

# Or use Android Studio:
# 1. Open Android Studio
# 2. Device Manager (left sidebar)
# 3. Click "Launch" on any emulator
# 4. Wait for emulator to boot
```

### Install & Run App
```bash
./gradlew :composeApp:run
```

Wait for app to launch on emulator.

### Visual Verification Checklist

#### MainMenu (check button fonts)
- [ ] **Continue** button text looks: **Warm, smooth sans-serif** (NOT handwritten) ← Manrope ✓
- [ ] **New Game** button text looks: **Same smooth sans-serif** ← Manrope ✓

#### ChatScreen (start a game and check message fonts)
- [ ] **Character messages**: Organic, flowing handwriting (NOT clean sans-serif) ← Kalam ✓
- [ ] **Choice label "✍️ Я решил:"**: Handwritten header style ← Kalam ✓
- [ ] **Player choice text**: Handwritten (matches character messages) ← Kalam ✓
- [ ] **"пишет в дневник..."**: Handwritten, italic ← Kalam ✓
- [ ] **Scene labels between messages**: Handwritten headers ← Kalam ✓

#### TopBar (check app chrome)
- [ ] **Title "Дневник · Character"**: Clean sans-serif ← Manrope ✓
- [ ] **Subtitle "Title · Month Year"**: Clean sans-serif ← Manrope ✓

#### Stats Panel (tap 📊 icon)
- [ ] **"🎯 Финансовая свобода"**: Clean sans-serif ← Manrope ✓
- [ ] **"💸 Денежный поток"**: Clean sans-serif ← Manrope ✓
- [ ] **All numbers**: Clean and professional ← Manrope ✓

### Expected Rendering

**What Kalam (handwritten) looks like:**
- Organic curves, slightly informal
- Individual characters feel personal
- NOT precise or mechanical
- Good for narrative/diary text

**What Manrope (professional) looks like:**
- Smooth, warm sans-serif
- Clean and legible
- Consistent character spacing
- Good for UI/buttons/labels

### If Fonts Don't Appear ❌

**Fonts look like system default (generic)?**
1. Restart emulator: Close and relaunch
2. Clear app cache: Settings → Apps → Financial Life Line → Clear Cache
3. Rebuild: `./gradlew clean :composeApp:assembleDebug`
4. Reinstall: Uninstall app from emulator, then run `./gradlew :composeApp:run`

**Only some text uses fonts?**
- Rebuild (`./gradlew build`) and restart emulator
- May need to do `./gradlew clean` and rebuild from scratch

**Text is blurry or pixelated?**
- Scroll chat up/down to re-render
- Usually resolves on second render

**Russian characters are missing?**
- Check you downloaded full TTF files (not subset versions)
- Verify files placed in: `composeApp/src/commonMain/composeResources/font/`

---

## ✅ Success Checklist

Once all verifications pass:

- [ ] All 6 font files downloaded and renamed correctly
- [ ] Files placed in: `composeApp/src/commonMain/composeResources/font/`
- [ ] `./gradlew build` completes with no errors
- [ ] App launches on emulator without crashes
- [ ] Manrope appears in UI (TopBar, buttons, stats) — warm, smooth
- [ ] Kalam appears in game (messages, choices, labels) — handwritten
- [ ] No system font fallback
- [ ] Russian text renders correctly in both fonts
- [ ] Font weights are distinct (Bold, Medium, Regular clearly different)

---

## 🎉 You're Done!

Design System Complete:
- ✅ Phase 1: Design tokens
- ✅ Phase 2: Brand voice (Manrope + Kalam)
- ✅ Phase 3: Screen hierarchy & VFX
- ✅ Phase 4: Typography system
- ✅ Phase 5: Apply Kalam to diary
- ✅ Font bundling & testing

The app now has professional fintech UI (Manrope) + intimate handwritten diary (Kalam). Ready for next phases! 🚀

---

## Quick Reference: File Locations

**Where fonts go:**
```
~/AndroidStudioProjects/FinancialLifeV2/composeApp/src/commonMain/composeResources/font/
```

**Modified source files:**
- `composeApp/src/commonMain/kotlin/kz/fearsom/financiallifev2/ui/theme/Fonts.kt`
- `composeApp/src/commonMain/kotlin/kz/fearsom/financiallifev2/ui/screens/ChatScreen.kt`
- `composeApp/src/commonMain/kotlin/kz/fearsom/financiallifev2/ui/screens/MainMenuScreen.kt`
- `composeApp/src/commonMain/kotlin/kz/fearsom/financiallifev2/ui/components/StatsPanelOverlay.kt`

**Download links:**
- Manrope: https://fonts.google.com/download?family=Manrope
- Kalam: https://fonts.google.com/download?family=Kalam
