package kz.fearsom.financiallifev2.achievements

import kotlinx.serialization.SerialName
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

@Serializable
enum class AchievementRarity { COMMON, RARE, LEGENDARY }

@Serializable
enum class AchievementKind {
    /** Gameplay milestone (emergency fund, debt-free, …). */
    GAME,
    /** Scam-collection entry — unlocks a historical dossier. */
    SCAM
}

/**
 * Inline localized text for DB-authored achievements. Empty strings mean
 * "no translation" — display code falls back ru → en → kk in that order
 * (mirrors Strings.get() where ru is the source of truth).
 */
@Serializable
data class LocalizedText(
    val ru: String = "",
    val en: String = "",
    val kk: String = ""
) {
    fun resolve(locale: String): String = when (locale) {
        "en" -> en.ifBlank { ru }
        "kk" -> kk.ifBlank { ru }
        else -> ru
    }

    val isBlank: Boolean get() = ru.isBlank() && en.isBlank() && kk.isBlank()
}

/** One entry on a scam dossier timeline (origin / variant / modern day). */
@Serializable
data class ScamTimelineEntry(
    val yearKey: String,
    val titleKey: String,
    val textKey: String
)

/**
 * Historical dossier shown for an unlocked SCAM achievement:
 * origin → variants → modern form, plus red-flag recognition signs.
 */
@Serializable
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
@Serializable
sealed class AchievementCondition {

    /** Unlocks when ANY of [flags] is present in PlayerState.flags. */
    @Serializable
    @SerialName("anyFlag")
    data class AnyFlag(val flags: Set<String>) : AchievementCondition()

    /** Unlocks after the first monthly report lands in the chat. */
    @Serializable
    @SerialName("firstMonthlyReport")
    data object FirstMonthlyReport : AchievementCondition()

    /** Unlocks when liquid capital covers [months] months of expenses. */
    @Serializable
    @SerialName("emergencyFund")
    data class EmergencyFund(val months: Int = 6) : AchievementCondition()

    /** Unlocks when debt hits zero after the player has carried debt. */
    @Serializable
    @SerialName("debtFree")
    data object DebtFree : AchievementCondition()

    /**
     * Unlocks when [months] months pass after a crisis event while stress
     * never reaches [stressBelow].
     */
    @Serializable
    @SerialName("calmThroughCrisis")
    data class CalmThroughCrisis(
        val months: Int = 12,
        val stressBelow: Int = 30
    ) : AchievementCondition()

    /** Unlocks when the character's story reaches any ending. */
    @Serializable
    @SerialName("storyCompleted")
    data object StoryCompleted : AchievementCondition()
}

/**
 * A single achievement definition.
 *
 * Two content sources coexist:
 * - Code-seeded entries (the original catalog) reference i18n keys
 *   ([titleKey]/[descKey]/[hintKey]/[dossier] keys) resolved via Strings.
 * - DB-authored entries (created in the admin panel) carry inline
 *   [titleText]/[descText]/[hintText] — no compile-time keys exist for them.
 *
 * Resolution rule for display code: inline text wins when present and
 * non-blank; otherwise fall back to the i18n key.
 */
@Serializable
data class AchievementDefinition(
    val id: String,
    val kind: AchievementKind,
    val emoji: String,
    val rarity: AchievementRarity,
    val titleKey: String = "",
    /** GAME only — what to do. */
    val descKey: String? = null,
    /** GAME only — how to approach it. */
    val hintKey: String? = null,
    /** SCAM only — the unlockable dossier. */
    val dossier: ScamDossier? = null,
    val condition: AchievementCondition,
    /** Inline texts for DB-authored achievements (win over keys when set). */
    val titleText: LocalizedText? = null,
    val descText: LocalizedText? = null,
    val hintText: LocalizedText? = null
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
     * Returns ids from [catalog] whose conditions hold for ([state], [facts]) and
     * are not already in [alreadyUnlocked].
     *
     * [catalog] defaults to the compile-time [AchievementCatalog]; the client passes
     * the server's active catalog (admin edits + custom entries, minus deactivated
     * ones) so admin changes have real effect. Param is last + defaulted to keep
     * existing 3-arg callers (tests) source-compatible.
     */
    fun newlyUnlocked(
        state: GameState,
        facts: AchievementSessionFacts,
        alreadyUnlocked: Set<String>,
        catalog: List<AchievementDefinition> = AchievementCatalog.all
    ): Set<String> =
        catalog
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
