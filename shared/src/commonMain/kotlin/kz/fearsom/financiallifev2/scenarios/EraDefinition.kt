package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.*
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.*

// ════════════════════════════════════════════════════════════════════
//  ERA DATA STRUCTURES
// ════════════════════════════════════════════════════════════════════

/**
 * Defines a historical period with:
 * - Global crises that fire at specific real-world dates
 * - Pool weight modifiers that shift which events are more/less likely in this era
 *
 * Weight modifier keys can be either an event id or a tag (e.g. "scam.pyramid").
 * When both match, the higher multiplier wins.
 */
data class EraDefinition(
    val id: String,
    val name: String,
    val startYear: Int,
    val endYear: Int,
    /** Events that fire on a specific year/month in game time. */
    val globalEvents: List<EraGlobalEvent> = emptyList(),
    /**
     * Multipliers applied to pool event weights.
     * Key = eventId OR tag string (e.g. "scam.pyramid", "scam.crypto", "career").
     * Value = multiplier (1.0 = no change, 2.0 = double, 0.0 = disabled).
     */
    val poolWeightModifiers: Map<String, Float> = emptyMap()
)

/** A global crisis or world event scheduled for a specific in-game date. */
data class EraGlobalEvent(
    val eventId: String,
    val year: Int,
    val month: Int = 1,
    /** Probability 0.0–1.0. Use < 1.0 for events that don't hit every player. */
    val probability: Float = 1.0f
)

// ════════════════════════════════════════════════════════════════════
//  ERA EVENT LIBRARY  — global crises referenced by EraDefinition
// ════════════════════════════════════════════════════════════════════

object EraEventLibrary {

    val all: List<GameEvent> = listOf(

        // ── KZ 90s ────────────────────────────────────────────────────

        GameEvent(
            id = "era_ussr_collapse",
            message = Strings["evt_era_ussr_collapse_msg"],
            flavor = "🏚️",
            tags = setOf("crisis", "era.kz_90s"),
            options = listOf(
                GameOption("barter_survive", Strings["evt_era_ussr_collapse_opt_barter_survive"], "🥖",
                    effects = Effect(capitalDelta = -50_000, stressDelta = 20, knowledgeDelta = 10),
                    next = MONTHLY_TICK),
                GameOption("hard_currency", Strings["evt_era_ussr_collapse_opt_hard_currency"], "💵",
                    effects = Effect(capitalDelta = -30_000, stressDelta = 10, knowledgeDelta = 15),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_tenge_introduced",
            message = Strings["evt_era_tenge_introduced_msg"],
            flavor = "💴",
            tags = setOf("crisis", "era.kz_90s"),
            options = listOf(
                GameOption("exchange_all", Strings["evt_era_tenge_introduced_opt_exchange_all"], "🏃",
                    effects = Effect(
                        stressDelta = -5,
                        knowledgeDelta = 8,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1,
                            denominator = 500
                        )
                    ),
                    next = MONTHLY_TICK),
                GameOption("wait_see", Strings["evt_era_tenge_introduced_opt_wait_see"], "⏳",
                    effects = Effect(
                        capitalDelta = -30_000,
                        stressDelta = 15,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1,
                            denominator = 500
                        )
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_mmm_wave_90s",
            message = Strings["evt_era_mmm_wave_90s_msg"],
            flavor = "📺",
            tags = setOf("scam", "scam.pyramid", "era.kz_90s"),
            unique = true,
            poolWeight = 25,
            options = listOf(
                GameOption("invest_mmm", Strings["evt_era_mmm_wave_90s_opt_invest_mmm"], "💸",
                    effects = Effect(
                        capitalDelta = -100_000, stressDelta = 5, riskDelta = 25,
                        scheduleEvent = ScheduledEvent("era_mmm_collapse", afterMonths = 4)
                    ),
                    next = MONTHLY_TICK),
                GameOption("skeptical", Strings["evt_era_mmm_wave_90s_opt_skeptical"], "🤔",
                    effects = Effect(
                        knowledgeDelta = 20, stressDelta = -5,
                        setFlags = setOf("learned.scam.pyramid")
                    ),
                    next = "era_mmm_skeptic_result")
            )
        ),

        GameEvent(
            id = "era_mmm_collapse",
            message = Strings["evt_era_mmm_collapse_msg"],
            flavor = "💀",
            tags = setOf("scam.pyramid", "consequence", "era.kz_90s"),
            options = listOf(
                GameOption("accept_lesson", Strings["evt_era_mmm_collapse_opt_accept_lesson"], "📚",
                    effects = Effect(
                        knowledgeDelta = 30, stressDelta = 15,
                        setFlags = setOf("learned.scam.pyramid", "lost_money_to_scam")
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_mmm_skeptic_result",
            message = Strings["evt_era_mmm_skeptic_result_msg"],
            flavor = "🛡️",
            tags = setOf("era.kz_90s"),
            options = listOf(
                GameOption("be_grateful", Strings["evt_era_mmm_skeptic_result_opt_be_grateful"], "🙏",
                    effects = Effect(knowledgeDelta = 10, stressDelta = -5),
                    next = MONTHLY_TICK)
            )
        ),

        // ── KZ Devaluation 2015 ───────────────────────────────────────

        GameEvent(
            id = "era_devaluation_2015",
            message = Strings["evt_era_devaluation_2015_msg"],
            flavor = "📉",
            tags = setOf("crisis", "era.kz_2015"),
            unique = true,
            options = listOf(
                GameOption("convert_to_dollar", Strings["evt_era_devaluation_2015_opt_convert_to_dollar"], "💵",
                    effects = Effect(stressDelta = 10, knowledgeDelta = 15),
                    next = MONTHLY_TICK),
                GameOption("keep_tenge", Strings["evt_era_devaluation_2015_opt_keep_tenge"], "🤞",
                    effects = Effect(capitalDelta = -100_000, stressDelta = 20),
                    next = MONTHLY_TICK),
                GameOption("buy_property", Strings["evt_era_devaluation_2015_opt_buy_property"], "🏠",
                    effects = Effect(capitalDelta = -200_000, stressDelta = 5, knowledgeDelta = 10),
                    next = MONTHLY_TICK)
            )
        ),

        // ── COVID 2020 ────────────────────────────────────────────────

        GameEvent(
            id = "era_covid_shock_2020",
            message = Strings["evt_era_covid_shock_2020_msg"],
            flavor = "🦠",
            tags = setOf("crisis", "era.modern"),
            unique = true,
            options = listOf(
                GameOption("has_cushion_good", Strings["evt_era_covid_shock_2020_opt_has_cushion_good"], "🛡️",
                    effects = Effect(stressDelta = -10, knowledgeDelta = 15),
                    next = MONTHLY_TICK),
                GameOption("no_cushion_crisis", Strings["evt_era_covid_shock_2020_opt_no_cushion_crisis"], "😱",
                    effects = Effect(incomeDelta = -100_000, stressDelta = 30, knowledgeDelta = 20),
                    next = MONTHLY_TICK),
                GameOption("buy_dip", Strings["evt_era_covid_shock_2020_opt_buy_dip"], "📈",
                    effects = Effect(
                        capitalDelta = -100_000, investmentsDelta = 100_000,
                        stressDelta = 5, knowledgeDelta = 20,
                        scheduleEvent = ScheduledEvent("era_covid_recovery_gain", afterMonths = 12)
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        GameEvent(
            id = "era_covid_recovery_gain",
            message = Strings["evt_era_covid_recovery_gain_msg"],
            flavor = "🚀",
            tags = setOf("investment", "consequence"),
            options = listOf(
                GameOption("celebrate_wisdom", Strings["evt_era_covid_recovery_gain_opt_celebrate_wisdom"], "🏆",
                    effects = Effect(
                        investmentsDelta = 70_000,
                        knowledgeDelta = 15, stressDelta = -10
                    ),
                    next = MONTHLY_TICK)
            )
        ),

        // ── KZ Devaluation 2022 ───────────────────────────────────────

        GameEvent(
            id = "era_kz_devaluation_2022",
            message = Strings["evt_era_kz_devaluation_2022_msg"],
            flavor = "⚡",
            tags = setOf("crisis", "era.modern"),
            unique = true,
            options = listOf(
                GameOption("diversify_currency", Strings["evt_era_kz_devaluation_2022_opt_diversify_currency"], "⚖️",
                    effects = Effect(stressDelta = -5, knowledgeDelta = 20),
                    next = MONTHLY_TICK),
                GameOption("panic_buy_dollar", Strings["evt_era_kz_devaluation_2022_opt_panic_buy_dollar"], "😰",
                    effects = Effect(stressDelta = 15, knowledgeDelta = 5),
                    next = MONTHLY_TICK),
                GameOption("ignore_it", Strings["evt_era_kz_devaluation_2022_opt_ignore_it"], "😶",
                    effects = Effect(capitalDelta = -80_000, stressDelta = 10),
                    next = MONTHLY_TICK)
            )
        )
    )

    fun findById(id: String): GameEvent? = all.find { it.id == id }
}

// ════════════════════════════════════════════════════════════════════
//  ERA REGISTRY — predefined eras used by ScenarioGraphFactory
// ════════════════════════════════════════════════════════════════════

object EraRegistry {

    val MODERN_KZ_2024 = EraDefinition(
        id = "modern_kz_2024",
        name = Strings.eraModernKz2024Name,
        startYear = 2020,
        endYear = 2030,
        globalEvents = listOf(
            EraGlobalEvent("era_covid_shock_2020",    year = 2020, month = 3),
            EraGlobalEvent("era_kz_devaluation_2022", year = 2022, month = 3, probability = 0.85f)
        ),
        poolWeightModifiers = mapOf(
            "scam.crypto"     to 2.5f,   // crypto scams peak in this era
            "scam.pyramid"    to 1.5f,
            "scam.romance"    to 2.0f,   // pig butchering is modern
            "scam.betting"    to 1.8f,
            "scam.mlm"        to 1.3f,
            "career"          to 1.5f,
            "investment"      to 1.8f
        )
    )

    val KZ_90S = EraDefinition(
        id = "kz_90s",
        name = Strings.eraKz90sName,
        startYear = 1991,
        endYear = 2000,
        globalEvents = listOf(
            EraGlobalEvent("era_ussr_collapse",  year = 1991, month = 12),
            EraGlobalEvent("era_tenge_introduced", year = 1993, month = 11),
            EraGlobalEvent("era_mmm_wave_90s",   year = 1994, month = 6, probability = 0.9f),
            EraGlobalEvent("chechen_war_broadcast", year = 1994, month = 12),
            EraGlobalEvent("nuclear_disarmament_reaction", year = 1995, month = 4),
            EraGlobalEvent("capital_move_debate", year = 1997, month = 12)
        ),
        poolWeightModifiers = mapOf(
            "scam.pyramid" to 4.0f,    // MMM era — rampant
            "scam.crypto"  to 0.0f,    // doesn't exist yet
            "scam.romance" to 0.0f,    // no internet yet
            "crisis"       to 3.0f,
            "scam.mlm"     to 2.0f     // Amway/Oriflame wave just starting
        )
    )

    val KZ_2015_DEVALUATION = EraDefinition(
        id = "kz_2015",
        name = Strings.eraKz2015Name,
        startYear = 2014,
        endYear = 2019,
        globalEvents = listOf(
            EraGlobalEvent("era_devaluation_2015", year = 2015, month = 8)
        ),
        poolWeightModifiers = mapOf(
            "scam.pyramid" to 2.0f,
            "scam.mlm"     to 2.5f,
            "crisis"       to 2.0f,
            "scam.crypto"  to 0.5f  // early crypto, not mainstream yet
        )
    )

    private val all = listOf(MODERN_KZ_2024, KZ_90S, KZ_2015_DEVALUATION)

    fun findById(eraId: String): EraDefinition? = all.find { it.id == eraId }
}
