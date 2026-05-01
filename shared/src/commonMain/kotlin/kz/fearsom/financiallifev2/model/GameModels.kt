package kz.fearsom.financiallifev2.model

import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.i18n.Strings

// ════════════════════════════════════════════════════════════════════
//  ERA
// ════════════════════════════════════════════════════════════════════

@Serializable
data class Era(
    val id: String,
    val name: String,
    val description: String,
    val startYear: Int,
    val endYear: Int,
    val baseInflationRate: Double,
    val baseSalaryMin: Long,
    val baseSalaryMax: Long,
    val currencySymbol: String = "₸",
    val availableCharacterIds: List<String>,
    val keyEconomicEvents: List<String>,
    val emoji: String,
    val isLocked: Boolean = false
)

// ════════════════════════════════════════════════════════════════════
//  CHARACTER STATS
// ════════════════════════════════════════════════════════════════════

@Serializable
data class CharacterStats(
    val capital: Long,
    val income: Long,
    val debt: Long,
    val monthlyExpenses: Long,
    val stress: Int,
    val financialKnowledge: Int,
    val riskLevel: Int
)

/** Convert CharacterStats into a PlayerState for the game engine. */
fun CharacterStats.toPlayerState(
    year: Int = 2024,
    month: Int = 1,
    characterId: String = "",
    eraId: String = "",
    currency: CurrencyCode = CurrencyCode.KZT
): PlayerState {
    val debtPayment = if (debt > 0) (debt / 36).coerceAtLeast(5_000L) else 0L
    return PlayerState(
        capital            = capital,
        income             = income,
        expenses           = monthlyExpenses,
        debt               = debt,
        debtPaymentMonthly = debtPayment,
        investments        = 0L,
        stress             = stress,
        financialKnowledge = financialKnowledge,
        riskLevel          = riskLevel,
        month              = month,
        year               = year,
        characterId        = characterId,
        eraId              = eraId,
        currency           = currency
    )
}

// ════════════════════════════════════════════════════════════════════
//  DIFFICULTY + CHARACTER TYPE
// ════════════════════════════════════════════════════════════════════

@Serializable
enum class Difficulty { EASY, MEDIUM, HARD, NIGHTMARE }

@Serializable
enum class CharacterType { PREDEFINED, CUSTOM_BUNDLE }

// ════════════════════════════════════════════════════════════════════
//  UNLOCK CONDITION
// ════════════════════════════════════════════════════════════════════

@Serializable
sealed class UnlockCondition {
    @Serializable
    data class FinishGameWith(val ending: GameEnding) : UnlockCondition()

    @Serializable
    data class ReachCapital(val amount: Long) : UnlockCondition()

    @Serializable
    data class PlayEra(val eraId: String) : UnlockCondition()

    @Serializable
    data class CompleteGames(val count: Int) : UnlockCondition()
}

// ════════════════════════════════════════════════════════════════════
//  GAME ENDING (session-level, superset of EndingType)
// ════════════════════════════════════════════════════════════════════

@Serializable
enum class GameEnding {
    BANKRUPTCY,
    PAYCHECK_TO_PAYCHECK,
    FINANCIAL_STABILITY,
    FINANCIAL_FREEDOM,
    WEALTH,
    PRISON
}

fun EndingType.toGameEnding(): GameEnding = when (this) {
    EndingType.BANKRUPTCY           -> GameEnding.BANKRUPTCY
    EndingType.PAYCHECK_TO_PAYCHECK -> GameEnding.PAYCHECK_TO_PAYCHECK
    EndingType.FINANCIAL_STABILITY  -> GameEnding.FINANCIAL_STABILITY
    EndingType.FINANCIAL_FREEDOM    -> GameEnding.FINANCIAL_FREEDOM
    EndingType.WEALTH               -> GameEnding.WEALTH
}

fun GameEnding.emoji(): String = when (this) {
    GameEnding.BANKRUPTCY           -> "💔"
    GameEnding.PAYCHECK_TO_PAYCHECK -> "😰"
    GameEnding.FINANCIAL_STABILITY  -> "😊"
    GameEnding.FINANCIAL_FREEDOM    -> "🎯"
    GameEnding.WEALTH               -> "🤑"
    GameEnding.PRISON               -> "⛓️"
}

fun GameEnding.label(): String = when (this) {
    GameEnding.BANKRUPTCY           -> Strings.endingBankruptcy
    GameEnding.PAYCHECK_TO_PAYCHECK -> Strings.endingPaycheck
    GameEnding.FINANCIAL_STABILITY  -> Strings.endingStability
    GameEnding.FINANCIAL_FREEDOM    -> Strings.endingFreedom
    GameEnding.WEALTH               -> Strings.endingWealth
    GameEnding.PRISON               -> Strings.endingPrison
}

// ════════════════════════════════════════════════════════════════════
//  PREDEFINED CHARACTER
// ════════════════════════════════════════════════════════════════════

@Serializable
data class PredefinedCharacter(
    val id: String,
    val name: String,
    val age: Int,
    val profession: String,
    val emoji: String,
    val backstory: String,
    val personality: String,
    val compatibleEraIds: List<String>,
    val initialStats: CharacterStats,
    val uniqueEventIds: List<String> = emptyList(),
    val unlockCondition: UnlockCondition? = null,
    val isUnlocked: Boolean = true,
    val difficulty: Difficulty = Difficulty.EASY
)

// ════════════════════════════════════════════════════════════════════
//  CHARACTER BUNDLE (custom/preset — immutable)
// ════════════════════════════════════════════════════════════════════

@Serializable
data class CharacterBundle(
    val id: String,
    val label: String,
    val description: String,
    val emoji: String,
    val compatibleEraIds: List<String>,
    val profession: String,
    val stats: CharacterStats,
    val traits: List<String>,
    val difficulty: Difficulty,
    val isLocked: Boolean = false,
    val unlockCondition: UnlockCondition? = null
)

// ════════════════════════════════════════════════════════════════════
//  GAME SESSION
// ════════════════════════════════════════════════════════════════════

@Serializable
enum class SessionStatus { ACTIVE, COMPLETED, ABANDONED }

@Serializable
data class GameSession(
    val id: String,
    val userId: String,
    val eraId: String,
    val eraName: String,
    val characterType: CharacterType,
    val characterId: String,
    val characterName: String,
    val characterEmoji: String,
    val characterTitle: String,
    val initialStats: CharacterStats,
    val currentStats: CharacterStats,
    val currentGameYear: Int,
    val currentGameMonth: Int,
    val startedAt: Long = 0L,
    val lastPlayedAt: Long = 0L,
    val status: SessionStatus = SessionStatus.ACTIVE,
    val ending: GameEnding? = null
)

// ════════════════════════════════════════════════════════════════════
//  STATISTICS MODELS
// ════════════════════════════════════════════════════════════════════

data class QuickStats(
    val totalGames: Int,
    val bestEnding: GameEnding?,
    val lastPlayedCharacter: String?
)

data class PlayerStatistics(
    val totalGamesPlayed: Int,
    val gamesCompleted: Int,
    val bestEnding: GameEnding?,
    val averageCapitalAtEnd: Long,
    val mostPlayedEraId: String?,
    val endingDistribution: Map<GameEnding, Int>,
    val perCharacter: List<CharacterStatistics>,
    val perEra: List<EraStatistics>
)

data class CharacterStatistics(
    val characterId: String,
    val characterName: String,
    val characterEmoji: String,
    val timesPlayed: Int,
    val bestEnding: GameEnding?,
    val averageCapital: Long
)

data class EraStatistics(
    val eraId: String,
    val eraName: String,
    val timesPlayed: Int,
    val bestEnding: GameEnding?
)
