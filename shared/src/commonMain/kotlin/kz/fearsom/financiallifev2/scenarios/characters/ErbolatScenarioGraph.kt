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
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option
import kz.fearsom.financiallifev2.scenarios.story
import kz.fearsom.financiallifev2.i18n.Strings

class ErbolatScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = when (eraId) {
        "kz_2015" -> PlayerState(
            capital = 2_800_000L,
            income = 780_000L,
            expenses = 650_000L,
            debt = 3_600_000L,
            debtPaymentMonthly = 100_000L,
            investments = 0L,
            investmentReturnRate = 0.08,
            stress = 64,
            financialKnowledge = 34,
            riskLevel = 48,
            month = 1,
            year = 2015,
            characterId = "erbolat",
            eraId = eraId
        )
        else -> PlayerState(
            capital = 4_200_000L,
            income = 1_050_000L,
            expenses = 860_000L,
            debt = 4_000_000L,
            debtPaymentMonthly = 111_111L,
            investments = 0L,
            investmentReturnRate = 0.10,
            stress = 66,
            financialKnowledge = 38,
            riskLevel = 52,
            month = 1,
            year = 2024,
            characterId = "erbolat",
            eraId = eraId
        )
    }

    override val events: Map<String, GameEvent> = when (eraId) {
        "kz_2015" -> erbolat2015Events()
        else -> erbolat2024Events()
    }

    override val conditionalEvents: List<GameEvent> = when (eraId) {
        "kz_2015" -> commonErbolatConditionals(
            burnoutIntro = Strings["evt_erbolat_kz2015_burnout_label"],
            financeIntro = Strings["evt_erbolat_kz2015_finance_label"]
        )
        else -> commonErbolatConditionals(
            burnoutIntro = Strings["evt_erbolat_kz2024_burnout_label"],
            financeIntro = Strings["evt_erbolat_kz2024_finance_label"]
        )
    }

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 16),
        PoolEntry("supplier_scam", 10),
        PoolEntry("franchise_offer", 6)
    ) + ScamEventLibrary.poolEntries

    private fun erbolat2015Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "💼",
            message = Strings["evt_erbolat_2015_intro_msg"],
            options = listOf(
                option("close_second_store", Strings["evt_erbolat_2015_intro_opt_close_second_store"], "🔒", "ecommerce_pivot",
                    Effect(incomeDelta = -220_000L, expensesDelta = -180_000L, stressDelta = -8, knowledgeDelta = 4)),
                option("fight_competition", Strings["evt_erbolat_2015_intro_opt_fight_competition"], "⚡", "franchise_offer",
                    Effect(capitalDelta = -180_000L, incomeDelta = 50_000L, stressDelta = 12, knowledgeDelta = 5)),
                option("wait_and_see", Strings["evt_erbolat_2015_intro_opt_wait_and_see"], "⏳", "supplier_scam",
                    Effect(stressDelta = 8))
            )
        ))
        put("franchise_offer", event(
            id = "franchise_offer",
            flavor = "🤝",
            message = Strings["evt_erbolat_2015_franchise_offer_msg"],
            options = listOf(
                option("take_franchise", Strings["evt_erbolat_2015_franchise_offer_opt_take_franchise"], "🚀", "franchise_result",
                    Effect(capitalDelta = -1_500_000L, debtDelta = 2_000_000L, incomeDelta = 180_000L, expensesDelta = 50_000L, stressDelta = 18, knowledgeDelta = 8)),
                option("skip_franchise", Strings["evt_erbolat_2015_franchise_offer_opt_skip_franchise"], "🛡️", "ecommerce_pivot",
                    Effect(knowledgeDelta = 5, stressDelta = -3)),
                option("negotiate_franchise", Strings["evt_erbolat_2015_franchise_offer_opt_negotiate_franchise"], "🗣️", "ecommerce_pivot",
                    Effect(capitalDelta = -900_000L, debtDelta = 1_000_000L, incomeDelta = 90_000L, stressDelta = 8, knowledgeDelta = 8))
            )
        ))
        put("franchise_result", event(
            id = "franchise_result",
            flavor = "📊",
            message = Strings["evt_erbolat_2015_franchise_result_msg"],
            options = listOf(
                option("third_store", Strings["evt_erbolat_2015_franchise_result_opt_third_store"], "🏬", MONTHLY_TICK,
                    Effect(debtDelta = 3_000_000L, incomeDelta = 220_000L, expensesDelta = 180_000L, stressDelta = 22)),
                option("master_franchise", Strings["evt_erbolat_2015_franchise_result_opt_master_franchise"], "🌐", MONTHLY_TICK,
                    Effect(debtDelta = 8_000_000L, incomeDelta = 420_000L, stressDelta = 30, knowledgeDelta = 14)),
                option("hold_position", Strings["evt_erbolat_2015_franchise_result_opt_hold_position"], "🎯", MONTHLY_TICK,
                    Effect(stressDelta = -6, knowledgeDelta = 4))
            )
        ))
        put("ecommerce_pivot", event(
            id = "ecommerce_pivot",
            flavor = "🛍️",
            message = Strings["evt_erbolat_2015_ecommerce_pivot_msg"],
            options = listOf(
                option("kaspi_marketplace", Strings["evt_erbolat_2015_ecommerce_pivot_opt_kaspi_marketplace"], "🛒", MONTHLY_TICK,
                    Effect(capitalDelta = -150_000L, incomeDelta = 150_000L, stressDelta = 4, knowledgeDelta = 10,
                        setFlags = setOf("erbolat.digital"))),
                option("own_website", Strings["evt_erbolat_2015_ecommerce_pivot_opt_own_website"], "🌐", MONTHLY_TICK,
                    Effect(capitalDelta = -350_000L, incomeDelta = 90_000L, stressDelta = 12, knowledgeDelta = 14,
                        setFlags = setOf("erbolat.digital"))),
                option("both_channels", Strings["evt_erbolat_2015_ecommerce_pivot_opt_both_channels"], "⚡", MONTHLY_TICK,
                    Effect(capitalDelta = -650_000L, incomeDelta = 220_000L, stressDelta = 24, knowledgeDelta = 18,
                        setFlags = setOf("erbolat.digital")))
            )
        ))
        put("supplier_scam", commonSupplierScamEvent())
        commonErbolatHubAndEndings("2015")
    }

    private fun erbolat2024Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "📦",
            message = Strings["evt_erbolat_2024_intro_msg"],
            options = listOf(
                option("close_second_store", Strings["evt_erbolat_2024_intro_opt_close_second_store"], "🔒", "ecommerce_pivot",
                    Effect(incomeDelta = -260_000L, expensesDelta = -220_000L, stressDelta = -10, knowledgeDelta = 5)),
                option("fight_competition", Strings["evt_erbolat_2024_intro_opt_fight_competition"], "⚔️", "franchise_offer",
                    Effect(capitalDelta = -250_000L, incomeDelta = 60_000L, stressDelta = 14, knowledgeDelta = 5)),
                option("wait_and_see", Strings["evt_erbolat_2024_intro_opt_wait_and_see"], "🕰️", "supplier_scam",
                    Effect(stressDelta = 10))
            )
        ))
        put("franchise_offer", event(
            id = "franchise_offer",
            flavor = "🌐",
            message = Strings["evt_erbolat_2024_franchise_offer_msg"],
            options = listOf(
                option("take_franchise", Strings["evt_erbolat_2024_franchise_offer_opt_take_franchise"], "🚀", "franchise_result",
                    Effect(capitalDelta = -2_000_000L, debtDelta = 2_500_000L, incomeDelta = 220_000L, expensesDelta = 80_000L, stressDelta = 18, knowledgeDelta = 10)),
                option("skip_franchise", Strings["evt_erbolat_2024_franchise_offer_opt_skip_franchise"], "🛡️", "ecommerce_pivot",
                    Effect(knowledgeDelta = 6, stressDelta = -3)),
                option("negotiate_franchise", Strings["evt_erbolat_2024_franchise_offer_opt_negotiate_franchise"], "🤝", "ecommerce_pivot",
                    Effect(capitalDelta = -1_100_000L, debtDelta = 1_200_000L, incomeDelta = 110_000L, stressDelta = 8, knowledgeDelta = 8))
            )
        ))
        put("franchise_result", event(
            id = "franchise_result",
            flavor = "📈",
            message = Strings["evt_erbolat_2024_franchise_result_msg"],
            options = listOf(
                option("third_store", Strings["evt_erbolat_2024_franchise_result_opt_third_store"], "🏬", MONTHLY_TICK,
                    Effect(debtDelta = 3_500_000L, incomeDelta = 280_000L, expensesDelta = 230_000L, stressDelta = 24)),
                option("master_franchise", Strings["evt_erbolat_2024_franchise_result_opt_master_franchise"], "🌍", MONTHLY_TICK,
                    Effect(debtDelta = 10_000_000L, incomeDelta = 520_000L, stressDelta = 32, knowledgeDelta = 14)),
                option("hold_position", Strings["evt_erbolat_2024_franchise_result_opt_hold_position"], "🧭", MONTHLY_TICK,
                    Effect(stressDelta = -8, knowledgeDelta = 5))
            )
        ))
        put("ecommerce_pivot", event(
            id = "ecommerce_pivot",
            flavor = "💻",
            message = Strings["evt_erbolat_2024_ecommerce_pivot_msg"],
            options = listOf(
                option("kaspi_marketplace", Strings["evt_erbolat_2024_ecommerce_pivot_opt_kaspi_marketplace"], "🛍️", MONTHLY_TICK,
                    Effect(capitalDelta = -180_000L, incomeDelta = 170_000L, stressDelta = 2, knowledgeDelta = 12,
                        setFlags = setOf("erbolat.digital"))),
                option("own_website", Strings["evt_erbolat_2024_ecommerce_pivot_opt_own_website"], "🌐", MONTHLY_TICK,
                    Effect(capitalDelta = -550_000L, incomeDelta = 120_000L, stressDelta = 12, knowledgeDelta = 16,
                        setFlags = setOf("erbolat.digital"))),
                option("both_channels", Strings["evt_erbolat_2024_ecommerce_pivot_opt_both_channels"], "⚡", MONTHLY_TICK,
                    Effect(capitalDelta = -800_000L, incomeDelta = 260_000L, stressDelta = 26, knowledgeDelta = 18,
                        setFlags = setOf("erbolat.digital")))
            )
        ))
        put("supplier_scam", commonSupplierScamEvent())
        commonErbolatHubAndEndings("2024")
    }

    private fun commonSupplierScamEvent() = event(
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

    private fun MutableMap<String, GameEvent>.commonErbolatHubAndEndings(eraLabel: String) {
        put("supplier_result", event(
            id = "supplier_result",
            flavor = "💀",
            message = Strings["evt_erbolat_common_supplier_result_msg"],
            options = listOf(
                option("cut_losses", Strings["evt_erbolat_common_supplier_result_opt_cut_losses"], "💪", "ecommerce_pivot",
                    Effect(stressDelta = 24, knowledgeDelta = 10)),
                option("sue_supplier", Strings["evt_erbolat_common_supplier_result_opt_sue_supplier"], "⚖️", MONTHLY_TICK,
                    Effect(capitalDelta = -60_000L, stressDelta = 15, knowledgeDelta = 5))
            )
        ))
        put("supplier_safe", event(
            id = "supplier_safe",
            flavor = "✅",
            message = Strings["evt_erbolat_common_supplier_safe_msg"],
            options = listOf(
                option("find_reliable", Strings["evt_erbolat_common_supplier_safe_opt_find_reliable"], "🤝", MONTHLY_TICK,
                    Effect(incomeDelta = 60_000L, stressDelta = -10, knowledgeDelta = 5)),
                option("search_yourself", Strings["evt_erbolat_common_supplier_safe_opt_search_yourself"], "🔍", MONTHLY_TICK,
                    Effect(knowledgeDelta = 8))
            )
        ))
        put("normal_life", event(
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
        ))
        put("ending_empire", event(
            id = "ending_empire",
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.WEALTH,
            message = Strings["evt_erbolat_common_ending_empire_msg"],
            options = emptyList()
        ))
        put("ending_pivot_success", event(
            id = "ending_pivot_success",
            flavor = "💻",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            message = Strings["evt_erbolat_common_ending_pivot_success_msg"],
            options = emptyList()
        ))
        put("ending_stable_business", event(
            id = "ending_stable_business",
            flavor = "🏪",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            message = Strings["evt_erbolat_common_ending_stable_business_msg"],
            options = emptyList()
        ))
        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            message = Strings["evt_erbolat_common_ending_bankruptcy_msg"],
            options = emptyList()
        ))
        put("ending_paycheck", event(
            id = "ending_paycheck",
            flavor = "😰",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            message = Strings["evt_erbolat_common_ending_paycheck_msg"],
            options = emptyList()
        ))
    }

    private fun commonErbolatConditionals(
        burnoutIntro: String,
        financeIntro: String
    ): List<GameEvent> = listOf(
        event(
            id = "debt_crisis",
            priority = 10,
            flavor = "🚨",
            conditions = listOf(cond(DEBT, GT, 3_000_000L), cond(CAPITAL, LTE, 700_000L)),
            message = Strings["evt_erbolat_common_debt_crisis_msg"],
            options = listOf(
                option("sell_store", Strings["evt_erbolat_common_debt_crisis_opt_sell_store"], "🔒", MONTHLY_TICK,
                    Effect(capitalDelta = 1_500_000L, debtDelta = -1_500_000L, incomeDelta = -280_000L, expensesDelta = -180_000L, stressDelta = 10)),
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
            options = listOf(option("claim_bankruptcy", Strings["evt_erbolat_common_ending_bankruptcy_trigger_opt_claim_bankruptcy"], "💀", "ending_bankruptcy"))
        ),
        event(
            id = "ending_wealth_trigger",
            priority = 2,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 18_000_000L)),
            message = Strings["evt_erbolat_common_ending_wealth_trigger_msg"],
            options = listOf(option("claim_empire", Strings["evt_erbolat_common_ending_wealth_trigger_opt_claim_empire"], "🏆", "ending_empire"))
        ),
        event(
            id = "ending_freedom_trigger",
            priority = 3,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 9_000_000L), Condition.HasFlag("erbolat.digital")),
            message = Strings["evt_erbolat_common_ending_freedom_trigger_msg"],
            options = listOf(option("claim_pivot", Strings["evt_erbolat_common_ending_freedom_trigger_opt_claim_pivot"], "💻", "ending_pivot_success"))
        ),
        event(
            id = "ending_stability_trigger",
            priority = 4,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 3_500_000L), cond(STRESS, LTE, 55L)),
            message = Strings["evt_erbolat_common_ending_stability_trigger_msg"],
            options = listOf(option("claim_stability", Strings["evt_erbolat_common_ending_stability_trigger_opt_claim_stability"], "🏪", "ending_stable_business"))
        ),
        event(
            id = "ending_paycheck_trigger",
            priority = 1,
            unique = true,
            conditions = listOf(cond(DEBT, GT, 2_000_000L), cond(CAPITAL, LTE, 300_000L)),
            message = Strings["evt_erbolat_common_ending_paycheck_trigger_msg"],
            options = listOf(option("claim_paycheck", Strings["evt_erbolat_common_ending_paycheck_trigger_opt_claim_paycheck"], "😰", "ending_paycheck"))
        )
    )
}
