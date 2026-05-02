package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.MONTH
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.STRESS
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GT
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.GTE
import kz.fearsom.financiallifev2.model.Condition.Stat.Op.LTE
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.Effect
import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kz.fearsom.financiallifev2.model.MonetaryReform
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option

fun aidar90sStoryArc(): EventArc = EventArc { map ->

    // ── INTRO ─────────────────────────────────────────────────────────────────
    map["intro"] = event(
        id = "intro",
        message = Strings["hardcoded_story_001_msg"],
        flavor = "📼",
        options = listOf(
            option("warn_parents", Strings["evt_aidar90s_intro_opt_warn_parents"], "🛑", "parents_conflict",
                Effect(knowledgeDelta = 5, stressDelta = 10, setFlags = setOf("warned_parents"))),
            option("send_money_help", Strings["evt_aidar90s_intro_opt_send_money_help"], "🤲", "parents_lost_money",
                Effect(capitalDelta = -20_000_000L, stressDelta = 20, setFlags = setOf("helped_parents_scam"))),
            option("ignore_focus_self", Strings["evt_aidar90s_intro_opt_ignore_focus_self"], "🧊", MONTHLY_TICK,
                Effect(stressDelta = -5, knowledgeDelta = 2, setFlags = setOf("self_preservation")))
        )
    )

    // ── ГЛАВА 1: Выживание ────────────────────────────────────────────────────
    map["parents_conflict"] = event(
        id = "parents_conflict",
        message = Strings["hardcoded_story_002_msg"],
        flavor = "📞",
        tags = setOf("family", "career"),
        options = listOf(
            option("take_goods", Strings["evt_aidar90s_parents_conflict_opt_take_goods"], "🍬", MONTHLY_TICK,
                Effect(capitalDelta = 2_500_000L, knowledgeDelta = 3, stressDelta = 2)),
            option("wait_cash", Strings["evt_aidar90s_parents_conflict_opt_wait_cash"], "💵", MONTHLY_TICK,
                Effect(stressDelta = 5)),
            option("look_side_hustle", Strings["evt_aidar90s_parents_conflict_opt_look_side_hustle"], "🏪", "market_opportunity",
                Effect(stressDelta = 10, knowledgeDelta = 5))
        )
    )

    map["parents_lost_money"] = event(
        id = "parents_lost_money",
        message = Strings["hardcoded_story_003_msg"],
        flavor = "💔",
        tags = setOf("crisis", "family"),
        options = listOf(
            option("take_extra_job", Strings["evt_aidar90s_parents_lost_money_opt_take_extra_job"], "🚕", MONTHLY_TICK,
                Effect(incomeDelta = 5_000_000L, stressDelta = 25, knowledgeDelta = 5)),
            option("sell_computer", Strings["evt_aidar90s_parents_lost_money_opt_sell_computer"], "💻", "no_computer_life",
                Effect(capitalDelta = 75_000_000L, incomeDelta = -2_500_000L, stressDelta = -10))
        )
    )

    map["market_opportunity"] = event(
        id = "market_opportunity",
        message = Strings["hardcoded_story_004_msg"],
        flavor = "⌚",
        tags = setOf("investment", "adventure"),
        options = listOf(
            option("buy_watches", Strings["evt_aidar90s_market_opportunity_opt_buy_watches"], "🎲", MONTHLY_TICK,
                Effect(capitalDelta = -15_000_000L, riskDelta = 20, scheduleEvent = ScheduledEvent("watches_result", 2))),
            option("skip_risk", Strings["evt_aidar90s_market_opportunity_opt_skip_risk"], "📦", MONTHLY_TICK,
                Effect(capitalDelta = 2_500_000L, stressDelta = 10, knowledgeDelta = 2))
        )
    )

    map["watches_result"] = event(
        id = "watches_result",
        message = Strings["hardcoded_story_005_msg"],
        flavor = "📦",
        tags = setOf("consequence"),
        options = listOf(
            option("sell_success", Strings["evt_aidar90s_watches_result_opt_sell_success"], "🤑", MONTHLY_TICK,
                Effect(capitalDelta = 90_000L, knowledgeDelta = 10, stressDelta = -10)),
            option("sell_loss", Strings["evt_aidar90s_watches_result_opt_sell_loss"], "📉", MONTHLY_TICK,
                Effect(capitalDelta = 10_000L, stressDelta = 20))
        )
    )

    map["no_computer_life"] = event(
        id = "no_computer_life",
        message = Strings["hardcoded_aidar90s_001_msg"],
        flavor = "🕯️",
        options = listOf(
            option("hard_labor", Strings["evt_aidar90s_no_computer_life_opt_hard_labor"], "🏭", MONTHLY_TICK,
                Effect(incomeDelta = 2_500_000L, stressDelta = 15, knowledgeDelta = -5))
        )
    )

    // ── ГЛАВА 2: Введение Тенге ───────────────────────────────────────────────
    map["tenge_introduction"] = event(
        id = "tenge_introduction",
        message = Strings["hardcoded_story_006_msg"],
        flavor = "📰",
        tags = setOf("era", "crisis"),
        options = listOf(
            option("buy_usd_tenge", Strings["evt_aidar90s_tenge_introduction_opt_buy_usd_tenge"], "💵", MONTHLY_TICK,
                Effect(
                    capitalDelta = -10_000_000L, investmentsDelta = 10_000_000L, knowledgeDelta = 5,
                    monetaryReform = MonetaryReform(CurrencyCode.RUB, CurrencyCode.KZT, 1L, 500L)
                )),
            option("hold_tenge", Strings["evt_aidar90s_tenge_introduction_opt_hold_tenge"], "🇰🇿", MONTHLY_TICK,
                Effect(
                    stressDelta = 10, knowledgeDelta = 3,
                    monetaryReform = MonetaryReform(CurrencyCode.RUB, CurrencyCode.KZT, 1L, 500L)
                )),
            option("buy_gold", Strings["evt_aidar90s_tenge_introduction_opt_buy_gold"], "🪙", MONTHLY_TICK,
                Effect(
                    capitalDelta = -12_500_000L, investmentsDelta = 12_500_000L, riskDelta = 10,
                    monetaryReform = MonetaryReform(CurrencyCode.RUB, CurrencyCode.KZT, 1L, 500L)
                ))
        )
    )

    map["inflation_crisis"] = event(
        id = "inflation_crisis",
        message = Strings["hardcoded_aidar90s_002_msg"],
        flavor = "📈",
        tags = setOf("crisis"),
        options = listOf(
            option("cut_expenses", Strings["evt_aidar90s_inflation_crisis_opt_cut_expenses"], "✂️", MONTHLY_TICK,
                Effect(expensesDelta = -3_000L, stressDelta = 15)),
            option("borrow_money", Strings["evt_aidar90s_inflation_crisis_opt_borrow_money"], "🤝", MONTHLY_TICK,
                Effect(debtDelta = 50_000L, capitalDelta = 50_000L, stressDelta = -5)),
            option("find_second_job", Strings["evt_aidar90s_inflation_crisis_opt_find_second_job"], "💼", MONTHLY_TICK,
                Effect(incomeDelta = 8_000L, stressDelta = 20))
        )
    )

    // ── ГЛАВА 3: Предпринимательство ─────────────────────────────────────────
    map["business_opportunity"] = event(
        id = "business_opportunity",
        message = Strings["hardcoded_aidar90s_003_msg"],
        flavor = "🏪",
        tags = setOf("investment", "career"),
        options = listOf(
            option("open_kiosk", Strings["evt_aidar90s_business_opportunity_opt_open_kiosk"], "🤝", "kiosk_opened",
                Effect(capitalDelta = -100_000L, incomeDelta = 25_000L, knowledgeDelta = 15, setFlags = setOf("has_kiosk"))),
            option("decline_business", Strings["evt_aidar90s_business_opportunity_opt_decline_business"], "❌", MONTHLY_TICK,
                Effect(knowledgeDelta = 5, stressDelta = -5)),
            option("negotiate_better", Strings["evt_aidar90s_business_opportunity_opt_negotiate_better"], "💬", "kiosk_negotiation",
                Effect(knowledgeDelta = 8, riskDelta = 5))
        )
    )

    map["kiosk_opened"] = event(
        id = "kiosk_opened",
        message = Strings["hardcoded_aidar90s_004_msg"],
        flavor = "🎉",
        tags = setOf("consequence"),
        options = listOf(
            option("pay_taxes", Strings["evt_aidar90s_kiosk_opened_opt_pay_taxes"], "📋", MONTHLY_TICK,
                Effect(incomeDelta = -5_000L, stressDelta = -10, knowledgeDelta = 5)),
            option("bribe_inspector", Strings["evt_aidar90s_kiosk_opened_opt_bribe_inspector"], "🤫", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, stressDelta = 15, riskDelta = 20))
        )
    )

    map["kiosk_negotiation"] = event(
        id = "kiosk_negotiation",
        message = Strings["hardcoded_aidar90s_005_msg"],
        flavor = "💬",
        tags = setOf("consequence"),
        options = listOf(
            option("accept_deal", Strings["evt_aidar90s_kiosk_negotiation_opt_accept_deal"], "✅", "kiosk_opened",
                Effect(capitalDelta = -120_000L, incomeDelta = 30_000L, setFlags = setOf("has_kiosk"))),
            option("walk_away", Strings["evt_aidar90s_kiosk_negotiation_opt_walk_away"], "🚶", MONTHLY_TICK,
                Effect(stressDelta = -10, knowledgeDelta = 5))
        )
    )

    // ── ГЛАВА 4: Повторный скам ───────────────────────────────────────────────
    map["parents_scam_again"] = event(
        id = "parents_scam_again",
        message = Strings["hardcoded_aidar90s_006_msg"],
        flavor = "📞",
        tags = setOf("family", "scam"),
        options = listOf(
            option("stop_parents_hard", Strings["evt_aidar90s_parents_scam_again_opt_stop_parents_hard"], "🛑", MONTHLY_TICK,
                Effect(stressDelta = 20, knowledgeDelta = 10, setFlags = setOf("parents_scam_stopped"))),
            option("let_them_try", Strings["evt_aidar90s_parents_scam_again_opt_let_them_try"], "🎲", "parents_lost_money_2",
                Effect(stressDelta = 10, setFlags = setOf("parents_scam_stopped"))),
            option("educate_parents", Strings["evt_aidar90s_parents_scam_again_opt_educate_parents"], "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 15, stressDelta = 5, setFlags = setOf("parents_educated")))
        )
    )

    map["parents_lost_money_2"] = event(
        id = "parents_lost_money_2",
        message = Strings["hardcoded_aidar90s_007_msg"],
        flavor = "💔",
        tags = setOf("crisis", "family"),
        options = listOf(
            option("pay_parents_debt", Strings["evt_aidar90s_parents_lost_money_2_opt_pay_parents_debt"], "💳", MONTHLY_TICK,
                Effect(debtDelta = 500_000L, stressDelta = 30, setFlags = setOf("saved_parents_home"))),
            option("let_them_suffer", Strings["evt_aidar90s_parents_lost_money_2_opt_let_them_suffer"], "🧊", MONTHLY_TICK,
                Effect(stressDelta = -20, knowledgeDelta = 10, setFlags = setOf("parents_lost_home")))
        )
    )

    // ── ГЛАВА 5: Конституция 1995 ─────────────────────────────────────────────
    map["constitution_1995"] = event(
        id = "constitution_1995",
        message = Strings["hardcoded_aidar90s_008_msg"],
        flavor = "📜",
        tags = setOf("era", "investment"),
        options = listOf(
            option("buy_land", Strings["evt_aidar90s_constitution_1995_opt_buy_land"], "🌾", MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L, knowledgeDelta = 20)),
            option("skip_land", Strings["evt_aidar90s_constitution_1995_opt_skip_land"], "❌", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    )

    map["chechen_war_broadcast"] = event(
        id = "chechen_war_broadcast",
        message = Strings["hardcoded_story_007_msg"],
        flavor = "📺",
        tags = setOf("era", "world", "family"),
        unique = true,
        options = listOf(
            option("call_relatives_russia", Strings["evt_aidar90s_chechen_war_broadcast_opt_call_relatives_russia"], "☎️", MONTHLY_TICK,
                Effect(capitalDelta = -2_000L, stressDelta = -5, knowledgeDelta = 3)),
            option("listen_veteran_story", Strings["evt_aidar90s_chechen_war_broadcast_opt_listen_veteran_story"], "🪖", "chechen_war_veteran_story",
                Effect(knowledgeDelta = 2, stressDelta = 3)),
            option("switch_off_tv", Strings["evt_aidar90s_chechen_war_broadcast_opt_switch_off_tv"], "📴", MONTHLY_TICK,
                Effect(stressDelta = -2))
        )
    )

    map["chechen_war_veteran_story"] = event(
        id = "chechen_war_veteran_story",
        message = Strings["hardcoded_story_008_msg"],
        flavor = "🕯️",
        tags = setOf("world", "reflection"),
        options = listOf(
            option("remember_peace_value", Strings["evt_aidar90s_chechen_war_veteran_story_opt_remember_peace_value"], "🕊️", MONTHLY_TICK,
                Effect(knowledgeDelta = 6, stressDelta = -4)),
            option("go_home_after_talk", Strings["evt_aidar90s_chechen_war_veteran_story_opt_go_home_after_talk"], "🏠", MONTHLY_TICK,
                Effect(stressDelta = -6))
        )
    )

    map["nuclear_disarmament_reaction"] = event(
        id = "nuclear_disarmament_reaction",
        message = Strings["hardcoded_story_009_msg"],
        flavor = "☢️",
        tags = setOf("era", "world", "reflection"),
        unique = true,
        options = listOf(
            option("support_disarmament", Strings["evt_aidar90s_nuclear_disarmament_reaction_opt_support_disarmament"], "🕊️", MONTHLY_TICK,
                Effect(knowledgeDelta = 8, stressDelta = -4)),
            option("ask_about_semey", Strings["evt_aidar90s_nuclear_disarmament_reaction_opt_ask_about_semey"], "🗣️", "semey_memory_story",
                Effect(knowledgeDelta = 3)),
            option("miss_missile_power", Strings["evt_aidar90s_nuclear_disarmament_reaction_opt_miss_missile_power"], "🧨", MONTHLY_TICK,
                Effect(riskDelta = 5, stressDelta = 2))
        )
    )

    map["semey_memory_story"] = event(
        id = "semey_memory_story",
        message = Strings["hardcoded_story_010_msg"],
        flavor = "🌫️",
        tags = setOf("world", "reflection"),
        options = listOf(
            option("choose_life_over_fear", Strings["evt_aidar90s_semey_memory_story_opt_choose_life_over_fear"], "❤️", MONTHLY_TICK,
                Effect(knowledgeDelta = 7, stressDelta = -5)),
            option("stay_silent_after_story", Strings["evt_aidar90s_semey_memory_story_opt_stay_silent_after_story"], "🤐", MONTHLY_TICK,
                Effect(stressDelta = -2, knowledgeDelta = 2))
        )
    )

    map["capital_move_debate"] = event(
        id = "capital_move_debate",
        message = Strings["hardcoded_story_011_msg"],
        flavor = "🏙️",
        tags = setOf("era", "world", "career"),
        unique = true,
        options = listOf(
            option("dismiss_capital_move", Strings["evt_aidar90s_capital_move_debate_opt_dismiss_capital_move"], "🙄", MONTHLY_TICK,
                Effect(stressDelta = -1)),
            option("study_new_capital_wave", Strings["evt_aidar90s_capital_move_debate_opt_study_new_capital_wave"], "🧭", "capital_move_opportunity",
                Effect(knowledgeDelta = 4)),
            option("talk_family_about_change", Strings["evt_aidar90s_capital_move_debate_opt_talk_family_about_change"], "👨‍👩‍👦", MONTHLY_TICK,
                Effect(stressDelta = -3, knowledgeDelta = 2))
        )
    )

    map["capital_move_opportunity"] = event(
        id = "capital_move_opportunity",
        message = Strings["hardcoded_story_012_msg"],
        flavor = "🗺️",
        tags = setOf("career", "reflection"),
        options = listOf(
            option("save_for_akmola_trip", Strings["evt_aidar90s_capital_move_opportunity_opt_save_for_akmola_trip"], "🧳", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, knowledgeDelta = 8)),
            option("just_note_trend", Strings["evt_aidar90s_capital_move_opportunity_opt_just_note_trend"], "📝", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    )

    // ── ГЛАВА 6: Кризис 1998 ──────────────────────────────────────────────────
    map["russia_crisis_1998"] = event(
        id = "russia_crisis_1998",
        message = Strings["hardcoded_aidar90s_009_msg"],
        flavor = "🚨",
        tags = setOf("era", "crisis"),
        options = listOf(
            option("hedge_currency", Strings["evt_aidar90s_russia_crisis_1998_opt_hedge_currency"], "💵", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, stressDelta = -10)),
            option("cut_business", Strings["evt_aidar90s_russia_crisis_1998_opt_cut_business"], "✂️", MONTHLY_TICK,
                Effect(incomeDelta = -20_000L, capitalDelta = 30_000L, stressDelta = 10)),
            option("hold_and_pray", Strings["evt_aidar90s_russia_crisis_1998_opt_hold_and_pray"], "🙏", MONTHLY_TICK,
                Effect(stressDelta = 25))
        )
    )

    // ── ГЛАВА 7: Финал ────────────────────────────────────────────────────────
    map["final_choice"] = event(
        id = "final_choice",
        message = Strings["hardcoded_aidar90s_010_msg"],
        flavor = "🎯",
        tags = setOf("ending"),
        options = listOf(
            option("emigrate", Strings["evt_aidar90s_final_choice_opt_emigrate"], "✈️", "ending_emigration", Effect()),
            option("stay_build", Strings["evt_aidar90s_final_choice_opt_stay_build"], "🏗️", "ending_business", Effect()),
            option("retire_early", Strings["evt_aidar90s_final_choice_opt_retire_early"], "🏖️", "ending_freedom", Effect())
        )
    )

    // ── КОНЦОВКИ ──────────────────────────────────────────────────────────────
    map["ending_freedom"] = event(
        id = "ending_freedom",
        message = Strings["hardcoded_aidar90s_011_msg"],
        flavor = "🏆",
        isEnding = true,
        endingType = EndingType.FINANCIAL_FREEDOM,
        options = emptyList()
    )

    map["ending_business"] = event(
        id = "ending_business",
        message = Strings["hardcoded_aidar90s_012_msg"],
        flavor = "🏢",
        isEnding = true,
        endingType = EndingType.WEALTH,
        options = emptyList()
    )

    map["ending_emigration"] = event(
        id = "ending_emigration",
        message = Strings["hardcoded_aidar90s_013_msg"],
        flavor = "✈️",
        isEnding = true,
        endingType = EndingType.FINANCIAL_STABILITY,
        options = emptyList()
    )

    map["ending_bankruptcy"] = event(
        id = "ending_bankruptcy",
        message = Strings["hardcoded_aidar90s_014_msg"],
        flavor = "💀",
        isEnding = true,
        endingType = EndingType.BANKRUPTCY,
        options = emptyList()
    )

    map["ending_paycheck"] = event(
        id = "ending_paycheck",
        message = Strings["hardcoded_aidar90s_015_msg"],
        flavor = "😐",
        isEnding = true,
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        options = emptyList()
    )

    // ── HUB: Обычная жизнь ────────────────────────────────────────────────────
    map["normal_life"] = event(
        id = "normal_life",
        message = Strings["hardcoded_aidar90s_016_msg"],
        flavor = "📺",
        poolWeight = 20,
        options = listOf(
            option("buy_usd", Strings["evt_aidar90s_normal_life_opt_buy_usd"], "💵", MONTHLY_TICK,
                Effect(capitalDelta = -10_000L, investmentsDelta = 10_000L, knowledgeDelta = 2)),
            option("study_books", Strings["evt_aidar90s_normal_life_opt_study_books"], "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 5, stressDelta = -2)),
            option("do_nothing", Strings["evt_aidar90s_normal_life_opt_do_nothing"], "😐", MONTHLY_TICK,
                Effect(stressDelta = 2)),
            option("network_friends", Strings["evt_aidar90s_normal_life_opt_network_friends"], "🍺", MONTHLY_TICK,
                Effect(knowledgeDelta = 3, capitalDelta = -3_000L, stressDelta = -5))
        )
    )
}

fun aidar90sConditionals(): List<GameEvent> = listOf(
    event(
        id = "bankruptcy_trigger",
        message = Strings["evt_aidar90s_bankruptcy_trigger_msg"],
        flavor = "💀",
        priority = 90,
        conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
        options = listOf(
            option("accept_bankruptcy", Strings["evt_aidar90s_bankruptcy_trigger_opt_accept_bankruptcy"], "💔", "ending_bankruptcy", Effect())
        )
    ),
    event(
        id = "debt_crisis",
        message = Strings["hardcoded_aidar90s_072_msg"],
        flavor = "🚨",
        priority = 100,
        conditions = listOf(cond(DEBT, GT, 100_000L)),
        tags = setOf("crisis"),
        options = listOf(
            option("debt_collect", Strings["evt_aidar90s_debt_crisis_opt_debt_collect"], "🔫", MONTHLY_TICK,
                Effect(debtDelta = -50_000L, stressDelta = 30, capitalDelta = -20_000L)),
            option("debt_wait", Strings["evt_aidar90s_debt_crisis_opt_debt_wait"], "⏳", MONTHLY_TICK,
                Effect(stressDelta = 10))
        )
    ),
    event(
        id = "parents_scam_again_conditional",
        message = Strings["hardcoded_aidar90s_050_msg"],
        flavor = "📞",
        priority = 80,
        conditions = listOf(
            cond(MONTH, GTE, 12L),
            Condition.NotFlag("parents_scam_stopped"),
            Condition.ForCharacter("aidar_90s")
        ),
        unique = true,
        tags = setOf("family", "scam"),
        options = listOf(
            option("stop_parents_hard", Strings["evt_aidar90s_parents_scam_again_conditional_opt_stop_parents_hard"], "🛑", MONTHLY_TICK,
                Effect(stressDelta = 20, knowledgeDelta = 10, setFlags = setOf("parents_scam_stopped"))),
            option("let_them_try", Strings["evt_aidar90s_parents_scam_again_conditional_opt_let_them_try"], "🎲", "parents_lost_money_2",
                Effect(stressDelta = 10, setFlags = setOf("parents_scam_stopped")))
        )
    ),
    event(
        id = "burnout_90s",
        message = Strings["hardcoded_aidar90s_073_msg"],
        flavor = "🔥",
        priority = 60,
        conditions = listOf(cond(STRESS, GT, 75L)),
        cooldownMonths = 6,
        options = listOf(
            option("rest_home", Strings["evt_aidar90s_burnout_90s_opt_rest_home"], "🏠", MONTHLY_TICK,
                Effect(expensesDelta = -5_000L, stressDelta = -15, incomeDelta = -5_000L)),
            option("drink_friends", Strings["evt_aidar90s_burnout_90s_opt_drink_friends"], "🍺", MONTHLY_TICK,
                Effect(capitalDelta = -5_000L, stressDelta = -10, knowledgeDelta = -2))
        )
    ),
    event(
        id = "era_tenge_introduced",
        message = Strings["hardcoded_aidar90s_051_msg"],
        flavor = "📰",
        priority = 95,
        conditions = listOf(
            cond(MONTH, GTE, 4L),
            cond(MONTH, LTE, 6L),
            Condition.InEra("kz_90s")
        ),
        unique = true,
        tags = setOf("era"),
        options = listOf(
            option("buy_usd_tenge", Strings["evt_aidar90s_era_tenge_introduced_opt_buy_usd_tenge"], "💵", MONTHLY_TICK,
                Effect(
                    capitalDelta = -10_000_000L, investmentsDelta = 10_000_000L, knowledgeDelta = 5,
                    monetaryReform = MonetaryReform(CurrencyCode.RUB, CurrencyCode.KZT, 1L, 500L)
                )),
            option("hold_tenge", Strings["evt_aidar90s_era_tenge_introduced_opt_hold_tenge"], "🇰🇿", MONTHLY_TICK,
                Effect(
                    stressDelta = 10, knowledgeDelta = 3,
                    monetaryReform = MonetaryReform(CurrencyCode.RUB, CurrencyCode.KZT, 1L, 500L)
                ))
        )
    ),
    event(
        id = "era_constitution_1995",
        message = Strings["hardcoded_aidar90s_052_msg"],
        flavor = "📜",
        priority = 95,
        conditions = listOf(
            cond(MONTH, GTE, 20L),
            cond(MONTH, LTE, 24L),
            Condition.InEra("kz_90s")
        ),
        unique = true,
        tags = setOf("era"),
        options = listOf(
            option("buy_land", Strings["evt_aidar90s_era_constitution_1995_opt_buy_land"], "🌾", MONTHLY_TICK,
                Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L, knowledgeDelta = 20)),
            option("skip_land", Strings["evt_aidar90s_era_constitution_1995_opt_skip_land"], "❌", MONTHLY_TICK,
                Effect(knowledgeDelta = 5))
        )
    ),
    event(
        id = "era_russia_crisis_1998",
        message = Strings["hardcoded_aidar90s_053_msg"],
        flavor = "🚨",
        priority = 95,
        conditions = listOf(
            cond(MONTH, GTE, 30L),
            cond(MONTH, LTE, 36L),
            Condition.InEra("kz_90s")
        ),
        unique = true,
        tags = setOf("era", "crisis"),
        options = listOf(
            option("hedge_currency", Strings["evt_aidar90s_era_russia_crisis_1998_opt_hedge_currency"], "💵", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, stressDelta = -10)),
            option("cut_business", Strings["evt_aidar90s_era_russia_crisis_1998_opt_cut_business"], "✂️", MONTHLY_TICK,
                Effect(incomeDelta = -20_000L, capitalDelta = 30_000L, stressDelta = 10))
        )
    ),
    event(
        id = "trap_warning",
        message = Strings["hardcoded_aidar90s_074_msg"],
        flavor = "⚠️",
        priority = 50,
        conditions = listOf(
            cond(CAPITAL, LTE, 50_000L),
            cond(KNOWLEDGE, LTE, 15L),
            cond(MONTH, GTE, 6L)
        ),
        tags = setOf("warning"),
        options = listOf(
            option("learn_more", Strings["evt_aidar90s_trap_warning_opt_learn_more"], "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 10, stressDelta = 5)),
            option("ignore_warning", Strings["evt_aidar90s_trap_warning_opt_ignore_warning"], "😶", MONTHLY_TICK,
                Effect(stressDelta = 5))
        )
    ),
    event(
        id = "bonus_received",
        message = Strings["hardcoded_aidar90s_075_msg"],
        flavor = "🎉",
        priority = 30,
        conditions = listOf(
            cond(CAPITAL, GTE, 500_000L),
            cond(KNOWLEDGE, GTE, 30L)
        ),
        cooldownMonths = 12,
        options = listOf(
            option("reinvest_bonus", Strings["evt_aidar90s_bonus_received_opt_reinvest_bonus"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 100_000L, capitalDelta = -100_000L, knowledgeDelta = 5)),
            option("celebrate_bonus", Strings["evt_aidar90s_bonus_received_opt_celebrate_bonus"], "🎊", MONTHLY_TICK,
                Effect(capitalDelta = -50_000L, stressDelta = -20))
        )
    )
)
