package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.i18n.Strings
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
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option

fun aidarMainArc(eraId: String, a: AidarEraAmounts): EventArc = EventArc { map ->
    val era = eraId.eraLabel()

    val introFlavor   = when (eraId) { "kz_2005" -> "🖥️"; "kz_2015" -> "📱"; else -> "🤖" }
    val pitchFlavor   = when (eraId) { "kz_2005" -> "📦"; "kz_2015" -> "💡"; else -> "🧠" }
    val pitch3Flavor  = when (eraId) { "kz_2005" -> "🌃"; "kz_2015" -> "🕐"; else -> "📊" }
    val resultFlavor  = when (eraId) { "kz_2005" -> "⚖️"; "kz_2015" -> "🎯"; else -> "🧾" }
    val successFlavor = when (eraId) { "kz_2015" -> "🏁"; else -> "🎉" }
    val seniorFlavor  = when (eraId) { "kz_2005" -> "📈"; "kz_2015" -> "📬"; else -> "📨" }

    map["intro"] = event(
        id = "intro",
        flavor = introFlavor,
        message = Strings["evt_aidar_${era}_intro_msg"],
        options = listOf(
            option("help_brother", Strings["evt_aidar_${era}_intro_opt_help_brother"],
                when (eraId) { "kz_2005" -> "👨‍👩‍👦"; "kz_2015" -> "👪"; else -> "🤝" },
                "startup_pitch",
                Effect(expensesDelta = a.introHelpBrotherExpenses, stressDelta = a.introHelpBrotherStress,
                    knowledgeDelta = 2, setFlags = setOf("aidar.family.first"))),
            option("save_first", Strings["evt_aidar_${era}_intro_opt_save_first"],
                when (eraId) { "kz_2005" -> "💵"; "kz_2015" -> "💼"; else -> "🛡️" },
                "startup_pitch",
                Effect(knowledgeDelta = 4, stressDelta = a.introSaveFirstStress, setFlags = a.introSaveFirstFlags))
        )
    )

    map["startup_pitch"] = event(
        id = "startup_pitch",
        flavor = pitchFlavor,
        message = Strings["evt_aidar_${era}_startup_pitch_msg"],
        options = listOf(
            option("join_startup", Strings["evt_aidar_${era}_startup_pitch_opt_join_startup"],
                when (eraId) { "kz_2005" -> "🎲"; else -> "🚀" },
                "startup_3months",
                Effect(capitalDelta = a.startupJoinCapital, stressDelta = a.startupJoinStress,
                    knowledgeDelta = a.startupJoinKnowledge, riskDelta = a.startupJoinRisk,
                    scheduleEvent = ScheduledEvent("startup_aftershock", a.startupJoinAfterMonths))),
            option("partial_startup", Strings["evt_aidar_${era}_startup_pitch_opt_partial_startup"],
                when (eraId) { "kz_2005" -> "🛠️"; else -> "💻" },
                "senior_promotion",
                Effect(stressDelta = a.startupPartialStress, knowledgeDelta = a.startupPartialKnowledge,
                    setFlags = setOf("aidar.side_hustle"))),
            option("skip_startup", Strings["evt_aidar_${era}_startup_pitch_opt_skip_startup"],
                when (eraId) { "kz_2024" -> "🧱"; else -> "🛡️" },
                "senior_promotion",
                Effect(knowledgeDelta = a.startupSkipKnowledge, stressDelta = a.startupSkipStress,
                    setFlags = a.startupSkipFlags))
        )
    )

    map["startup_3months"] = event(
        id = "startup_3months",
        flavor = pitch3Flavor,
        message = Strings["evt_aidar_${era}_startup_3months_msg"],
        options = listOf(
            option("pitch_investors", Strings["evt_aidar_${era}_startup_3months_opt_pitch_investors"],
                when (eraId) { "kz_2005" -> "🚀"; else -> "🎤" },
                "startup_result",
                Effect(stressDelta = a.startup3PitchStress, knowledgeDelta = a.startup3PitchKnowledge,
                    riskDelta = a.startup3PitchRisk)),
            option("exit_startup", Strings["evt_aidar_${era}_startup_3months_opt_exit_startup"],
                "🚪",
                "senior_promotion",
                Effect(capitalDelta = a.startup3ExitCapital, stressDelta = a.startup3ExitStress,
                    knowledgeDelta = a.startup3ExitKnowledge))
        )
    )

    map["startup_result"] = event(
        id = "startup_result",
        flavor = resultFlavor,
        message = Strings["evt_aidar_${era}_startup_result_msg"],
        options = listOf(
            option("strong_pitch", Strings["evt_aidar_${era}_startup_result_opt_strong_pitch"],
                when (eraId) { "kz_2005" -> "💼"; "kz_2015" -> "💪"; else -> "🔥" },
                a.startupResultStrongNext,
                Effect(incomeDelta = a.startupResultStrongIncome, stressDelta = a.startupResultStrongStress,
                    knowledgeDelta = a.startupResultStrongKnowledge, setFlags = a.startupResultStrongFlags)),
            option("safe_exit", Strings["evt_aidar_${era}_startup_result_opt_safe_exit"],
                when (eraId) { "kz_2024" -> "🧭"; else -> "🔄" },
                "senior_promotion",
                Effect(stressDelta = a.startupResultSafeStress, knowledgeDelta = a.startupResultSafeKnowledge,
                    setFlags = a.startupResultSafeFlags))
        )
    )

    if (a.hasStartupSuccess) {
        map["startup_success"] = event(
            id = "startup_success",
            flavor = successFlavor,
            message = Strings["evt_aidar_${era}_startup_success_msg"],
            options = listOf(
                option("quit_job", Strings["evt_aidar_${era}_startup_success_opt_quit_job"], "🦅", MONTHLY_TICK,
                    Effect(capitalDelta = a.startupSuccessCapital, incomeDelta = a.startupSuccessQuitIncomeDelta,
                        stressDelta = a.startupSuccessQuitStress, knowledgeDelta = a.startupSuccessQuitKnowledge,
                        setFlags = setOf("aidar.quit.for.startup"))),
                option("keep_job", Strings["evt_aidar_${era}_startup_success_opt_keep_job"], "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = a.startupSuccessCapital, stressDelta = a.startupSuccessKeepStress,
                        knowledgeDelta = a.startupSuccessKeepKnowledge, setFlags = setOf("aidar.double.load")))
            )
        )
    }

    map["senior_promotion"] = event(
        id = "senior_promotion",
        flavor = seniorFlavor,
        message = Strings["evt_aidar_${era}_senior_promotion_msg"],
        options = listOf(
            option("middle_promo", Strings["evt_aidar_${era}_senior_promotion_opt_middle_promo"],
                when (eraId) { "kz_2015" -> "🧗"; else -> "🧱" },
                MONTHLY_TICK,
                Effect(incomeDelta = a.seniorMiddleIncome, stressDelta = a.seniorMiddleStress, knowledgeDelta = 8)),
            option("senior_astana", Strings["evt_aidar_${era}_senior_promotion_opt_senior_astana"],
                when (eraId) { "kz_2024" -> "🚄"; else -> "🏙️" },
                a.seniorAstanaNext,
                Effect(incomeDelta = a.seniorAstanaIncome, expensesDelta = a.seniorAstanaExpenses,
                    stressDelta = a.seniorAstanaStress, knowledgeDelta = a.seniorAstanaKnowledge,
                    riskDelta = a.seniorAstanaRisk, setFlags = a.seniorAstanaFlags)),
            option("stay_same", Strings["evt_aidar_${era}_senior_promotion_opt_stay_same"],
                when (eraId) { "kz_2005" -> "🔍"; else -> "🕰️" },
                MONTHLY_TICK,
                Effect(knowledgeDelta = a.seniorStaySameKnowledge, stressDelta = a.seniorStaySameStress))
        )
    )
}

fun aidarPoolArc(): EventArc = EventArc { map ->
    map["freelance_order"] = event(
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
    )
    map["family_pressure"] = event(
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
                Effect(capitalDelta = -40_000L, stressDelta = 14, setFlags = setOf("aidar.guilt")))
        )
    )
    map["mortgage_offer"] = event(
        id = "mortgage_offer",
        flavor = "🏠",
        poolWeight = 6,
        tags = setOf("mortgage"),
        conditions = listOf(cond(CAPITAL, GTE, 2_400_000L), Condition.NotFlag("aidar.has.mortgage")),
        message = Strings["evt_aidar_2024_mortgage_offer_msg"],
        options = listOf(
            option("take_mortgage", Strings["evt_aidar_2024_mortgage_offer_opt_take_mortgage"], "🔑", MONTHLY_TICK,
                Effect(capitalDelta = -2_400_000L, debtDelta = 9_600_000L, expensesDelta = 85_000L,
                    stressDelta = 16, knowledgeDelta = 4, setFlags = setOf("aidar.has.mortgage"))),
            option("skip_mortgage", Strings["evt_aidar_2024_mortgage_offer_opt_skip_mortgage"], "⏳", MONTHLY_TICK,
                Effect(knowledgeDelta = 4, stressDelta = -2))
        )
    )
    map["startup_aftershock"] = event(
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
    )
    map["normal_life"] = event(
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
    )
}

fun aidarEndingsArc(): EventArc = EventArc { map ->
    map["ending_startup_king"] = event(
        id = "ending_startup_king",
        isEnding = true,
        endingType = EndingType.WEALTH,
        flavor = "🚀",
        message = Strings["evt_aidar_2024_ending_startup_king_msg"],
        options = emptyList()
    )
    map["ending_senior_dev"] = event(
        id = "ending_senior_dev",
        isEnding = true,
        endingType = EndingType.FINANCIAL_STABILITY,
        flavor = "💼",
        message = Strings["evt_aidar_2024_ending_senior_dev_msg"],
        options = emptyList()
    )
    map["ending_freedom"] = event(
        id = "ending_freedom",
        isEnding = true,
        endingType = EndingType.FINANCIAL_FREEDOM,
        flavor = "🏖️",
        message = Strings["evt_aidar_2024_ending_freedom_msg"],
        options = emptyList()
    )
    map["ending_broke"] = event(
        id = "ending_broke",
        isEnding = true,
        endingType = EndingType.BANKRUPTCY,
        flavor = "💀",
        message = Strings["evt_aidar_2024_ending_broke_msg"],
        options = emptyList()
    )
    map["ending_paycheck"] = event(
        id = "ending_paycheck",
        isEnding = true,
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        flavor = "😐",
        message = Strings["evt_aidar_2024_ending_paycheck_msg"],
        options = emptyList()
    )
}

fun commonAidarConditionals(): List<GameEvent> = listOf(
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
        options = listOf(
            option("accept_broke", Strings["evt_aidar_2024_ending_broke_trigger_opt_accept_broke"], "💀", "ending_broke")
        )
    ),
    event(
        id = "ending_freedom_trigger",
        priority = 4,
        unique = true,
        flavor = "🌅",
        conditions = listOf(cond(CAPITAL, GTE, 8_000_000L), cond(KNOWLEDGE, GTE, 60L)),
        message = Strings["evt_aidar_2024_ending_freedom_trigger_msg"],
        options = listOf(
            option("claim_freedom", Strings["evt_aidar_2024_ending_freedom_trigger_opt_claim_freedom"], "🏖️", "ending_freedom")
        )
    ),
    event(
        id = "ending_stability_trigger",
        priority = 3,
        unique = true,
        conditions = listOf(cond(CAPITAL, GTE, 3_000_000L), cond(STRESS, LTE, 45L)),
        message = Strings["evt_aidar_2024_ending_stability_trigger_msg"],
        options = listOf(
            option("claim_stability", Strings["evt_aidar_2024_ending_stability_trigger_opt_claim_stability"], "💼", "ending_senior_dev")
        )
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
        options = listOf(
            option("claim_wealth", Strings["evt_aidar_2024_ending_wealth_trigger_opt_claim_wealth"], "🚀", "ending_startup_king")
        )
    ),
    event(
        id = "ending_paycheck_trigger",
        priority = 1,
        unique = true,
        conditions = listOf(cond(CAPITAL, LTE, 120_000L), cond(KNOWLEDGE, LTE, 25L)),
        message = Strings["evt_aidar_2024_ending_paycheck_trigger_msg"],
        options = listOf(
            option("accept_paycheck", Strings["evt_aidar_2024_ending_paycheck_trigger_opt_accept_paycheck"], "😐", "ending_paycheck")
        )
    )
)

fun aidarEventPool(eraId: String): List<PoolEntry> = buildList {
    add(PoolEntry("normal_life", 18))
    add(PoolEntry("family_pressure", 8))
    if (eraId != "kz_2005") add(PoolEntry("mortgage_offer", 6))
    add(PoolEntry("freelance_order", 7))
    addAll(ScamEventLibrary.poolEntries)
}
