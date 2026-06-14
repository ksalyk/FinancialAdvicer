package kz.fearsom.financiallifev2.ui.components

import androidx.compose.ui.graphics.Color
import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.Difficulty
import kz.fearsom.financiallifev2.model.UnlockCondition
import kz.fearsom.financiallifev2.model.emoji
import kz.fearsom.financiallifev2.model.label
import kz.fearsom.financiallifev2.ui.theme.GoldPrimary
import kz.fearsom.financiallifev2.ui.theme.GreenSuccess
import kz.fearsom.financiallifev2.ui.theme.PurpleAccent
import kz.fearsom.financiallifev2.ui.theme.RedDanger

internal fun Long.shortFormat(): String = when {
    this >= 1_000_000L -> "${this / 1_000_000}M₸"
    this >= 1_000L -> "${this / 1_000}k₸"
    else -> "$this₸"
}

internal fun Difficulty.difficultyColor(): Color = when (this) {
    Difficulty.EASY -> GreenSuccess
    Difficulty.MEDIUM -> GoldPrimary
    Difficulty.HARD -> RedDanger
    Difficulty.NIGHTMARE -> PurpleAccent
}
internal fun UnlockCondition.unlockHint(): String = when (this) {
    is UnlockCondition.FinishGameWith -> "${Strings.uiCharSelUnlockComplete} ${ending.emoji()} ${ending.label()}"
    is UnlockCondition.ReachCapital   -> "${Strings.uiCharSelUnlockReach} ${amount.shortFormat()}"
    is UnlockCondition.PlayEra        -> "${Strings.uiCharSelUnlockEra} $eraId"
    is UnlockCondition.CompleteGames  -> "${Strings.uiCharSelUnlockCompleteN} $count"
}

fun Difficulty.label(): String = when (this) {
    Difficulty.EASY      -> Strings.uiCharSelDiffEasy
    Difficulty.MEDIUM    -> Strings.uiCharSelDiffMedium
    Difficulty.HARD      -> Strings.uiCharSelDiffHard
    Difficulty.NIGHTMARE -> Strings.uiCharSelDiffNightmare
}

fun Long.longFormat(): String = when {
    this >= 1_000_000L -> "${this / 1_000_000}.${(this % 1_000_000) / 100_000}M ₸"
    this >= 1_000L     -> "${this / 1_000}k ₸"
    else               -> "$this ₸"
}