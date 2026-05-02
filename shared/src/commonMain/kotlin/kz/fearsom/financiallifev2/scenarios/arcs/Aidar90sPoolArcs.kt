package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option

fun aidar90sPoolArc(): EventArc = EventArc { map ->

    map["job_offer"] = event(
        id = "job_offer",
        message = Strings["hardcoded_aidar90s_017_msg"],
        flavor = "💼",
        poolWeight = 10,
        tags = setOf("career"),
        options = listOf(
            option("take_job", Strings["evt_aidar90s_job_offer_opt_take_job"], "✅", MONTHLY_TICK,
                Effect(incomeDelta = 10_000L, stressDelta = 10)),
            option("decline_job", Strings["evt_aidar90s_job_offer_opt_decline_job"], "❌", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = -5))
        )
    )

    map["health_issue"] = event(
        id = "health_issue",
        message = Strings["hardcoded_aidar90s_018_msg"],
        flavor = "🏥",
        poolWeight = 8,
        tags = setOf("crisis"),
        options = listOf(
            option("treat_health", Strings["evt_aidar90s_health_issue_opt_treat_health"], "💊", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, stressDelta = -15)),
            option("ignore_health", Strings["evt_aidar90s_health_issue_opt_ignore_health"], "🤷", MONTHLY_TICK,
                Effect(stressDelta = 10, knowledgeDelta = -5))
        )
    )

    map["friend_investment"] = event(
        id = "friend_investment",
        message = Strings["hardcoded_aidar90s_019_msg"],
        flavor = "🤝",
        poolWeight = 12,
        tags = setOf("investment", "scam"),
        options = listOf(
            option("invest_friend", Strings["evt_aidar90s_friend_investment_opt_invest_friend"], "💰", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, riskDelta = 15)),
            option("decline_friend", Strings["evt_aidar90s_friend_investment_opt_decline_friend"], "🛑", MONTHLY_TICK,
                Effect(knowledgeDelta = 5, stressDelta = 5))
        )
    )

    map["black_market"] = event(
        id = "black_market",
        message = Strings["hardcoded_aidar90s_020_msg"],
        flavor = "🌑",
        poolWeight = 10,
        tags = setOf("investment", "adventure"),
        options = listOf(
            option("buy_black_usd", Strings["evt_aidar90s_black_market_opt_buy_black_usd"], "🤫", MONTHLY_TICK,
                Effect(capitalDelta = -15_000L, investmentsDelta = 18_000L, riskDelta = 25)),
            option("buy_official_usd", Strings["evt_aidar90s_black_market_opt_buy_official_usd"], "🏦", MONTHLY_TICK,
                Effect(capitalDelta = -18_000L, investmentsDelta = 18_000L, knowledgeDelta = 3))
        )
    )

    map["family_celebration"] = event(
        id = "family_celebration",
        message = Strings["hardcoded_aidar90s_021_msg"],
        flavor = "🎉",
        poolWeight = 15,
        tags = setOf("family"),
        options = listOf(
            option("give_gift", Strings["evt_aidar90s_family_celebration_opt_give_gift"], "🎁", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, stressDelta = -5, knowledgeDelta = 2)),
            option("skip_celebration", Strings["evt_aidar90s_family_celebration_opt_skip_celebration"], "🏠", MONTHLY_TICK,
                Effect(stressDelta = 10, knowledgeDelta = -3))
        )
    )

    map["utility_bills"] = event(
        id = "utility_bills",
        message = Strings["hardcoded_aidar90s_022_msg"],
        flavor = "💡",
        poolWeight = 18,
        tags = setOf("crisis"),
        options = listOf(
            option("pay_bills", Strings["evt_aidar90s_utility_bills_opt_pay_bills"], "💳", MONTHLY_TICK,
                Effect(expensesDelta = 8_000L, stressDelta = -5)),
            option("pay_partial", Strings["evt_aidar90s_utility_bills_opt_pay_partial"], "💰", MONTHLY_TICK,
                Effect(expensesDelta = 4_000L, stressDelta = 5)),
            option("skip_bills", Strings["evt_aidar90s_utility_bills_opt_skip_bills"], "⚠️", MONTHLY_TICK,
                Effect(stressDelta = 15, riskDelta = 10))
        )
    )

    map["education_opportunity"] = event(
        id = "education_opportunity",
        message = Strings["hardcoded_aidar90s_023_msg"],
        flavor = "🎓",
        poolWeight = 10,
        tags = setOf("career"),
        options = listOf(
            option("take_course", Strings["evt_aidar90s_education_opportunity_opt_take_course"], "📚", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, knowledgeDelta = 25, incomeDelta = 5_000L)),
            option("skip_course", Strings["evt_aidar90s_education_opportunity_opt_skip_course"], "❌", MONTHLY_TICK,
                Effect(stressDelta = -3))
        )
    )

    map["car_purchase"] = event(
        id = "car_purchase",
        message = Strings["hardcoded_aidar90s_024_msg"],
        flavor = "🚗",
        poolWeight = 8,
        tags = setOf("investment"),
        options = listOf(
            option("buy_car", Strings["evt_aidar90s_car_purchase_opt_buy_car"], "🔑", MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, incomeDelta = 15_000L, riskDelta = 10)),
            option("skip_car", Strings["evt_aidar90s_car_purchase_opt_skip_car"], "🚶", MONTHLY_TICK,
                Effect(knowledgeDelta = 3))
        )
    )

    map["apartment_rent"] = event(
        id = "apartment_rent",
        message = Strings["hardcoded_aidar90s_025_msg"],
        flavor = "🏠",
        poolWeight = 15,
        tags = setOf("crisis"),
        options = listOf(
            option("accept_raise", Strings["evt_aidar90s_apartment_rent_opt_accept_raise"], "😤", MONTHLY_TICK,
                Effect(expensesDelta = 5_000L, stressDelta = 10)),
            option("move_out", Strings["evt_aidar90s_apartment_rent_opt_move_out"], "📦", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, expensesDelta = -2_000L, stressDelta = 15))
        )
    )

    map["winter_prep"] = event(
        id = "winter_prep",
        message = Strings["hardcoded_aidar90s_026_msg"],
        flavor = "❄️",
        poolWeight = 12,
        tags = setOf("crisis"),
        options = listOf(
            option("buy_fuel", Strings["evt_aidar90s_winter_prep_opt_buy_fuel"], "🪵", MONTHLY_TICK,
                Effect(capitalDelta = -15_000L, stressDelta = -10)),
            option("risk_cold", Strings["evt_aidar90s_winter_prep_opt_risk_cold"], "🥶", MONTHLY_TICK,
                Effect(stressDelta = 20, riskDelta = 15))
        )
    )

    map["tax_inspection"] = event(
        id = "tax_inspection",
        message = Strings["hardcoded_aidar90s_027_msg"],
        flavor = "📋",
        poolWeight = 6,
        tags = setOf("crisis"),
        options = listOf(
            option("pay_fine", Strings["evt_aidar90s_tax_inspection_opt_pay_fine"], "💳", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, knowledgeDelta = 10)),
            option("bribe_tax", Strings["evt_aidar90s_tax_inspection_opt_bribe_tax"], "🤫", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, riskDelta = 30, stressDelta = 10))
        )
    )

    map["lottery_win"] = event(
        id = "lottery_win",
        message = Strings["hardcoded_aidar90s_028_msg"],
        flavor = "🎰",
        poolWeight = 5,
        tags = setOf("windfall"),
        options = listOf(
            option("save_win", Strings["evt_aidar90s_lottery_win_opt_save_win"], "💰", MONTHLY_TICK,
                Effect(capitalDelta = 10_000L, knowledgeDelta = 3)),
            option("spend_win", Strings["evt_aidar90s_lottery_win_opt_spend_win"], "🎁", MONTHLY_TICK,
                Effect(stressDelta = -10)),
            option("invest_win", Strings["evt_aidar90s_lottery_win_opt_invest_win"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 10_000L, knowledgeDelta = 5))
        )
    )

    map["old_friend_return"] = event(
        id = "old_friend_return",
        message = Strings["hardcoded_aidar90s_029_msg"],
        flavor = "👋",
        poolWeight = 8,
        tags = setOf("career", "investment"),
        options = listOf(
            option("partner_friend", Strings["evt_aidar90s_old_friend_return_opt_partner_friend"], "🤝", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, incomeDelta = 10_000L, riskDelta = 15)),
            option("decline_friend", Strings["evt_aidar90s_old_friend_return_opt_decline_friend"], "🙏", MONTHLY_TICK,
                Effect(knowledgeDelta = 5, stressDelta = -5))
        )
    )

    map["child_birth"] = event(
        id = "child_birth",
        message = Strings["hardcoded_aidar90s_030_msg"],
        flavor = "👶",
        poolWeight = 4,
        tags = setOf("family"),
        options = listOf(
            option("accept_child", Strings["evt_aidar90s_child_birth_opt_accept_child"], "💝", MONTHLY_TICK,
                Effect(expensesDelta = 10_000L, stressDelta = 10, knowledgeDelta = 5)),
            option("plan_budget", Strings["evt_aidar90s_child_birth_opt_plan_budget"], "📊", MONTHLY_TICK,
                Effect(expensesDelta = 10_000L, knowledgeDelta = 10))
        )
    )

    map["theft_victim"] = event(
        id = "theft_victim",
        message = Strings["hardcoded_aidar90s_031_msg"],
        flavor = "🚨",
        poolWeight = 6,
        tags = setOf("crisis"),
        options = listOf(
            option("report_theft", Strings["evt_aidar90s_theft_victim_opt_report_theft"], "👮", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, stressDelta = 10)),
            option("accept_loss", Strings["evt_aidar90s_theft_victim_opt_accept_loss"], "😔", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, knowledgeDelta = 5))
        )
    )

    map["currency_exchange"] = event(
        id = "currency_exchange",
        message = Strings["hardcoded_aidar90s_032_msg"],
        flavor = "💱",
        poolWeight = 12,
        tags = setOf("investment"),
        options = listOf(
            option("buy_usd_now", Strings["evt_aidar90s_currency_exchange_opt_buy_usd_now"], "💵", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, investmentsDelta = 20_000L)),
            option("wait_dip", Strings["evt_aidar90s_currency_exchange_opt_wait_dip"], "⏳", MONTHLY_TICK,
                Effect(knowledgeDelta = 5, stressDelta = 5))
        )
    )

    map["business_partner_betray"] = event(
        id = "business_partner_betray",
        message = Strings["hardcoded_aidar90s_033_msg"],
        flavor = "😡",
        poolWeight = 5,
        tags = setOf("crisis", "scam"),
        options = listOf(
            option("hunt_partner", Strings["evt_aidar90s_business_partner_betray_opt_hunt_partner"], "🔍", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, stressDelta = 25, riskDelta = 20)),
            option("write_off", Strings["evt_aidar90s_business_partner_betray_opt_write_off"], "📝", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, knowledgeDelta = 15))
        )
    )

    map["government_subsidy"] = event(
        id = "government_subsidy",
        message = Strings["hardcoded_aidar90s_034_msg"],
        flavor = "🏛️",
        poolWeight = 8,
        tags = setOf("investment"),
        options = listOf(
            option("apply_subsidy", Strings["evt_aidar90s_government_subsidy_opt_apply_subsidy"], "📋", MONTHLY_TICK,
                Effect(debtDelta = 100_000L, capitalDelta = 100_000L, knowledgeDelta = 10)),
            option("skip_subsidy", Strings["evt_aidar90s_government_subsidy_opt_skip_subsidy"], "❌", MONTHLY_TICK,
                Effect(stressDelta = -5))
        )
    )

    map["medical_emergency"] = event(
        id = "medical_emergency",
        message = Strings["hardcoded_aidar90s_035_msg"],
        flavor = "🚑",
        poolWeight = 4,
        tags = setOf("crisis", "family"),
        options = listOf(
            option("pay_medical", Strings["evt_aidar90s_medical_emergency_opt_pay_medical"], "💝", MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, stressDelta = -20, setFlags = setOf("saved_relative"))),
            option("partial_help", Strings["evt_aidar90s_medical_emergency_opt_partial_help"], "🤲", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, stressDelta = 10))
        )
    )

    map["new_year_bonus"] = event(
        id = "new_year_bonus",
        message = Strings["hardcoded_aidar90s_036_msg"],
        flavor = "🎄",
        poolWeight = 10,
        tags = setOf("windfall"),
        options = listOf(
            option("save_bonus", Strings["evt_aidar90s_new_year_bonus_opt_save_bonus"], "🛡️", MONTHLY_TICK,
                Effect(capitalDelta = 30_000L, knowledgeDelta = 5)),
            option("celebrate", Strings["evt_aidar90s_new_year_bonus_opt_celebrate"], "🎉", MONTHLY_TICK,
                Effect(capitalDelta = -15_000L, stressDelta = -20)),
            option("invest_bonus", Strings["evt_aidar90s_new_year_bonus_opt_invest_bonus"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 30_000L, knowledgeDelta = 8))
        )
    )

    map["apartment_purchase"] = event(
        id = "apartment_purchase",
        message = Strings["hardcoded_aidar90s_037_msg"],
        flavor = "🏠",
        poolWeight = 6,
        tags = setOf("investment"),
        options = listOf(
            option("buy_apartment", Strings["evt_aidar90s_apartment_purchase_opt_buy_apartment"], "🔑", MONTHLY_TICK,
                Effect(capitalDelta = -500_000L, debtDelta = 1_500_000L, debtPaymentDelta = 50_000L, investmentsDelta = 2_000_000L)),
            option("wait_apartment", Strings["evt_aidar90s_apartment_purchase_opt_wait_apartment"], "⏳", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    )

    map["political_change"] = event(
        id = "political_change",
        message = Strings["hardcoded_aidar90s_038_msg"],
        flavor = "🗳️",
        poolWeight = 8,
        tags = setOf("era"),
        options = listOf(
            option("adapt_law", Strings["evt_aidar90s_political_change_opt_adapt_law"], "📜", MONTHLY_TICK,
                Effect(knowledgeDelta = 15, incomeDelta = -5_000L)),
            option("go_shadow", Strings["evt_aidar90s_political_change_opt_go_shadow"], "🌑", MONTHLY_TICK,
                Effect(incomeDelta = 10_000L, riskDelta = 30))
        )
    )

    map["newsstand_digest"] = event(
        id = "newsstand_digest",
        message = Strings["hardcoded_story_013_msg"],
        flavor = "🗞️",
        poolWeight = 9,
        tags = setOf("world", "reflection"),
        options = listOf(
            option("buy_newspaper_world_page", Strings["evt_aidar90s_newsstand_digest_opt_buy_newspaper_world_page"], "📰", "newsstand_world_page",
                Effect(capitalDelta = -300L, knowledgeDelta = 4)),
            option("ask_trader_about_news", Strings["evt_aidar90s_newsstand_digest_opt_ask_trader_about_news"], "🧳", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, stressDelta = 1)),
            option("walk_past_newsstand", Strings["evt_aidar90s_newsstand_digest_opt_walk_past_newsstand"], "🚶", MONTHLY_TICK,
                Effect(stressDelta = -2))
        )
    )

    map["newsstand_world_page"] = event(
        id = "newsstand_world_page",
        message = Strings["hardcoded_story_014_msg"],
        flavor = "📚",
        tags = setOf("world", "reflection"),
        options = listOf(
            option("keep_following_context", Strings["evt_aidar90s_newsstand_world_page_opt_keep_following_context"], "🧠", MONTHLY_TICK,
                Effect(knowledgeDelta = 6)),
            option("close_newspaper_and_focus", Strings["evt_aidar90s_newsstand_world_page_opt_close_newspaper_and_focus"], "🛠️", MONTHLY_TICK,
                Effect(stressDelta = -3))
        )
    )

    map["child_education"] = event(
        id = "child_education",
        message = Strings["hardcoded_aidar90s_039_msg"],
        flavor = "🎓",
        poolWeight = 10,
        tags = setOf("family"),
        options = listOf(
            option("private_school", Strings["evt_aidar90s_child_education_opt_private_school"], "🏫", MONTHLY_TICK,
                Effect(expensesDelta = 50_000L, knowledgeDelta = 10)),
            option("public_school", Strings["evt_aidar90s_child_education_opt_public_school"], "🏛️", MONTHLY_TICK,
                Effect(stressDelta = 5, knowledgeDelta = -5))
        )
    )

    map["retirement_planning"] = event(
        id = "retirement_planning",
        message = Strings["hardcoded_aidar90s_040_msg"],
        flavor = "👴",
        poolWeight = 8,
        tags = setOf("investment"),
        options = listOf(
            option("save_retirement", Strings["evt_aidar90s_retirement_planning_opt_save_retirement"], "🐖", MONTHLY_TICK,
                Effect(investmentsDelta = 1_500L, capitalDelta = -1_500L, knowledgeDelta = 10)),
            option("no_retirement", Strings["evt_aidar90s_retirement_planning_opt_no_retirement"], "❌", MONTHLY_TICK,
                Effect(stressDelta = -5))
        )
    )

    map["inflation_spike"] = event(
        id = "inflation_spike",
        message = Strings["hardcoded_aidar90s_041_msg"],
        flavor = "📈",
        poolWeight = 10,
        tags = setOf("crisis"),
        options = listOf(
            option("convert_all", Strings["evt_aidar90s_inflation_spike_opt_convert_all"], "💵", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, investmentsDelta = 30_000L)),
            option("buy_goods", Strings["evt_aidar90s_inflation_spike_opt_buy_goods"], "🛒", MONTHLY_TICK,
                Effect(capitalDelta = -20_000L, stressDelta = -10))
        )
    )

    map["gang_protection"] = event(
        id = "gang_protection",
        message = Strings["hardcoded_aidar90s_042_msg"],
        flavor = "🚔",
        poolWeight = 6,
        tags = setOf("crisis"),
        options = listOf(
            option("pay_protection", Strings["evt_aidar90s_gang_protection_opt_pay_protection"], "🤝", MONTHLY_TICK,
                Effect(expensesDelta = 10_000L, stressDelta = -15, riskDelta = 10)),
            option("refuse_protection", Strings["evt_aidar90s_gang_protection_opt_refuse_protection"], "🛑", MONTHLY_TICK,
                Effect(riskDelta = 40, stressDelta = 20))
        )
    )

    map["export_opportunity"] = event(
        id = "export_opportunity",
        message = Strings["hardcoded_aidar90s_043_msg"],
        flavor = "🌍",
        poolWeight = 5,
        tags = setOf("investment", "career"),
        options = listOf(
            option("pursue_export", Strings["evt_aidar90s_export_opportunity_opt_pursue_export"], "📋", MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, incomeDelta = 50_000L, knowledgeDelta = 20)),
            option("skip_export", Strings["evt_aidar90s_export_opportunity_opt_skip_export"], "❌", MONTHLY_TICK,
                Effect(stressDelta = -5))
        )
    )

    map["bank_collapse"] = event(
        id = "bank_collapse",
        message = Strings["hardcoded_aidar90s_044_msg"],
        flavor = "🏦",
        poolWeight = 4,
        tags = setOf("crisis"),
        options = listOf(
            option("accept_loss", Strings["evt_aidar90s_bank_collapse_opt_accept_loss"], "😔", MONTHLY_TICK,
                Effect(capitalDelta = -100_000L, knowledgeDelta = 20)),
            option("protest_bank", Strings["evt_aidar90s_bank_collapse_opt_protest_bank"], "📢", MONTHLY_TICK,
                Effect(stressDelta = 15, knowledgeDelta = 5))
        )
    )

    map["wedding_expense"] = event(
        id = "wedding_expense",
        message = Strings["hardcoded_aidar90s_045_msg"],
        flavor = "💒",
        poolWeight = 3,
        tags = setOf("family"),
        options = listOf(
            option("big_wedding", Strings["evt_aidar90s_wedding_expense_opt_big_wedding"], "🎉", MONTHLY_TICK,
                Effect(capitalDelta = -200_000L, stressDelta = -20, knowledgeDelta = 5)),
            option("small_wedding", Strings["evt_aidar90s_wedding_expense_opt_small_wedding"], "🏠", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, stressDelta = 15))
        )
    )

    map["corruption_demand"] = event(
        id = "corruption_demand",
        message = Strings["hardcoded_aidar90s_046_msg"],
        flavor = "🤫",
        poolWeight = 6,
        tags = setOf("crisis"),
        options = listOf(
            option("pay_bribe", Strings["evt_aidar90s_corruption_demand_opt_pay_bribe"], "💰", MONTHLY_TICK,
                Effect(capitalDelta = -30_000L, riskDelta = 20)),
            option("report_corruption", Strings["evt_aidar90s_corruption_demand_opt_report_corruption"], "📢", MONTHLY_TICK,
                Effect(stressDelta = 25, knowledgeDelta = 10, riskDelta = 30))
        )
    )

    map["stock_market_90s"] = event(
        id = "stock_market_90s",
        message = Strings["hardcoded_aidar90s_047_msg"],
        flavor = "📈",
        poolWeight = 8,
        tags = setOf("investment"),
        options = listOf(
            option("buy_stocks", Strings["evt_aidar90s_stock_market_90s_opt_buy_stocks"], "📊", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, riskDelta = 25)),
            option("avoid_stocks", Strings["evt_aidar90s_stock_market_90s_opt_avoid_stocks"], "🎰", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    )

    map["family_immigration"] = event(
        id = "family_immigration",
        message = Strings["hardcoded_aidar90s_048_msg"],
        flavor = "✈️",
        poolWeight = 5,
        tags = setOf("family"),
        options = listOf(
            option("join_immigration", Strings["evt_aidar90s_family_immigration_opt_join_immigration"], "🧳", "ending_emigration",
                Effect(capitalDelta = -500_000L)),
            option("stay_kazakhstan", Strings["evt_aidar90s_family_immigration_opt_stay_kazakhstan"], "🇰🇿", MONTHLY_TICK,
                Effect(stressDelta = 10, knowledgeDelta = 5))
        )
    )

    map["millennium_eve"] = event(
        id = "millennium_eve",
        message = Strings["hardcoded_aidar90s_049_msg"],
        flavor = "🎆",
        poolWeight = 3,
        tags = setOf("ending"),
        options = listOf(
            option("reflect_wisdom", Strings["evt_aidar90s_millennium_eve_opt_reflect_wisdom"], "📚", "final_choice",
                Effect(knowledgeDelta = 20)),
            option("reflect_money", Strings["evt_aidar90s_millennium_eve_opt_reflect_money"], "💰", "final_choice",
                Effect(riskDelta = 20))
        )
    )
}

fun aidar90sEventPool(): List<PoolEntry> = buildList {
    add(PoolEntry("normal_life", 20))
    add(PoolEntry("job_offer", 10))
    add(PoolEntry("health_issue", 8))
    add(PoolEntry("friend_investment", 12))
    add(PoolEntry("black_market", 10))
    add(PoolEntry("family_celebration", 15))
    add(PoolEntry("utility_bills", 18))
    add(PoolEntry("education_opportunity", 10))
    add(PoolEntry("car_purchase", 8))
    add(PoolEntry("apartment_rent", 15))
    add(PoolEntry("winter_prep", 12))
    add(PoolEntry("tax_inspection", 6))
    add(PoolEntry("lottery_win", 5))
    add(PoolEntry("old_friend_return", 8))
    add(PoolEntry("child_birth", 4))
    add(PoolEntry("theft_victim", 6))
    add(PoolEntry("currency_exchange", 12))
    add(PoolEntry("business_partner_betray", 5))
    add(PoolEntry("government_subsidy", 8))
    add(PoolEntry("medical_emergency", 4))
    add(PoolEntry("new_year_bonus", 10))
    add(PoolEntry("apartment_purchase", 6))
    add(PoolEntry("political_change", 8))
    add(PoolEntry("newsstand_digest", 9))
    add(PoolEntry("child_education", 10))
    add(PoolEntry("retirement_planning", 8))
    add(PoolEntry("inflation_spike", 10))
    add(PoolEntry("gang_protection", 6))
    add(PoolEntry("export_opportunity", 5))
    add(PoolEntry("bank_collapse", 4))
    add(PoolEntry("wedding_expense", 3))
    add(PoolEntry("corruption_demand", 6))
    add(PoolEntry("stock_market_90s", 8))
    add(PoolEntry("family_immigration", 5))
    add(PoolEntry("millennium_eve", 3))
    add(PoolEntry("market_opportunity", 8))
    add(PoolEntry("business_opportunity", 8))
    add(PoolEntry("inflation_crisis", 10))
    add(PoolEntry("constitution_1995", 5))
    add(PoolEntry("russia_crisis_1998", 5))
    addAll(ScamEventLibrary.poolEntries)
}
