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
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option

fun erbolatMainArc(eraId: String, a: ErbolatEraAmounts): EventArc = EventArc { map ->
    val era = eraId.eraLabel()

    val introFlavor     = when (eraId) { "kz_2015" -> "💼"; else -> "📦" }
    val franchiseFlavor = when (eraId) { "kz_2015" -> "🤝"; else -> "🌐" }
    val resultFlavor    = when (eraId) { "kz_2015" -> "📊"; else -> "📈" }
    val ecommerceFlavor = when (eraId) { "kz_2015" -> "🛍️"; else -> "💻" }

    map["intro"] = event(
        id = "intro",
        flavor = introFlavor,
        message = Strings["evt_erbolat_${era}_intro_msg"],
        options = listOf(
            option("close_second_store", Strings["evt_erbolat_${era}_intro_opt_close_second_store"],
                "🔒",
                "ecommerce_pivot",
                Effect(incomeDelta = a.introCloseIncome, expensesDelta = a.introCloseExpenses,
                    stressDelta = a.introCloseStress, knowledgeDelta = a.introCloseKnowledge)),
            option("fight_competition", Strings["evt_erbolat_${era}_intro_opt_fight_competition"],
                when (eraId) { "kz_2015" -> "⚡"; else -> "⚔️" },
                "franchise_offer",
                Effect(capitalDelta = a.introFightCapital, incomeDelta = a.introFightIncome,
                    stressDelta = a.introFightStress, knowledgeDelta = 5)),
            option("wait_and_see", Strings["evt_erbolat_${era}_intro_opt_wait_and_see"],
                when (eraId) { "kz_2015" -> "⏳"; else -> "🕰️" },
                "supplier_scam",
                Effect(stressDelta = when (eraId) { "kz_2015" -> 8; else -> 10 }))
        )
    )

    map["franchise_offer"] = event(
        id = "franchise_offer",
        flavor = franchiseFlavor,
        message = Strings["evt_erbolat_${era}_franchise_offer_msg"],
        options = listOf(
            option("take_franchise", Strings["evt_erbolat_${era}_franchise_offer_opt_take_franchise"],
                "🚀",
                "franchise_result",
                Effect(capitalDelta = a.franchiseTakeCapital, debtDelta = a.franchiseTakeDebt,
                    incomeDelta = a.franchiseTakeIncome, expensesDelta = a.franchiseTakeExpenses,
                    stressDelta = 18, knowledgeDelta = a.franchiseTakeKnowledge)),
            option("skip_franchise", Strings["evt_erbolat_${era}_franchise_offer_opt_skip_franchise"],
                "🛡️",
                "ecommerce_pivot",
                Effect(knowledgeDelta = when (eraId) { "kz_2015" -> 5; else -> 6 }, stressDelta = -3)),
            option("negotiate_franchise", Strings["evt_erbolat_${era}_franchise_offer_opt_negotiate_franchise"],
                when (eraId) { "kz_2015" -> "🗣️"; else -> "🤝" },
                "ecommerce_pivot",
                Effect(capitalDelta = a.franchiseNegotiateCapital, debtDelta = a.franchiseNegotiateDebt,
                    incomeDelta = a.franchiseNegotiateIncome, stressDelta = 8, knowledgeDelta = 8))
        )
    )

    map["franchise_result"] = event(
        id = "franchise_result",
        flavor = resultFlavor,
        message = Strings["evt_erbolat_${era}_franchise_result_msg"],
        options = listOf(
            option("third_store", Strings["evt_erbolat_${era}_franchise_result_opt_third_store"],
                "🏬",
                MONTHLY_TICK,
                Effect(debtDelta = a.resultThirdDebt, incomeDelta = a.resultThirdIncome,
                    expensesDelta = a.resultThirdExpenses, stressDelta = a.resultThirdStress)),
            option("master_franchise", Strings["evt_erbolat_${era}_franchise_result_opt_master_franchise"],
                when (eraId) { "kz_2015" -> "🌐"; else -> "🌍" },
                MONTHLY_TICK,
                Effect(debtDelta = a.resultMasterDebt, incomeDelta = a.resultMasterIncome,
                    stressDelta = a.resultMasterStress, knowledgeDelta = 14)),
            option("hold_position", Strings["evt_erbolat_${era}_franchise_result_opt_hold_position"],
                when (eraId) { "kz_2015" -> "🎯"; else -> "🧭" },
                MONTHLY_TICK,
                Effect(stressDelta = a.resultHoldStress, knowledgeDelta = a.resultHoldKnowledge))
        )
    )

    map["ecommerce_pivot"] = event(
        id = "ecommerce_pivot",
        flavor = ecommerceFlavor,
        message = Strings["evt_erbolat_${era}_ecommerce_pivot_msg"],
        options = listOf(
            option("kaspi_marketplace", Strings["evt_erbolat_${era}_ecommerce_pivot_opt_kaspi_marketplace"],
                when (eraId) { "kz_2015" -> "🛒"; else -> "🛍️" },
                MONTHLY_TICK,
                Effect(capitalDelta = a.kaspiCapital, incomeDelta = a.kaspiIncome,
                    stressDelta = a.kaspiStress, knowledgeDelta = a.kaspiKnowledge,
                    setFlags = setOf("erbolat.digital"))),
            option("own_website", Strings["evt_erbolat_${era}_ecommerce_pivot_opt_own_website"],
                "🌐",
                MONTHLY_TICK,
                Effect(capitalDelta = a.websiteCapital, incomeDelta = a.websiteIncome,
                    stressDelta = 12, knowledgeDelta = a.websiteKnowledge,
                    setFlags = setOf("erbolat.digital"))),
            option("both_channels", Strings["evt_erbolat_${era}_ecommerce_pivot_opt_both_channels"],
                "⚡",
                MONTHLY_TICK,
                Effect(capitalDelta = a.bothCapital, incomeDelta = a.bothIncome,
                    stressDelta = a.bothStress, knowledgeDelta = 18,
                    setFlags = setOf("erbolat.digital")))
        )
    )
}

fun erbolatHubArc(): EventArc = EventArc { map ->
    map["supplier_scam"] = event(
        id = "supplier_scam",
        flavor = "⚠️",
        message = Strings["evt_erbolat_common_supplier_scam_msg"],
        options = listOf(
            option("pay_supplier", Strings["evt_erbolat_common_supplier_scam_opt_pay_supplier"], "💸", "supplier_result",
                Effect(capitalDelta = -800_000L, stressDelta = 10)),
            option("check_supplier", Strings["evt_erbolat_common_supplier_scam_opt_check_supplier"], "🔍", "supplier_safe",
                Effect(capitalDelta = -40_000L, knowledgeDelta = 8)),
            option("skip_supplier", Strings["evt_erbolat_common_supplier_scam_opt_skip_supplier"], "🛡️", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    )
    map["supplier_result"] = event(
        id = "supplier_result",
        flavor = "💀",
        message = Strings["evt_erbolat_common_supplier_result_msg"],
        options = listOf(
            option("cut_losses", Strings["evt_erbolat_common_supplier_result_opt_cut_losses"], "💪", "ecommerce_pivot",
                Effect(stressDelta = 24, knowledgeDelta = 10)),
            option("sue_supplier", Strings["evt_erbolat_common_supplier_result_opt_sue_supplier"], "⚖️", MONTHLY_TICK,
                Effect(capitalDelta = -60_000L, stressDelta = 15, knowledgeDelta = 5))
        )
    )
    map["supplier_safe"] = event(
        id = "supplier_safe",
        flavor = "✅",
        message = Strings["evt_erbolat_common_supplier_safe_msg"],
        options = listOf(
            option("find_reliable", Strings["evt_erbolat_common_supplier_safe_opt_find_reliable"], "🤝", MONTHLY_TICK,
                Effect(incomeDelta = 60_000L, stressDelta = -10, knowledgeDelta = 5)),
            option("search_yourself", Strings["evt_erbolat_common_supplier_safe_opt_search_yourself"], "🔍", MONTHLY_TICK,
                Effect(knowledgeDelta = 8))
        )
    )
    map["normal_life"] = event(
        id = "normal_life",
        flavor = "📋",
        poolWeight = 20,
        message = Strings["evt_erbolat_common_normal_life_msg"],
        options = listOf(
            option("pay_debt", Strings["evt_erbolat_common_normal_life_opt_pay_debt"], "💳", MONTHLY_TICK,
                Effect(capitalDelta = -250_000L, debtDelta = -250_000L, stressDelta = -6, knowledgeDelta = 2)),
            option("marketing", Strings["evt_erbolat_common_normal_life_opt_marketing"], "📣", MONTHLY_TICK,
                Effect(capitalDelta = -120_000L, incomeDelta = 90_000L, knowledgeDelta = 5)),
            option("inventory", Strings["evt_erbolat_common_normal_life_opt_inventory"], "📦", MONTHLY_TICK,
                Effect(capitalDelta = -320_000L, incomeDelta = 130_000L, stressDelta = 5)),
            option("finance_course", Strings["evt_erbolat_common_normal_life_opt_finance_course"], "🎓", MONTHLY_TICK,
                Effect(capitalDelta = -60_000L, knowledgeDelta = 12, stressDelta = -4))
        )
    )
}

fun erbolatEndingsArc(): EventArc = EventArc { map ->
    map["ending_empire"] = event(
        id = "ending_empire",
        flavor = "🏆",
        isEnding = true,
        endingType = EndingType.WEALTH,
        message = Strings["evt_erbolat_common_ending_empire_msg"],
        options = emptyList()
    )
    map["ending_pivot_success"] = event(
        id = "ending_pivot_success",
        flavor = "💻",
        isEnding = true,
        endingType = EndingType.FINANCIAL_FREEDOM,
        message = Strings["evt_erbolat_common_ending_pivot_success_msg"],
        options = emptyList()
    )
    map["ending_stable_business"] = event(
        id = "ending_stable_business",
        flavor = "🏪",
        isEnding = true,
        endingType = EndingType.FINANCIAL_STABILITY,
        message = Strings["evt_erbolat_common_ending_stable_business_msg"],
        options = emptyList()
    )
    map["ending_bankruptcy"] = event(
        id = "ending_bankruptcy",
        flavor = "💀",
        isEnding = true,
        endingType = EndingType.BANKRUPTCY,
        message = Strings["evt_erbolat_common_ending_bankruptcy_msg"],
        options = emptyList()
    )
    map["ending_paycheck"] = event(
        id = "ending_paycheck",
        flavor = "😰",
        isEnding = true,
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        message = Strings["evt_erbolat_common_ending_paycheck_msg"],
        options = emptyList()
    )
}

fun commonErbolatConditionals(): List<GameEvent> = listOf(
    event(
        id = "debt_crisis",
        priority = 10,
        flavor = "🚨",
        conditions = listOf(cond(DEBT, GT, 3_000_000L), cond(CAPITAL, LTE, 700_000L)),
        message = Strings["evt_erbolat_common_debt_crisis_msg"],
        options = listOf(
            option("sell_store", Strings["evt_erbolat_common_debt_crisis_opt_sell_store"], "🔒", MONTHLY_TICK,
                Effect(capitalDelta = 1_500_000L, debtDelta = -1_500_000L, incomeDelta = -280_000L,
                    expensesDelta = -180_000L, stressDelta = 10)),
            option("bank_restructure", Strings["evt_erbolat_common_debt_crisis_opt_bank_restructure"], "🏦", MONTHLY_TICK,
                Effect(expensesDelta = -50_000L, stressDelta = 20, knowledgeDelta = 8)),
            option("find_investor", Strings["evt_erbolat_common_debt_crisis_opt_find_investor"], "🤝", MONTHLY_TICK,
                Effect(capitalDelta = 2_000_000L, stressDelta = 14, knowledgeDelta = 10))
        )
    ),
    event(
        id = "burnout_warning",
        priority = 8,
        flavor = "😮‍💨",
        conditions = listOf(cond(STRESS, GTE, 82L)),
        message = Strings["evt_erbolat_common_burnout_warning_msg"],
        options = listOf(
            option("family_vacation", Strings["evt_erbolat_common_burnout_warning_opt_family_vacation"], "✈️", MONTHLY_TICK,
                Effect(capitalDelta = -300_000L, stressDelta = -38)),
            option("delegate", Strings["evt_erbolat_common_burnout_warning_opt_delegate"], "👤", MONTHLY_TICK,
                Effect(expensesDelta = 150_000L, stressDelta = -24, knowledgeDelta = 8))
        )
    ),
    event(
        id = "investment_unlock",
        priority = 5,
        flavor = "💡",
        unique = true,
        conditions = listOf(cond(KNOWLEDGE, GTE, 52L)),
        message = Strings["evt_erbolat_common_investment_unlock_msg"],
        options = listOf(
            option("use_factoring", Strings["evt_erbolat_common_investment_unlock_opt_use_factoring"], "⚡", MONTHLY_TICK,
                Effect(capitalDelta = 500_000L, expensesDelta = 10_000L, knowledgeDelta = 5)),
            option("skip_factoring", Strings["evt_erbolat_common_investment_unlock_opt_skip_factoring"], "🛡️", MONTHLY_TICK,
                Effect())
        )
    ),
    event(
        id = "ending_bankruptcy_trigger",
        priority = 100,
        conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
        message = Strings["evt_erbolat_common_ending_bankruptcy_trigger_msg"],
        options = listOf(
            option("claim_bankruptcy", Strings["evt_erbolat_common_ending_bankruptcy_trigger_opt_claim_bankruptcy"], "💀", "ending_bankruptcy")
        )
    ),
    event(
        id = "ending_wealth_trigger",
        priority = 2,
        unique = true,
        conditions = listOf(cond(CAPITAL, GTE, 18_000_000L)),
        message = Strings["evt_erbolat_common_ending_wealth_trigger_msg"],
        options = listOf(
            option("claim_empire", Strings["evt_erbolat_common_ending_wealth_trigger_opt_claim_empire"], "🏆", "ending_empire")
        )
    ),
    event(
        id = "ending_freedom_trigger",
        priority = 3,
        unique = true,
        conditions = listOf(cond(CAPITAL, GTE, 9_000_000L), Condition.HasFlag("erbolat.digital")),
        message = Strings["evt_erbolat_common_ending_freedom_trigger_msg"],
        options = listOf(
            option("claim_pivot", Strings["evt_erbolat_common_ending_freedom_trigger_opt_claim_pivot"], "💻", "ending_pivot_success")
        )
    ),
    event(
        id = "ending_stability_trigger",
        priority = 4,
        unique = true,
        conditions = listOf(cond(CAPITAL, GTE, 3_500_000L), cond(STRESS, LTE, 55L)),
        message = Strings["evt_erbolat_common_ending_stability_trigger_msg"],
        options = listOf(
            option("claim_stability", Strings["evt_erbolat_common_ending_stability_trigger_opt_claim_stability"], "🏪", "ending_stable_business")
        )
    ),
    event(
        id = "ending_paycheck_trigger",
        priority = 1,
        unique = true,
        conditions = listOf(cond(DEBT, GT, 2_000_000L), cond(CAPITAL, LTE, 300_000L)),
        message = Strings["evt_erbolat_common_ending_paycheck_trigger_msg"],
        options = listOf(
            option("claim_paycheck", Strings["evt_erbolat_common_ending_paycheck_trigger_opt_claim_paycheck"], "😰", "ending_paycheck")
        )
    )
)

fun erbolatEventPool(): List<PoolEntry> = listOf(
    PoolEntry("normal_life", 16),
    PoolEntry("supplier_scam", 10),
    PoolEntry("franchise_offer", 6)
) + ScamEventLibrary.poolEntries
