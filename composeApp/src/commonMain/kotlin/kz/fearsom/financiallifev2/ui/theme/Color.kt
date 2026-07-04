package kz.fearsom.financiallifev2.ui.theme

import androidx.compose.ui.graphics.Color

// ═══ Finance LifeLine · Redesign 2026-07 ══════════════════════════════════════
// Calm fintech palette: Indigo (#5C6BE6) + Emerald, linear icons, mono numerals.
// Source of truth: "Finance LifeLine — Редизайн.dc.html".

// ─── Brand Accents ────────────────────────────────────────────────────────────
val IndigoPrimary  = Color(0xFF5C6BE6)   // primary accent (buttons, player bubble, knowledge)
val IndigoLight    = Color(0xFF7C8AF0)
val IndigoDark     = Color(0xFF4553C4)
val PurpleGradient = Color(0xFF7C5CE6)   // second stop of the Asan avatar gradient

// Emerald — positive / safe
val EmeraldOnDark    = Color(0xFF2BC48A) // on dark surfaces
val EmeraldOnLight   = Color(0xFF16A974) // fills on light surfaces
val EmeraldTextLight = Color(0xFF0F8A5E) // text on light surfaces

// Coral — risk / danger
val CoralOnDark    = Color(0xFFFF6B5E)
val CoralSoftDark  = Color(0xFFFF8A7E)   // chip text on dark
val CoralOnLight   = Color(0xFFE5594E)
val CoralTextLight = Color(0xFFC8443A)   // chip/badge text on light

// Amber — stress / caution
val AmberOnDark    = Color(0xFFF0B94A)
val AmberOnLight   = Color(0xFFD9A441)
val AmberTextLight = Color(0xFFC28A1E)

// ─── Legacy accent aliases ────────────────────────────────────────────────────
// Old brand names kept so untouched screens retint automatically.
// TODO(redesign): mechanical rename Gold* → Indigo* across screens, then drop.
val GoldPrimary    = IndigoPrimary
val GoldLight      = IndigoLight
val GoldDark       = IndigoDark

val GreenSuccess   = EmeraldOnDark
val GreenMedium    = EmeraldOnLight

val RedDanger      = CoralOnDark
val RedLight       = CoralSoftDark

val BlueAccent     = IndigoPrimary
val PurpleAccent   = PurpleGradient

// ─── Dark Surfaces ────────────────────────────────────────────────────────────
val BackgroundDeep     = Color(0xFF0E1217)   // screen background
val BackgroundCard     = Color(0xFF161C24)   // cards, bubbles
val BackgroundElevated = Color(0xFF1F2630)   // icon containers, elevated chips
val BackgroundChat     = Color(0xFF0E1217)   // chat canvas
val BackgroundPanel    = Color(0xFF0B0E12)   // bottom action panel
val BackgroundSubCard  = Color(0xFF13181F)   // tertiary cells (income/expenses)

val DividerDark        = Color(0xFF1D242D)   // hairline separators
val BorderDark         = Color(0xFF232B35)   // card borders
val BorderStrongDark   = Color(0xFF2A3340)   // option/composer borders

val BubbleAiDark       = Color(0xFF171D2A)   // Asan AI bubble
val BubbleAiBorderDark = Color(0xFF2A3354)

val SurfaceGlass       = Color(0x1AFFFFFF)
val SurfaceGlassBorder = Color(0x33FFFFFF)

// ─── Dark Text ────────────────────────────────────────────────────────────────
val TextPrimary   = Color(0xFFEAEEF4)
val TextBody      = Color(0xFFD6DCE4)   // message body copy
val TextEmphasis  = Color(0xFFB7C0CD)   // card labels
val TextSecondary = Color(0xFF8B95A4)   // meta, subtitles
val TextHint      = Color(0xFF5C6677)   // placeholders, faint

// ─── Light Surfaces ───────────────────────────────────────────────────────────
val BackgroundLightDeep     = Color(0xFFF4F6F8)
val BackgroundLightCard     = Color(0xFFFFFFFF)
val BackgroundLightElevated = Color(0xFFEEF0F4)
val BackgroundLightChat     = Color(0xFFF4F6F8)
val BackgroundLightPanel    = Color(0xFFFFFFFF)
val BackgroundLightSubCard  = Color(0xFFFFFFFF)

val DividerLight            = Color(0xFFE7EAEF)
val BorderLight             = Color(0xFFE7EAEF)
val BorderStrongLight       = Color(0xFFE7EAEF)

val BubbleAiLight           = Color(0xFFFFFFFF)
val BubbleAiBorderLight     = Color(0xFFE7EAEF)

val SurfaceGlassLight       = Color(0x1A000000)
val SurfaceGlassBorderLight = Color(0x33000000)

// ─── Light Text ───────────────────────────────────────────────────────────────
val TextPrimaryLight   = Color(0xFF161B22)
val TextBodyLight      = Color(0xFF2B333F)
val TextEmphasisLight  = Color(0xFF3B4452)
val TextSecondaryLight = Color(0xFF5C6675)
val TextHintLight      = Color(0xFF98A1AF)

// ─── Message Bubbles (Dark) ───────────────────────────────────────────────────
val BubbleCharacter = BackgroundCard      // character bubble on dark
val BubblePlayer    = IndigoPrimary       // player bubble (both themes)
val BubbleReport    = BackgroundCard      // monthly report card
val BubbleSystem    = BackgroundCard      // date / system chip

// ─── Message Bubbles (Light) ──────────────────────────────────────────────────
val BubbleCharacterLight = Color(0xFFFFFFFF)
val BubblePlayerLight    = IndigoPrimary
val BubbleReportLight    = Color(0xFFFFFFFF)
val BubbleSystemLight    = Color(0xFFE7EAEF)

// ─── Stats (legacy names, dark-surface variants) ──────────────────────────────
// Prefer AppColors.accent* in redesigned components — these stay for old call sites.
val StatCapital   = EmeraldOnDark
val StatDebt      = CoralOnDark
val StatStress    = AmberOnDark
val StatKnowledge = IndigoPrimary
val StatRisk      = CoralOnDark
