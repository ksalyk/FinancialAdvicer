package kz.fearsom.financiallifev2.model

import kotlinx.serialization.Serializable
import kz.fearsom.financiallifev2.i18n.Strings

// ════════════════════════════════════════════════════════════════════
//  LAYER 1 — NARRATIVE GRAPH
// ════════════════════════════════════════════════════════════════════

/**
 * Delta-based effect on [PlayerState].
 * Every numeric field is a delta: positive = increase, negative = decrease.
 * [setFlags] / [clearFlags] mutate the player's boolean flag set.
 * [scheduleEvent] queues a named event to fire N months from now.
 */
@Serializable
data class Effect(
    val capitalDelta: Long = 0,
    val incomeDelta: Long = 0,
    val expensesDelta: Long = 0,
    val debtDelta: Long = 0,
    val debtPaymentDelta: Long = 0,    // change in monthly debt repayment
    val investmentsDelta: Long = 0,
    val stressDelta: Int = 0,
    val knowledgeDelta: Int = 0,
    val riskDelta: Int = 0,
    /** Flags added to [PlayerState.flags] when this choice is made. */
    val setFlags: Set<String> = emptySet(),
    /** Flags removed from [PlayerState.flags] when this choice is made. */
    val clearFlags: Set<String> = emptySet(),
    /** Deferred event that fires [ScheduledEvent.afterMonths] months from now. */
    val scheduleEvent: ScheduledEvent? = null,
    /** Reprices local-currency amounts and optionally switches active currency. */
    val monetaryReform: MonetaryReform? = null
)

@Serializable
enum class CurrencyCode {
    RUB, KZT, USD
}

/**
 * Applies a single ratio to all local-currency money fields in [PlayerState].
 * Used for redenominations and currency reforms such as RUB → KZT 500:1.
 */
@Serializable
data class MonetaryReform(
    val from: CurrencyCode,
    val to: CurrencyCode,
    val numerator: Long,
    val denominator: Long
)

/** Describes a future event to be queued inside an [Effect]. */
@Serializable
data class ScheduledEvent(
    val eventId: String,
    val afterMonths: Int
)

/** A pending scheduled event stored inside [PlayerState] until it fires. */
@Serializable
data class PendingEvent(
    val eventId: String,
    val fireAtYear: Int,
    val fireAtMonth: Int
)

/**
 * Condition that must hold for a conditional or pool event to be eligible.
 * Sealed hierarchy supports numeric stats, boolean flags, era, and character targeting.
 * All conditions in an event's list must pass simultaneously (AND logic).
 */
@Serializable
sealed class Condition {
    abstract fun check(state: PlayerState): Boolean

    /** Numeric stat comparison. Example: CAPITAL > 500_000 */
    @Serializable
    data class Stat(val field: Field, val op: Op, val value: Long) : Condition() {
        override fun check(state: PlayerState): Boolean {
            val actual: Long = when (field) {
                Field.CAPITAL   -> state.capital
                Field.INCOME    -> state.income
                Field.EXPENSES  -> state.expenses
                Field.DEBT      -> state.debt
                Field.STRESS    -> state.stress.toLong()
                Field.KNOWLEDGE -> state.financialKnowledge.toLong()
                Field.RISK      -> state.riskLevel.toLong()
                Field.MONTH     -> state.month.toLong()
            }
            return when (op) {
                Op.GT  -> actual > value
                Op.LT  -> actual < value
                Op.GTE -> actual >= value
                Op.LTE -> actual <= value
                Op.EQ  -> actual == value
                Op.NEQ -> actual != value
            }
        }

        @Serializable
        enum class Field { CAPITAL, INCOME, EXPENSES, DEBT, STRESS, KNOWLEDGE, RISK, MONTH }

        @Serializable
        enum class Op { GT, LT, GTE, LTE, EQ, NEQ }
    }

    /** True when [flag] is present in [PlayerState.flags]. */
    @Serializable
    data class HasFlag(val flag: String) : Condition() {
        override fun check(state: PlayerState) = flag in state.flags
    }

    /** True when [flag] is NOT present in [PlayerState.flags]. */
    @Serializable
    data class NotFlag(val flag: String) : Condition() {
        override fun check(state: PlayerState) = flag !in state.flags
    }

    /** True when the player is in the given era. */
    @Serializable
    data class InEra(val eraId: String) : Condition() {
        override fun check(state: PlayerState) = normalizeEraId(state.eraId) == normalizeEraId(eraId)

        private fun normalizeEraId(value: String) = when (value) {
            "modern_kz_2024" -> "kz_2024"
            else -> value
        }
    }

    /** True when playing as the given character archetype. */
    @Serializable
    data class ForCharacter(val characterId: String) : Condition() {
        override fun check(state: PlayerState) = state.characterId == characterId
    }
}

/** A player choice within an event. */
@Serializable
data class GameOption(
    val id: String,
    val text: String,
    val emoji: String,
    val effects: Effect = Effect(),
    /** Where to go after this choice. Use [MONTHLY_TICK] to trigger monthly sim first. */
    val next: String
)

/**
 * One node in the narrative graph.
 *
 * [conditions]     — ALL must be true for this event to be injected (leave empty for story events).
 * [priority]       — higher = checked first when multiple conditional events match.
 * [tags]           — categories for pool filtering and era weight modifiers.
 *                    Convention: "scam", "scam.pyramid", "career", "crisis", "family", etc.
 * [poolWeight]     — relative weight in random pool selection (higher = more likely).
 * [unique]         — if true, can only fire once per game session.
 * [cooldownMonths] — minimum months before this event can fire again (0 = no cooldown).
 * [isEnding]       — leaf node that terminates the game.
 */
@Serializable
data class GameEvent(
    val id: String,
    val message: String,
    val flavor: String = "💬",
    val options: List<GameOption>,
    val conditions: List<Condition> = emptyList(),
    val priority: Int = 0,
    val isEnding: Boolean = false,
    val endingType: EndingType? = null,
    val tags: Set<String> = emptySet(),
    val poolWeight: Int = 10,
    val unique: Boolean = false,
    val cooldownMonths: Int = 0
)

/** Entry in a weighted event pool. */
@Serializable
data class PoolEntry(val eventId: String, val baseWeight: Int)

/** Sentinel next-event ID: causes the engine to run a monthly economic tick first. */
const val MONTHLY_TICK = "__monthly_tick__"

@Serializable
enum class EndingType {
    BANKRUPTCY, PAYCHECK_TO_PAYCHECK, FINANCIAL_STABILITY, FINANCIAL_FREEDOM, WEALTH
}

// ════════════════════════════════════════════════════════════════════
//  LAYER 2 — STATE SYSTEM
// ════════════════════════════════════════════════════════════════════

/**
 * Full economic + psychological state of the player.
 * Monetary fields are denominated in [currency].
 */
@Serializable
data class PlayerState(
    // ── Money ────────────────────────────────────────────────────────
    val capital: Long    = 200_000L,    // liquid savings
    val income: Long     = 450_000L,    // monthly gross income
    val expenses: Long   = 200_000L,    // fixed monthly expenses
    val debt: Long       = 120_000L,    // total outstanding debt
    val debtPaymentMonthly: Long = 18_000L,  // monthly repayment amount
    val investments: Long = 0L,         // total money in market
    val investmentReturnRate: Double = 0.08, // annual return (8% default)

    // ── Soft stats ───────────────────────────────────────────────────
    val stress: Int             = 25,   // 0–100
    val financialKnowledge: Int = 10,   // 0–100
    val riskLevel: Int          = 15,   // 0–100

    // ── Time ─────────────────────────────────────────────────────────
    val month: Int = 1,
    val year:  Int = 2024,

    // ── Identity ─────────────────────────────────────────────────────
    /** Character archetype ID — used for event targeting. */
    val characterId: String = "",
    /** Era ID — used for era-specific event filtering and weight modifiers. */
    val eraId: String = "",
    /** Active local currency for all monetary fields above. */
    val currency: CurrencyCode = CurrencyCode.KZT,

    // ── Event tracking ────────────────────────────────────────────────
    /** Boolean game-state flags, e.g. "learned.scam.pyramid", "has_emergency_fund". */
    val flags: Set<String> = emptySet(),
    /** IDs of unique events already triggered — prevents re-triggering. */
    val triggeredUniqueEvents: Set<String> = emptySet(),
    /** Deferred events queued by previous choices. */
    val pendingScheduled: List<PendingEvent> = emptyList(),
    /** eventId → absoluteMonth when cooldown expires. */
    val eventCooldowns: Map<String, Int> = emptyMap()
) {
    val monthLabel: String get() = "$year / ${"$month".padStart(2, '0')}"

    /** Net cash flow after all obligations. */
    val netMonthlyFlow: Long get() =
        income - expenses - debtPaymentMonthly + monthlyInvestmentReturn

    val monthlyInvestmentReturn: Long get() =
        (investments * investmentReturnRate / 12).toLong()

    val netWorth: Long get() = capital + investments - debt

    /** Absolute month counter used for cooldown calculations. */
    val absoluteMonth: Int get() = year * 12 + month
}

/** Result of a single monthly economic simulation tick. */
@Serializable
data class MonthlyReport(
    val month: Int,
    val year: Int,
    val currency: CurrencyCode,
    val incomeReceived: Long,
    val expensesPaid: Long,
    val debtPayment: Long,
    val investmentGain: Long,
    val netFlow: Long,
    val capitalBefore: Long,
    val capitalAfter: Long,
    val debtAfter: Long,
    val stressDelta: Int
) {
    fun toMessage(): String = buildString {
        appendLine("${Strings.sysMonthlyTitle} ${Strings.monthNames.getOrElse(month) { "?" }} $year")
        appendLine()
        appendLine("${Strings.sysMonthlyIncome}+${incomeReceived.moneyFormat(currency)}")
        appendLine("${Strings.sysMonthlyExpenses}-${expensesPaid.moneyFormat(currency)}")
        if (debtPayment > 0) appendLine("${Strings.sysMonthlyDebtPayment}-${debtPayment.moneyFormat(currency)}")
        if (investmentGain > 0) appendLine("${Strings.sysMonthlyInvestments}+${investmentGain.moneyFormat(currency)}")
        appendLine()
        val sign = if (netFlow >= 0) "+" else ""
        appendLine("${if (netFlow >= 0) Strings.sysMonthlyNetPositive else Strings.sysMonthlyNetNegative} $sign${netFlow.moneyFormat(currency)}")
        appendLine("${Strings.sysMonthlyCapital}${capitalAfter.moneyFormat(currency)}")
        if (debtAfter > 0) append("${Strings.sysMonthlyDebtRemaining}${debtAfter.moneyFormat(currency)}")
    }.trimEnd()
}

// ════════════════════════════════════════════════════════════════════
//  CHAT / UI LAYER
// ════════════════════════════════════════════════════════════════════

@Serializable
enum class MessageSender { CHARACTER, SYSTEM, MONTHLY_REPORT, PLAYER }

@Serializable
data class ChatMessage(
    val id: String = "",
    val sender: MessageSender = MessageSender.SYSTEM,
    val text: String = "",
    val emoji: String = "",
    val timestampMs: Long = 0L,
    val sourceEventId: String? = null,
    val sourceOptionId: String? = null,
    val sourcePlayerState: PlayerState? = null,
    val monthlyReport: MonthlyReport? = null,
    val textKey: String? = null,
    val textArgs: List<String> = emptyList(),
    /**
     * Semantic scene category resolved from [GameEvent.tags].
     * Values: "scam" | "crisis" | "career" | "family" | "investment" |
     *         "mortgage" | "windfall" | "world" | null (no image)
     * The UI layer maps this string to the actual drawable resource.
     */
    val sceneTag: String? = null
)

// ════════════════════════════════════════════════════════════════════
//  COMPOSITE GAME STATE
// ════════════════════════════════════════════════════════════════════

@Serializable
data class GameState(
    val playerState: PlayerState = PlayerState(),
    val currentEventId: String = "",
    val characterName: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isWaitingForChoice: Boolean = false,
    val gameOver: Boolean = false,
    val endingType: EndingType? = null
)

// ════════════════════════════════════════════════════════════════════
//  AUTH
// ════════════════════════════════════════════════════════════════════

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class LoginResponse(
    val success: Boolean = false,
    val token: String  = "",
    val userId: String = "",
    val message: String = ""
)
