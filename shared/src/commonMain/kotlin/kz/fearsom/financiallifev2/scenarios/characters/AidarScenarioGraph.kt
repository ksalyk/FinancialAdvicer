package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option
import kz.fearsom.financiallifev2.scenarios.story
import kz.fearsom.financiallifev2.i18n.Strings

class AidarScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = when (eraId) {
        "kz_2005" -> PlayerState(
            capital = 120_000L,
            income = 95_000L,
            expenses = 68_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.07,
            stress = 28,
            financialKnowledge = 12,
            riskLevel = 22,
            month = 1,
            year = 2005,
            characterId = "aidar",
            eraId = eraId
        )
        "kz_2015" -> PlayerState(
            capital = 260_000L,
            income = 310_000L,
            expenses = 215_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.08,
            stress = 30,
            financialKnowledge = 18,
            riskLevel = 24,
            month = 1,
            year = 2015,
            characterId = "aidar",
            eraId = eraId
        )
        else -> PlayerState(
            capital = 420_000L,
            income = 520_000L,
            expenses = 325_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.08,
            stress = 32,
            financialKnowledge = 24,
            riskLevel = 28,
            month = 1,
            year = 2024,
            characterId = "aidar",
            eraId = eraId
        )
    }

    override val events: Map<String, GameEvent> = when (eraId) {
        "kz_2005" -> aidar2005Events()
        "kz_2015" -> aidar2015Events()
        else -> aidar2024Events()
    }

    override val conditionalEvents: List<GameEvent> = when (eraId) {
        "kz_2005" -> aidar2005Conditionals()
        "kz_2015" -> aidar2015Conditionals()
        else -> aidar2024Conditionals()
    }

    override val eventPool: List<PoolEntry> = when (eraId) {
        "kz_2005" -> listOf(
            PoolEntry("normal_life", 18),
            PoolEntry("family_pressure", 8),
            PoolEntry("freelance_order", 7)
        ) + ScamEventLibrary.poolEntries
        "kz_2015" -> listOf(
            PoolEntry("normal_life", 18),
            PoolEntry("family_pressure", 8),
            PoolEntry("mortgage_offer", 6),
            PoolEntry("freelance_order", 7)
        ) + ScamEventLibrary.poolEntries
        else -> listOf(
            PoolEntry("normal_life", 18),
            PoolEntry("family_pressure", 8),
            PoolEntry("mortgage_offer", 6),
            PoolEntry("freelance_order", 7)
        ) + ScamEventLibrary.poolEntries
    }

    private fun aidar2005Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "🖥️",
            message = Strings["evt_aidar_2005_intro_msg"],
            options = listOf(
                option("help_brother", Strings["evt_aidar_2005_intro_opt_help_brother"], "👨‍👩‍👦", "startup_pitch",
                    Effect(expensesDelta = 8_000L, stressDelta = -3, knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
                option("save_first", Strings["evt_aidar_2005_intro_opt_save_first"], "💵", "startup_pitch",
                    Effect(knowledgeDelta = 4, stressDelta = 2, setFlags = setOf("aidar.self.first")))
            )
        ))
        put("startup_pitch", event(
            id = "startup_pitch",
            flavor = "📦",
            message = Strings["evt_aidar_2005_startup_pitch_msg"],
            options = listOf(
                option("join_startup", Strings["evt_aidar_2005_startup_pitch_opt_join_startup"], "🎲", "startup_3months",
                    Effect(capitalDelta = -80_000L, stressDelta = 14, riskDelta = 12,
                        scheduleEvent = ScheduledEvent("startup_aftershock", 3))),
                option("partial_startup", Strings["evt_aidar_2005_startup_pitch_opt_partial_startup"], "🛠️", "senior_promotion",
                    Effect(stressDelta = 8, knowledgeDelta = 7, setFlags = setOf("aidar.side_hustle"))),
                option("skip_startup", Strings["evt_aidar_2005_startup_pitch_opt_skip_startup"], "🛡️", "senior_promotion",
                    Effect(knowledgeDelta = 4, stressDelta = -2))
            )
        ))
        put("startup_3months", event(
            id = "startup_3months",
            flavor = "🌃",
            message = Strings["evt_aidar_2005_startup_3months_msg"],
            options = listOf(
                option("pitch_investors", Strings["evt_aidar_2005_startup_3months_opt_pitch_investors"], "🚀", "startup_result",
                    Effect(stressDelta = 18, knowledgeDelta = 6, riskDelta = 10)),
                option("exit_startup", Strings["evt_aidar_2005_startup_3months_opt_exit_startup"], "🚪", "senior_promotion",
                    Effect(capitalDelta = 45_000L, stressDelta = -10, knowledgeDelta = 5))
            )
        ))
        put("startup_result", event(
            id = "startup_result",
            flavor = "⚖️",
            message = Strings["evt_aidar_2005_startup_result_msg"],
            options = listOf(
                option("strong_pitch", Strings["evt_aidar_2005_startup_result_opt_strong_pitch"], "💼", MONTHLY_TICK,
                    Effect(incomeDelta = 60_000L, stressDelta = 20, knowledgeDelta = 12, setFlags = setOf("aidar.2005_trade_risk"))),
                option("safe_exit", Strings["evt_aidar_2005_startup_result_opt_safe_exit"], "🔄", "senior_promotion",
                    Effect(stressDelta = -12, knowledgeDelta = 8, setFlags = setOf("aidar.learned_hype")))
            )
        ))
        put("senior_promotion", event(
            id = "senior_promotion",
            flavor = "📈",
            message = Strings["evt_aidar_2005_senior_promotion_msg"],
            options = listOf(
                option("middle_promo", Strings["evt_aidar_2005_senior_promotion_opt_middle_promo"], "🧱", MONTHLY_TICK,
                    Effect(incomeDelta = 45_000L, stressDelta = 6, knowledgeDelta = 8)),
                option("senior_astana", Strings["evt_aidar_2005_senior_promotion_opt_senior_astana"], "🏙️", "freelance_order",
                    Effect(stressDelta = 12, knowledgeDelta = 10, riskDelta = 6)),
                option("stay_same", Strings["evt_aidar_2005_senior_promotion_opt_stay_same"], "🔍", MONTHLY_TICK,
                    Effect(knowledgeDelta = 3, stressDelta = -2))
            )
        ))
        commonAidarEvents()
        commonAidarEndings("2005")
    }

    private fun aidar2015Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "📱",
            message = Strings["evt_aidar_2015_intro_msg"],
            options = listOf(
                option("help_brother", Strings["evt_aidar_2015_intro_opt_help_brother"], "👪", "startup_pitch",
                    Effect(expensesDelta = 12_000L, stressDelta = -4, knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
                option("save_first", Strings["evt_aidar_2015_intro_opt_save_first"], "💼", "startup_pitch",
                    Effect(knowledgeDelta = 4, setFlags = setOf("aidar.buffer.first")))
            )
        ))
        put("startup_pitch", event(
            id = "startup_pitch",
            flavor = "💡",
            message = Strings["evt_aidar_2015_startup_pitch_msg"],
            options = listOf(
                option("join_startup", Strings["evt_aidar_2015_startup_pitch_opt_join_startup"], "🚀", "startup_3months",
                    Effect(capitalDelta = -150_000L, stressDelta = 16, knowledgeDelta = 6,
                        scheduleEvent = ScheduledEvent("startup_aftershock", 4))),
                option("partial_startup", Strings["evt_aidar_2015_startup_pitch_opt_partial_startup"], "💻", "senior_promotion",
                    Effect(stressDelta = 8, knowledgeDelta = 8, setFlags = setOf("aidar.side_hustle"))),
                option("skip_startup", Strings["evt_aidar_2015_startup_pitch_opt_skip_startup"], "🛡️", "senior_promotion",
                    Effect(knowledgeDelta = 4, setFlags = setOf("aidar.learned_boundaries")))
            )
        ))
        put("startup_3months", event(
            id = "startup_3months",
            flavor = "🕐",
            message = Strings["evt_aidar_2015_startup_3months_msg"],
            options = listOf(
                option("pitch_investors", Strings["evt_aidar_2015_startup_3months_opt_pitch_investors"], "🎤", "startup_result",
                    Effect(stressDelta = 20, knowledgeDelta = 10)),
                option("exit_startup", Strings["evt_aidar_2015_startup_3months_opt_exit_startup"], "🚪", "senior_promotion",
                    Effect(capitalDelta = 80_000L, stressDelta = -10, knowledgeDelta = 4))
            )
        ))
        put("startup_result", event(
            id = "startup_result",
            flavor = "🎯",
            message = Strings["evt_aidar_2015_startup_result_msg"],
            options = listOf(
                option("strong_pitch", Strings["evt_aidar_2015_startup_result_opt_strong_pitch"], "💪", "startup_success",
                    Effect(knowledgeDelta = 5)),
                option("safe_exit", Strings["evt_aidar_2015_startup_result_opt_safe_exit"], "🔄", "senior_promotion",
                    Effect(stressDelta = -12, knowledgeDelta = 6))
            )
        ))
        put("startup_success", event(
            id = "startup_success",
            flavor = "🏁",
            message = Strings["evt_aidar_2015_startup_success_msg"],
            options = listOf(
                option("quit_job", Strings["evt_aidar_2015_startup_success_opt_quit_job"], "🦅", MONTHLY_TICK,
                    Effect(capitalDelta = 500_000L, incomeDelta = -310_000L, stressDelta = 22, knowledgeDelta = 14,
                        setFlags = setOf("aidar.quit.for.startup"))),
                option("keep_job", Strings["evt_aidar_2015_startup_success_opt_keep_job"], "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = 500_000L, stressDelta = 28, knowledgeDelta = 9,
                        setFlags = setOf("aidar.double.load")))
            )
        ))
        put("senior_promotion", event(
            id = "senior_promotion",
            flavor = "📬",
            message = Strings["evt_aidar_2015_senior_promotion_msg"],
            options = listOf(
                option("middle_promo", Strings["evt_aidar_2015_senior_promotion_opt_middle_promo"], "🧗", MONTHLY_TICK,
                    Effect(incomeDelta = 80_000L, stressDelta = 5, knowledgeDelta = 8)),
                option("senior_astana", Strings["evt_aidar_2015_senior_promotion_opt_senior_astana"], "🏙️", MONTHLY_TICK,
                    Effect(incomeDelta = 150_000L, expensesDelta = 40_000L, stressDelta = 16, knowledgeDelta = 12,
                        setFlags = setOf("aidar.relocated"))),
                option("stay_same", Strings["evt_aidar_2015_senior_promotion_opt_stay_same"], "🕰️", MONTHLY_TICK,
                    Effect(knowledgeDelta = 4, stressDelta = -2))
            )
        ))
        commonAidarEvents()
        commonAidarEndings("2015")
    }

    private fun aidar2024Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "🤖",
            message = Strings["evt_aidar_2024_intro_msg"],
            options = listOf(
                option("help_brother", Strings["evt_aidar_2024_intro_opt_help_brother"], "🤝", "startup_pitch",
                    Effect(expensesDelta = 20_000L, stressDelta = -4, knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
                option("save_first", Strings["evt_aidar_2024_intro_opt_save_first"], "🛡️", "startup_pitch",
                    Effect(knowledgeDelta = 4, setFlags = setOf("aidar.buffer.first")))
            )
        ))
        put("startup_pitch", event(
            id = "startup_pitch",
            flavor = "🧠",
            message = Strings["evt_aidar_2024_startup_pitch_msg"],
            options = listOf(
                option("join_startup", Strings["evt_aidar_2024_startup_pitch_opt_join_startup"], "🚀", "startup_3months",
                    Effect(capitalDelta = -220_000L, stressDelta = 18, knowledgeDelta = 8,
                        scheduleEvent = ScheduledEvent("startup_aftershock", 3))),
                option("partial_startup", Strings["evt_aidar_2024_startup_pitch_opt_partial_startup"], "💻", "senior_promotion",
                    Effect(stressDelta = 10, knowledgeDelta = 10, setFlags = setOf("aidar.side_hustle"))),
                option("skip_startup", Strings["evt_aidar_2024_startup_pitch_opt_skip_startup"], "🧱", "senior_promotion",
                    Effect(knowledgeDelta = 5, setFlags = setOf("aidar.learned_boundaries")))
            )
        ))
        put("startup_3months", event(
            id = "startup_3months",
            flavor = "📊",
            message = Strings["evt_aidar_2024_startup_3months_msg"],
            options = listOf(
                option("pitch_investors", Strings["evt_aidar_2024_startup_3months_opt_pitch_investors"], "🎤", "startup_result",
                    Effect(stressDelta = 22, knowledgeDelta = 10)),
                option("exit_startup", Strings["evt_aidar_2024_startup_3months_opt_exit_startup"], "🚪", "senior_promotion",
                    Effect(capitalDelta = 120_000L, stressDelta = -12, knowledgeDelta = 5))
            )
        ))
        put("startup_result", event(
            id = "startup_result",
            flavor = "🧾",
            message = Strings["evt_aidar_2024_startup_result_msg"],
            options = listOf(
                option("strong_pitch", Strings["evt_aidar_2024_startup_result_opt_strong_pitch"], "🔥", "startup_success",
                    Effect(knowledgeDelta = 6)),
                option("safe_exit", Strings["evt_aidar_2024_startup_result_opt_safe_exit"], "🧭", "senior_promotion",
                    Effect(stressDelta = -10, knowledgeDelta = 8))
            )
        ))
        put("startup_success", event(
            id = "startup_success",
            flavor = "🎉",
            message = Strings["evt_aidar_2024_startup_success_msg"],
            options = listOf(
                option("quit_job", Strings["evt_aidar_2024_startup_success_opt_quit_job"], "🦅", MONTHLY_TICK,
                    Effect(capitalDelta = 900_000L, incomeDelta = -520_000L, stressDelta = 24, knowledgeDelta = 16,
                        setFlags = setOf("aidar.quit.for.startup"))),
                option("keep_job", Strings["evt_aidar_2024_startup_success_opt_keep_job"], "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = 900_000L, stressDelta = 30, knowledgeDelta = 10,
                        setFlags = setOf("aidar.double.load")))
            )
        ))
        put("senior_promotion", event(
            id = "senior_promotion",
            flavor = "📨",
            message = Strings["evt_aidar_2024_senior_promotion_msg"],
            options = listOf(
                option("middle_promo", Strings["evt_aidar_2024_senior_promotion_opt_middle_promo"], "🧱", MONTHLY_TICK,
                    Effect(incomeDelta = 120_000L, stressDelta = 4, knowledgeDelta = 8)),
                option("senior_astana", Strings["evt_aidar_2024_senior_promotion_opt_senior_astana"], "🚄", MONTHLY_TICK,
                    Effect(incomeDelta = 220_000L, expensesDelta = 60_000L, stressDelta = 15, knowledgeDelta = 12,
                        setFlags = setOf("aidar.relocated"))),
                option("stay_same", Strings["evt_aidar_2024_senior_promotion_opt_stay_same"], "🕰️", MONTHLY_TICK,
                    Effect(knowledgeDelta = 4, stressDelta = -3))
            )
        ))
        commonAidarEvents()
        commonAidarEndings("2024")
    }

    private fun MutableMap<String, GameEvent>.commonAidarEvents() {
        put("freelance_order", event(
            id = "freelance_order",
            flavor = "🧾",
            poolWeight = 8,
            tags = setOf("career"),
            message = Strings["evt_aidar_2024_freelance_order_msg"],
            options = listOf(
                option("freelance_yes", Strings["evt_aidar_2024_freelance_order_opt_freelance_yes"], "💼", MONTHLY_TICK,
                    Effect(incomeDelta = 90_000L, stressDelta = 14, knowledgeDelta = 6)),
                option("freelance_half", Strings["evt_aidar_2024_freelance_order_opt_freelance_half"], "✂️", MONTHLY_TICK,
                    Effect(incomeDelta = 40_000L, stressDelta = 5, knowledgeDelta = 5)),
                option("freelance_no", Strings["evt_aidar_2024_freelance_order_opt_freelance_no"], "🛏️", MONTHLY_TICK,
                    Effect(stressDelta = -5, knowledgeDelta = 2))
            )
        ))
        put("family_pressure", event(
            id = "family_pressure",
            flavor = "👨‍👩‍👧",
            poolWeight = 8,
            tags = setOf("family"),
            message = Strings["evt_aidar_2024_family_pressure_msg"],
            options = listOf(
                option("help_fully", Strings["evt_aidar_2024_family_pressure_opt_help_fully"], "❤️", MONTHLY_TICK,
                    Effect(capitalDelta = -220_000L, stressDelta = 8, setFlags = setOf("aidar.family.helped"))),
                option("help_partial", Strings["evt_aidar_2024_family_pressure_opt_help_partial"], "🏥", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, stressDelta = 4, knowledgeDelta = 2)),
                option("help_minimum", Strings["evt_aidar_2024_family_pressure_opt_help_minimum"], "🧊", MONTHLY_TICK,
                    Effect(capitalDelta = -40_000L, stressDelta = 14, setFlags = setOf("aidar.guilt"))))
        ))
        put("mortgage_offer", event(
            id = "mortgage_offer",
            flavor = "🏠",
            poolWeight = 6,
            tags = setOf("mortgage"),
            conditions = listOf(cond(CAPITAL, GTE, 2_400_000L), Condition.NotFlag("aidar.has.mortgage")),
            message = Strings["evt_aidar_2024_mortgage_offer_msg"],
            options = listOf(
                option("take_mortgage", Strings["evt_aidar_2024_mortgage_offer_opt_take_mortgage"], "🔑", MONTHLY_TICK,
                    Effect(capitalDelta = -2_400_000L, debtDelta = 9_600_000L, expensesDelta = 85_000L, stressDelta = 16, knowledgeDelta = 4,
                        setFlags = setOf("aidar.has.mortgage"))),
                option("skip_mortgage", Strings["evt_aidar_2024_mortgage_offer_opt_skip_mortgage"], "⏳", MONTHLY_TICK,
                    Effect(knowledgeDelta = 4, stressDelta = -2))
            )
        ))
        put("startup_aftershock", event(
            id = "startup_aftershock",
            flavor = "🧨",
            tags = setOf("consequence"),
            message = Strings["evt_aidar_2024_startup_aftershock_msg"],
            options = listOf(
                option("aftershock_scale", Strings["evt_aidar_2024_startup_aftershock_opt_aftershock_scale"], "📈", MONTHLY_TICK,
                    Effect(incomeDelta = 60_000L, stressDelta = 16, knowledgeDelta = 6, riskDelta = 8)),
                option("aftershock_exit", Strings["evt_aidar_2024_startup_aftershock_opt_aftershock_exit"], "🛟", MONTHLY_TICK,
                    Effect(stressDelta = -12, knowledgeDelta = 8, setFlags = setOf("aidar.learned_hype")))
            )
        ))
        put("normal_life", event(
            id = "normal_life",
            flavor = "☕",
            poolWeight = 20,
            message = Strings["evt_aidar_2024_normal_life_msg"],
            options = listOf(
                option("invest_etf", Strings["evt_aidar_2024_normal_life_opt_invest_etf"], "📊", MONTHLY_TICK,
                    Effect(capitalDelta = -40_000L, investmentsDelta = 40_000L, knowledgeDelta = 4)),
                option("online_course", Strings["evt_aidar_2024_normal_life_opt_online_course"], "📚", MONTHLY_TICK,
                    Effect(capitalDelta = -25_000L, knowledgeDelta = 8, stressDelta = -3)),
                option("save_cash", Strings["evt_aidar_2024_normal_life_opt_save_cash"], "🐷", MONTHLY_TICK,
                    Effect(stressDelta = -4)),
                option("network_it", Strings["evt_aidar_2024_normal_life_opt_network_it"], "🤝", MONTHLY_TICK,
                    Effect(capitalDelta = -10_000L, knowledgeDelta = 5, stressDelta = -4))
            )
        ))
    }

    private fun MutableMap<String, GameEvent>.commonAidarEndings(eraLabel: String) {
        put("ending_startup_king", event(
            id = "ending_startup_king",
            isEnding = true,
            endingType = EndingType.WEALTH,
            flavor = "🚀",
            message = Strings["evt_aidar_2024_ending_startup_king_msg"],
            options = emptyList()
        ))
        put("ending_senior_dev", event(
            id = "ending_senior_dev",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            flavor = "💼",
            message = Strings["evt_aidar_2024_ending_senior_dev_msg"],
            options = emptyList()
        ))
        put("ending_freedom", event(
            id = "ending_freedom",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            flavor = "🏖️",
            message = Strings["evt_aidar_2024_ending_freedom_msg"],
            options = emptyList()
        ))
        put("ending_broke", event(
            id = "ending_broke",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            flavor = "💀",
            message = Strings["evt_aidar_2024_ending_broke_msg"],
            options = emptyList()
        ))
        put("ending_paycheck", event(
            id = "ending_paycheck",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            flavor = "😐",
            message = Strings["evt_aidar_2024_ending_paycheck_msg"],
            options = emptyList()
        ))
    }

    private fun aidar2005Conditionals(): List<GameEvent> = commonAidarConditionals(
        burnoutLabel = Strings["evt_aidar_2005_burnout_label"],
        investmentLabel = Strings["evt_aidar_2005_investment_label"]
    )

    private fun aidar2015Conditionals(): List<GameEvent> = commonAidarConditionals(
        burnoutLabel = Strings["evt_aidar_2015_burnout_label"],
        investmentLabel = Strings["evt_aidar_2015_investment_label"]
    )

    private fun aidar2024Conditionals(): List<GameEvent> = commonAidarConditionals(
        burnoutLabel = Strings["evt_aidar_2024_burnout_label"],
        investmentLabel = Strings["evt_aidar_2024_investment_label"]
    )

    private fun commonAidarConditionals(
        burnoutLabel: String,
        investmentLabel: String
    ): List<GameEvent> = listOf(
        event(
            id = "debt_crisis",
            priority = 10,
            flavor = "🚨",
            conditions = listOf(cond(DEBT, GT, 0L), cond(CAPITAL, LTE, 70_000L)),
            message = Strings["evt_aidar_2024_debt_crisis_msg"],
            options = listOf(
                option("sell_investments", Strings["evt_aidar_2024_debt_crisis_opt_sell_investments"], "📉", MONTHLY_TICK,
                    Effect(debtDelta = -200_000L, investmentsDelta = -150_000L, stressDelta = 12)),
                option("debt_restructure", Strings["evt_aidar_2024_debt_crisis_opt_debt_restructure"], "🏦", MONTHLY_TICK,
                    Effect(expensesDelta = -20_000L, stressDelta = 18, knowledgeDelta = 5))
            )
        ),
        event(
            id = "burnout_warning",
            priority = 8,
            flavor = "😮‍💨",
            conditions = listOf(cond(STRESS, GTE, 75L)),
            message = Strings["evt_aidar_2024_burnout_warning_msg"],
            options = listOf(
                option("take_vacation", Strings["evt_aidar_2024_burnout_warning_opt_take_vacation"], "🏖️", MONTHLY_TICK,
                    Effect(capitalDelta = -60_000L, stressDelta = -30, knowledgeDelta = 3)),
                option("push_through", Strings["evt_aidar_2024_burnout_warning_opt_push_through"], "😤", MONTHLY_TICK,
                    Effect(stressDelta = 10, knowledgeDelta = 2))
            )
        ),
        event(
            id = "investment_unlock",
            priority = 5,
            flavor = "💡",
            unique = true,
            conditions = listOf(cond(KNOWLEDGE, GTE, 42L)),
            message = Strings["evt_aidar_2024_investment_unlock_msg"],
            options = listOf(
                option("open_iis", Strings["evt_aidar_2024_investment_unlock_opt_open_iis"], "📈", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, investmentsDelta = 120_000L, knowledgeDelta = 5)),
                option("skip_iis", Strings["evt_aidar_2024_investment_unlock_opt_skip_iis"], "⏸️", MONTHLY_TICK,
                    Effect())
            )
        ),
        event(
            id = "ending_broke_trigger",
            priority = 100,
            flavor = "🧱",
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            message = Strings["evt_aidar_2024_ending_broke_trigger_msg"],
            options = listOf(option("accept_broke", Strings["evt_aidar_2024_ending_broke_trigger_opt_accept_broke"], "💀", "ending_broke"))
        ),
        event(
            id = "ending_freedom_trigger",
            priority = 4,
            unique = true,
            flavor = "🌅",
            conditions = listOf(cond(CAPITAL, GTE, 8_000_000L), cond(KNOWLEDGE, GTE, 60L)),
            message = Strings["evt_aidar_2024_ending_freedom_trigger_msg"],
            options = listOf(option("claim_freedom", Strings["evt_aidar_2024_ending_freedom_trigger_opt_claim_freedom"], "🏖️", "ending_freedom"))
        ),
        event(
            id = "ending_stability_trigger",
            priority = 3,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 3_000_000L), cond(STRESS, LTE, 45L)),
            message = Strings["evt_aidar_2024_ending_stability_trigger_msg"],
            options = listOf(option("claim_stability", Strings["evt_aidar_2024_ending_stability_trigger_opt_claim_stability"], "💼", "ending_senior_dev"))
        ),
        event(
            id = "ending_wealth_trigger",
            priority = 2,
            unique = true,
            conditions = listOf(
                cond(CAPITAL, GTE, 15_000_000L),
                Condition.HasFlag("aidar.quit.for.startup")
            ),
            message = Strings["evt_aidar_2024_ending_wealth_trigger_msg"],
            options = listOf(option("claim_wealth", Strings["evt_aidar_2024_ending_wealth_trigger_opt_claim_wealth"], "🚀", "ending_startup_king"))
        ),
        event(
            id = "ending_paycheck_trigger",
            priority = 1,
            unique = true,
            conditions = listOf(cond(CAPITAL, LTE, 120_000L), cond(KNOWLEDGE, LTE, 25L)),
            message = Strings["evt_aidar_2024_ending_paycheck_trigger_msg"],
            options = listOf(option("accept_paycheck", Strings["evt_aidar_2024_ending_paycheck_trigger_opt_accept_paycheck"], "😐", "ending_paycheck"))
        )
    )
}
