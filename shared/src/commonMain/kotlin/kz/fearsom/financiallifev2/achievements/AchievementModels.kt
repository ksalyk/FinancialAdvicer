package kz.fearsom.financiallifev2.achievements

import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.model.GameState
import kz.fearsom.financiallifev2.model.MessageSender

// ════════════════════════════════════════════════════════════════════
//  ACHIEVEMENTS — models
//
//  Definitions live in [AchievementCatalog] (code, not DB) and reference
//  localized content via i18n string keys ("ach_*"), same convention as
//  scenario content. The server stores only per-user unlock state.
// ════════════════════════════════════════════════════════════════════

enum class AchievementRarity { COMMON, RARE, LEGENDARY }

enum class AchievementKind {
    /** Gameplay milestone (emergency fund, debt-free, …). */
    GAME,
    /** Scam-collection entry — unlocks a historical dossier. */
    SCAM
}

/** One entry on a scam dossier timeline (origin / variant / modern day). */
data class ScamTimelineEntry(
    val yearKey: String,
    val titleKey: String,
    val textKey: String
)

/**
 * Historical dossier shown for an unlocked SCAM achievement:
 * origin → variants → modern form, plus red-flag recognition signs.
 */
data class ScamDossier(
    val ageKey: String,
    val origin: ScamTimelineEntry,
    val variants: List<ScamTimelineEntry>,
    val modern: ScamTimelineEntry,
    val signKeys: List<String>
)

/**
 * Unlock rule evaluated against the live [GameState] + per-session facts.
 *
 * Kept separate from the engine's event [kz.fearsom.financiallifev2.model.Condition]
 * on purpose: achievement rules need cross-turn memory (ever had debt, stress
 * history through a crisis) that event conditions don't model.
 */
sealed class AchievementCondition {

    /** Unlocks when ANY of [flags] is present in PlayerState.flags. */
    data class AnyFlag(val flags: Set<String>) : AchievementCondition()

    /** Unlocks after the first monthly report lands in the chat. */
    data object FirstMonthlyReport : AchievementCondition()

    /** Unlocks when liquid capital covers [months] months of expenses. */
    data class EmergencyFund(val months: Int = 6) : AchievementCondition()

    /** Unlocks when debt hits zero after the player has carried debt. */
    data object DebtFree : AchievementCondition()

    /**
     * Unlocks when [months] months pass after a crisis event while stress
     * never reaches [stressBelow].
     */
    data class CalmThroughCrisis(
        val months: Int = 12,
        val stressBelow: Int = 30
    ) : AchievementCondition()

    /** Unlocks when the character's story reaches any ending. */
    data object StoryCompleted : AchievementCondition()
}

data class AchievementDefinition(
    val id: String,
    val kind: AchievementKind,
    val emoji: String,
    val rarity: AchievementRarity,
    val titleKey: String,
    /** GAME only — what to do. */
    val descKey: String? = null,
    /** GAME only — how to approach it. */
    val hintKey: String? = null,
    /** SCAM only — the unlockable dossier. */
    val dossier: ScamDossier? = null,
    val condition: AchievementCondition
)

// ════════════════════════════════════════════════════════════════════
//  SESSION FACTS — cross-turn memory needed by unlock rules
// ════════════════════════════════════════════════════════════════════

/**
 * Immutable per-session accumulator. Feed every [GameState] emission through
 * [update]; conditions read the resulting facts.
 *
 * Lifetime: one game session (reset on new game / continue). Not persisted —
 * losing it mid-session only delays DebtFree / CalmThroughCrisis detection,
 * never falsely unlocks.
 */
data class AchievementSessionFacts(
    val everHadDebt: Boolean = false,
    /** Absolute month when the most recent crisis window opened. */
    val crisisStartAbsMonth: Int? = null,
    /** Chat message id of the crisis that opened the window (dedup guard). */
    val crisisMessageId: String? = null,
    /** Max stress observed since the crisis window opened. */
    val maxStressSinceCrisis: Int = 0
) {
    fun update(state: GameState): AchievementSessionFacts {
        val ps = state.playerState

        val latestCrisis = state.messages.lastOrNull { it.sceneTag == SCENE_TAG_CRISIS }
        val isNewCrisis = latestCrisis != null && latestCrisis.id != crisisMessageId

        return copy(
            everHadDebt = everHadDebt || ps.debt > 0L,
            crisisStartAbsMonth = when {
                isNewCrisis -> latestCrisis?.sourcePlayerState?.absoluteMonth ?: ps.absoluteMonth
                else        -> crisisStartAbsMonth
            },
            crisisMessageId = latestCrisis?.id ?: crisisMessageId,
            maxStressSinceCrisis = when {
                isNewCrisis                  -> ps.stress
                crisisStartAbsMonth != null  -> maxOf(maxStressSinceCrisis, ps.stress)
                else                         -> maxStressSinceCrisis
            }
        )
    }

    companion object {
        const val SCENE_TAG_CRISIS = "crisis"
    }
}

// ════════════════════════════════════════════════════════════════════
//  EVALUATOR — pure unlock check
// ════════════════════════════════════════════════════════════════════

object AchievementEvaluator {

    /**
     * Returns ids from [AchievementCatalog] whose conditions hold for
     * ([state], [facts]) and are not already in [alreadyUnlocked].
     */
    fun newlyUnlocked(
        state: GameState,
        facts: AchievementSessionFacts,
        alreadyUnlocked: Set<String>
    ): Set<String> =
        AchievementCatalog.all
            .asSequence()
            .filter { it.id !in alreadyUnlocked }
            .filter { isSatisfied(it.condition, state, facts) }
            .map { it.id }
            .toSet()

    fun isSatisfied(
        condition: AchievementCondition,
        state: GameState,
        facts: AchievementSessionFacts
    ): Boolean {
        val ps = state.playerState
        return when (condition) {
            is AchievementCondition.AnyFlag ->
                condition.flags.any { it in ps.flags }

            is AchievementCondition.FirstMonthlyReport ->
                state.messages.any { it.sender == MessageSender.MONTHLY_REPORT }

            is AchievementCondition.EmergencyFund ->
                ps.expenses > 0L && ps.capital >= ps.expenses * condition.months

            is AchievementCondition.DebtFree ->
                facts.everHadDebt && ps.debt == 0L

            is AchievementCondition.CalmThroughCrisis -> {
                val start = facts.crisisStartAbsMonth
                start != null &&
                    ps.absoluteMonth - start >= condition.months &&
                    facts.maxStressSinceCrisis < condition.stressBelow &&
                    ps.stress < condition.stressBelow
            }

            is AchievementCondition.StoryCompleted ->
                state.gameOver && state.endingType != null
        }
    }
}

// ════════════════════════════════════════════════════════════════════
//  SYNC DTOs — shared by client (composeApp) and server
// ════════════════════════════════════════════════════════════════════

@Serializable
data class AchievementUnlockDto(
    val achievementId: String,
    val unlockedAt: Long,
    /** Character whose playthrough unlocked it — drives "Открыта · <name>". */
    val sourceCharacterId: String? = null,
    val sourceEraId: String? = null
)

@Serializable
data class UnlockAchievementsRequest(val unlocks: List<AchievementUnlockDto>)

@Serializable
data class UserAchievementsResponse(val unlocks: List<AchievementUnlockDto>)

/** Response of POST /achievements/unlock — insert count + full merged list. */
@Serializable
data class UnlockAchievementsResponse(
    val inserted: Int,
    val unlocks: List<AchievementUnlockDto>
)

/** [vote] is "up", "down", or null to clear a previous vote. */
@Serializable
data class AchievementFeedbackRequest(val vote: String? = null)

object AchievementVote {
    const val UP = "up"
    const val DOWN = "down"
    val VALID = setOf(UP, DOWN)
}
