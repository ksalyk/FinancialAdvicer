// ── Пакеты ─────────────────────────────────────────────────────────────────
package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.Condition
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.MONTH
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
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.ScheduledEvent
import kz.fearsom.financiallifev2.i18n.Strings

// ── Сценарий: Айдар (90-е) ────────────────────────────────────────────────
class Aidar90sScenarioGraph : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital = 25_000_000L,
        income = 7_500_000L,
        expenses = 6_000_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.05,
        stress = 60,
        financialKnowledge = 15,
        riskLevel = 40,
        month = 10,
        year = 1993,
        characterId = "aidar_90s",
        eraId = "kz_90s",
        currency = CurrencyCode.RUB,
        flags = setOf()
    )

    override val events: Map<String, GameEvent> = buildMap {

        // ── ОБЯЗАТЕЛЬНО: Стартовое событие ───────────────────────────────
        put("intro", event(
            id = "intro",
            message = Strings["hardcoded_story_001_msg"],
            flavor = "📼",
            options = listOf(
                option(
                    id = "warn_parents",
                    text = Strings["evt_aidar90s_intro_opt_warn_parents"],
                    emoji = "🛑",
                    next = "parents_conflict",
                    fx = Effect(knowledgeDelta = 5, stressDelta = 10, setFlags = setOf("warned_parents"))
                ),
                option(
                    id = "send_money_help",
                    text = Strings["evt_aidar90s_intro_opt_send_money_help"],
                    emoji = "🤲",
                    next = "parents_lost_money",
                    fx = Effect(capitalDelta = -20_000_000L, stressDelta = 20, setFlags = setOf("helped_parents_scam"))
                ),
                option(
                    id = "ignore_focus_self",
                    text = Strings["evt_aidar90s_intro_opt_ignore_focus_self"],
                    emoji = "🧊",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5, knowledgeDelta = 2, setFlags = setOf("self_preservation"))
                )
            )
        ))

        // ── ГЛАВА 1: Выживание (Месяцы 1-3) ──────────────────────────────
        put("parents_conflict", event(
            id = "parents_conflict",
            message = Strings["hardcoded_story_002_msg"],
            flavor = "📞",
            tags = setOf("family", "career"),
            options = listOf(
                option(
                    id = "take_goods",
                    text = Strings["evt_aidar90s_parents_conflict_opt_take_goods"],
                    emoji = "🍬",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 2_500_000L, knowledgeDelta = 3, stressDelta = 2)
                ),
                option(
                    id = "wait_cash",
                    text = Strings["evt_aidar90s_parents_conflict_opt_wait_cash"],
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 5)
                ),
                option(
                    id = "look_side_hustle",
                    text = Strings["evt_aidar90s_parents_conflict_opt_look_side_hustle"],
                    emoji = "🏪",
                    next = "market_opportunity",
                    fx = Effect(stressDelta = 10, knowledgeDelta = 5)
                )
            )
        ))

        put("parents_lost_money", event(
            id = "parents_lost_money",
            message = Strings["hardcoded_story_003_msg"],
            flavor = "💔",
            tags = setOf("crisis", "family"),
            options = listOf(
                option(
                    id = "take_extra_job",
                    text = Strings["evt_aidar90s_parents_lost_money_opt_take_extra_job"],
                    emoji = "🚕",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 5_000_000L, stressDelta = 25, knowledgeDelta = 5)
                ),
                option(
                    id = "sell_computer",
                    text = Strings["evt_aidar90s_parents_lost_money_opt_sell_computer"],
                    emoji = "💻",
                    next = "no_computer_life",
                    fx = Effect(capitalDelta = 75_000_000L, incomeDelta = -2_500_000L, stressDelta = -10)
                )
            )
        ))

        put("market_opportunity", event(
            id = "market_opportunity",
            message = Strings["hardcoded_story_004_msg"],
            flavor = "⌚",
            tags = setOf("investment", "adventure"),
            options = listOf(
                option(
                    id = "buy_watches",
                    text = Strings["evt_aidar90s_market_opportunity_opt_buy_watches"],
                    emoji = "🎲",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000_000L, riskDelta = 20, scheduleEvent = ScheduledEvent("watches_result", 2))
                ),
                option(
                    id = "skip_risk",
                    text = Strings["evt_aidar90s_market_opportunity_opt_skip_risk"],
                    emoji = "📦",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 2_500_000L, stressDelta = 10, knowledgeDelta = 2)
                )
            )
        ))

        put("watches_result", event(
            id = "watches_result",
            message = Strings["hardcoded_story_005_msg"],
            flavor = "📦",
            tags = setOf("consequence"),
            options = listOf(
                option(
                    id = "sell_success",
                    text = Strings["evt_aidar90s_watches_result_opt_sell_success"],
                    emoji = "🤑",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 90_000L, knowledgeDelta = 10, stressDelta = -10),
//                    conditions = listOf(cond(KNOWLEDGE, GTE, 25L))
                ),
                option(
                    id = "sell_loss",
                    text = Strings["evt_aidar90s_watches_result_opt_sell_loss"],
                    emoji = "📉",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 10_000L, stressDelta = 20),
//                    conditions = listOf(cond(KNOWLEDGE, LT, 25L))
                )
            )
        ))

        put("no_computer_life", event(
            id = "no_computer_life",
            message = Strings["hardcoded_aidar90s_001_msg"],
            flavor = "🕯️",
            options = listOf(
                option(
                    id = "hard_labor",
                    text = Strings["evt_aidar90s_no_computer_life_opt_hard_labor"],
                    emoji = "🏭",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 2_500_000L, stressDelta = 15, knowledgeDelta = -5)
                )
            )
        ))

        // ── ГЛАВА 2: Введение Тенге (Месяц 4-6) ───────────────────────────
        put("tenge_introduction", event(
            id = "tenge_introduction",
            message = Strings["hardcoded_story_006_msg"],
            flavor = "📰",
            tags = setOf("era", "crisis"),
            options = listOf(
                option(
                    id = "buy_usd_tenge",
                    text = Strings["evt_aidar90s_tenge_introduction_opt_buy_usd_tenge"],
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        capitalDelta = -10_000_000L,
                        investmentsDelta = 10_000_000L,
                        knowledgeDelta = 5,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1L,
                            denominator = 500L
                        )
                    )
                ),
                option(
                    id = "hold_tenge",
                    text = Strings["evt_aidar90s_tenge_introduction_opt_hold_tenge"],
                    emoji = "🇰🇿",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        stressDelta = 10,
                        knowledgeDelta = 3,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1L,
                            denominator = 500L
                        )
                    )
                ),
                option(
                    id = "buy_gold",
                    text = Strings["evt_aidar90s_tenge_introduction_opt_buy_gold"],
                    emoji = "🪙",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        capitalDelta = -12_500_000L,
                        investmentsDelta = 12_500_000L,
                        riskDelta = 10,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1L,
                            denominator = 500L
                        )
                    )
                )
            )
        ))

        put("inflation_crisis", event(
            id = "inflation_crisis",
            message = Strings["hardcoded_aidar90s_002_msg"],
            flavor = "📈",
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "cut_expenses",
                    text = Strings["evt_aidar90s_inflation_crisis_opt_cut_expenses"],
                    emoji = "✂️",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = -3_000L, stressDelta = 15)
                ),
                option(
                    id = "borrow_money",
                    text = Strings["evt_aidar90s_inflation_crisis_opt_borrow_money"],
                    emoji = "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(debtDelta = 50_000L, capitalDelta = 50_000L, stressDelta = -5)
                ),
                option(
                    id = "find_second_job",
                    text = Strings["evt_aidar90s_inflation_crisis_opt_find_second_job"],
                    emoji = "💼",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 8_000L, stressDelta = 20)
                )
            )
        ))

        // ── ГЛАВА 3: Предпринимательство (Месяц 7-12) ────────────────────
        put("business_opportunity", event(
            id = "business_opportunity",
            message = Strings["hardcoded_aidar90s_003_msg"],
            flavor = "🏪",
            tags = setOf("investment", "career"),
            options = listOf(
                option(
                    id = "open_kiosk",
                    text = Strings["evt_aidar90s_business_opportunity_opt_open_kiosk"],
                    emoji = "🤝",
                    next = "kiosk_opened",
                    fx = Effect(capitalDelta = -100_000L, incomeDelta = 25_000L, knowledgeDelta = 15, setFlags = setOf("has_kiosk"))
                ),
                option(
                    id = "decline_business",
                    text = Strings["evt_aidar90s_business_opportunity_opt_decline_business"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -5)
                ),
                option(
                    id = "negotiate_better",
                    text = Strings["evt_aidar90s_business_opportunity_opt_negotiate_better"],
                    emoji = "💬",
                    next = "kiosk_negotiation",
                    fx = Effect(knowledgeDelta = 8, riskDelta = 5)
                )
            )
        ))

        put("kiosk_opened", event(
            id = "kiosk_opened",
            message = Strings["hardcoded_aidar90s_004_msg"],
            flavor = "🎉",
            tags = setOf("consequence"),
            options = listOf(
                option(
                    id = "pay_taxes",
                    text = Strings["evt_aidar90s_kiosk_opened_opt_pay_taxes"],
                    emoji = "📋",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = -5_000L, stressDelta = -10, knowledgeDelta = 5)
                ),
                option(
                    id = "bribe_inspector",
                    text = Strings["evt_aidar90s_kiosk_opened_opt_bribe_inspector"],
                    emoji = "🤫",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -5_000L, stressDelta = 15, riskDelta = 20)
                )
            )
        ))

        put("kiosk_negotiation", event(
            id = "kiosk_negotiation",
            message = Strings["hardcoded_aidar90s_005_msg"],
            flavor = "💬",
            tags = setOf("consequence"),
            options = listOf(
                option(
                    id = "accept_deal",
                    text = Strings["evt_aidar90s_kiosk_negotiation_opt_accept_deal"],
                    emoji = "✅",
                    next = "kiosk_opened",
                    fx = Effect(capitalDelta = -120_000L, incomeDelta = 30_000L, setFlags = setOf("has_kiosk"))
                ),
                option(
                    id = "walk_away",
                    text = Strings["evt_aidar90s_kiosk_negotiation_opt_walk_away"],
                    emoji = "🚶",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -10, knowledgeDelta = 5)
                )
            )
        ))

        // ── ГЛАВА 4: Повторный Скам (Месяц 13-18) ────────────────────────
        put("parents_scam_again", event(
            id = "parents_scam_again",
            message = Strings["hardcoded_aidar90s_006_msg"],
            flavor = "📞",
            tags = setOf("family", "scam"),
            options = listOf(
                option(
                    id = "stop_parents_hard",
                    text = Strings["evt_aidar90s_parents_scam_again_opt_stop_parents_hard"],
                    emoji = "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 20, knowledgeDelta = 10, setFlags = setOf("parents_scam_stopped"))
                ),
                option(
                    id = "let_them_try",
                    text = Strings["evt_aidar90s_parents_scam_again_opt_let_them_try"],
                    emoji = "🎲",
                    next = "parents_lost_money_2",
                    fx = Effect(stressDelta = 10, setFlags = setOf("parents_scam_stopped"))
                ),
                option(
                    id = "educate_parents",
                    text = Strings["evt_aidar90s_parents_scam_again_opt_educate_parents"],
                    emoji = "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 15, stressDelta = 5, setFlags = setOf("parents_educated"))
                )
            )
        ))

        put("parents_lost_money_2", event(
            id = "parents_lost_money_2",
            message = Strings["hardcoded_aidar90s_007_msg"],
            flavor = "💔",
            tags = setOf("crisis", "family"),
            options = listOf(
                option(
                    id = "pay_parents_debt",
                    text = Strings["evt_aidar90s_parents_lost_money_2_opt_pay_parents_debt"],
                    emoji = "💳",
                    next = MONTHLY_TICK,
                    fx = Effect(debtDelta = 500_000L, stressDelta = 30, setFlags = setOf("saved_parents_home"))
                ),
                option(
                    id = "let_them_suffer",
                    text = Strings["evt_aidar90s_parents_lost_money_2_opt_let_them_suffer"],
                    emoji = "🧊",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -20, knowledgeDelta = 10, setFlags = setOf("parents_lost_home"))
                )
            )
        ))

        // ── ГЛАВА 5: Конституция 1995 (Месяц 19-24) ──────────────────────
        put("constitution_1995", event(
            id = "constitution_1995",
            message = Strings["hardcoded_aidar90s_008_msg"],
            flavor = "📜",
            tags = setOf("era", "investment"),
            options = listOf(
                option(
                    id = "buy_land",
                    text = Strings["evt_aidar90s_constitution_1995_opt_buy_land"],
                    emoji = "🌾",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L, knowledgeDelta = 20)
                ),
                option(
                    id = "skip_land",
                    text = Strings["evt_aidar90s_constitution_1995_opt_skip_land"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        put("chechen_war_broadcast", event(
            id = "chechen_war_broadcast",
            message = Strings["hardcoded_story_007_msg"],
            flavor = "📺",
            tags = setOf("era", "world", "family"),
            unique = true,
            options = listOf(
                option(
                    id = "call_relatives_russia",
                    text = Strings["evt_aidar90s_chechen_war_broadcast_opt_call_relatives_russia"],
                    emoji = "☎️",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -2_000L, stressDelta = -5, knowledgeDelta = 3)
                ),
                option(
                    id = "listen_veteran_story",
                    text = Strings["evt_aidar90s_chechen_war_broadcast_opt_listen_veteran_story"],
                    emoji = "🪖",
                    next = "chechen_war_veteran_story",
                    fx = Effect(knowledgeDelta = 2, stressDelta = 3)
                ),
                option(
                    id = "switch_off_tv",
                    text = Strings["evt_aidar90s_chechen_war_broadcast_opt_switch_off_tv"],
                    emoji = "📴",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -2)
                )
            )
        ))

        put("chechen_war_veteran_story", event(
            id = "chechen_war_veteran_story",
            message = Strings["hardcoded_story_008_msg"],
            flavor = "🕯️",
            tags = setOf("world", "reflection"),
            options = listOf(
                option(
                    id = "remember_peace_value",
                    text = Strings["evt_aidar90s_chechen_war_veteran_story_opt_remember_peace_value"],
                    emoji = "🕊️",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 6, stressDelta = -4)
                ),
                option(
                    id = "go_home_after_talk",
                    text = Strings["evt_aidar90s_chechen_war_veteran_story_opt_go_home_after_talk"],
                    emoji = "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -6)
                )
            )
        ))

        put("nuclear_disarmament_reaction", event(
            id = "nuclear_disarmament_reaction",
            message = Strings["hardcoded_story_009_msg"],
            flavor = "☢️",
            tags = setOf("era", "world", "reflection"),
            unique = true,
            options = listOf(
                option(
                    id = "support_disarmament",
                    text = Strings["evt_aidar90s_nuclear_disarmament_reaction_opt_support_disarmament"],
                    emoji = "🕊️",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 8, stressDelta = -4)
                ),
                option(
                    id = "ask_about_semey",
                    text = Strings["evt_aidar90s_nuclear_disarmament_reaction_opt_ask_about_semey"],
                    emoji = "🗣️",
                    next = "semey_memory_story",
                    fx = Effect(knowledgeDelta = 3)
                ),
                option(
                    id = "miss_missile_power",
                    text = Strings["evt_aidar90s_nuclear_disarmament_reaction_opt_miss_missile_power"],
                    emoji = "🧨",
                    next = MONTHLY_TICK,
                    fx = Effect(riskDelta = 5, stressDelta = 2)
                )
            )
        ))

        put("semey_memory_story", event(
            id = "semey_memory_story",
            message = Strings["hardcoded_story_010_msg"],
            flavor = "🌫️",
            tags = setOf("world", "reflection"),
            options = listOf(
                option(
                    id = "choose_life_over_fear",
                    text = Strings["evt_aidar90s_semey_memory_story_opt_choose_life_over_fear"],
                    emoji = "❤️",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 7, stressDelta = -5)
                ),
                option(
                    id = "stay_silent_after_story",
                    text = Strings["evt_aidar90s_semey_memory_story_opt_stay_silent_after_story"],
                    emoji = "🤐",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -2, knowledgeDelta = 2)
                )
            )
        ))

        put("capital_move_debate", event(
            id = "capital_move_debate",
            message = Strings["hardcoded_story_011_msg"],
            flavor = "🏙️",
            tags = setOf("era", "world", "career"),
            unique = true,
            options = listOf(
                option(
                    id = "dismiss_capital_move",
                    text = Strings["evt_aidar90s_capital_move_debate_opt_dismiss_capital_move"],
                    emoji = "🙄",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -1)
                ),
                option(
                    id = "study_new_capital_wave",
                    text = Strings["evt_aidar90s_capital_move_debate_opt_study_new_capital_wave"],
                    emoji = "🧭",
                    next = "capital_move_opportunity",
                    fx = Effect(knowledgeDelta = 4)
                ),
                option(
                    id = "talk_family_about_change",
                    text = Strings["evt_aidar90s_capital_move_debate_opt_talk_family_about_change"],
                    emoji = "👨‍👩‍👦",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -3, knowledgeDelta = 2)
                )
            )
        ))

        put("capital_move_opportunity", event(
            id = "capital_move_opportunity",
            message = Strings["hardcoded_story_012_msg"],
            flavor = "🗺️",
            tags = setOf("career", "reflection"),
            options = listOf(
                option(
                    id = "save_for_akmola_trip",
                    text = Strings["evt_aidar90s_capital_move_opportunity_opt_save_for_akmola_trip"],
                    emoji = "🧳",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, knowledgeDelta = 8)
                ),
                option(
                    id = "just_note_trend",
                    text = Strings["evt_aidar90s_capital_move_opportunity_opt_just_note_trend"],
                    emoji = "📝",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        // ── ГЛАВА 6: Кризис 1998 (Месяц 25-36) ───────────────────────────
        put("russia_crisis_1998", event(
            id = "russia_crisis_1998",
            message = Strings["hardcoded_aidar90s_009_msg"],
            flavor = "🚨",
            tags = setOf("era", "crisis"),
            options = listOf(
                option(
                    id = "hedge_currency",
                    text = Strings["evt_aidar90s_russia_crisis_1998_opt_hedge_currency"],
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, stressDelta = -10)
                ),
                option(
                    id = "cut_business",
                    text = Strings["evt_aidar90s_russia_crisis_1998_opt_cut_business"],
                    emoji = "✂️",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = -20_000L, capitalDelta = 30_000L, stressDelta = 10)
                ),
                option(
                    id = "hold_and_pray",
                    text = Strings["evt_aidar90s_russia_crisis_1998_opt_hold_and_pray"],
                    emoji = "🙏",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 25)
                )
            )
        ))

        // ── ГЛАВА 7: Финал (Месяц 37+) ───────────────────────────────────
        put("final_choice", event(
            id = "final_choice",
            message = Strings["hardcoded_aidar90s_010_msg"],
            flavor = "🎯",
            tags = setOf("ending"),
            options = listOf(
                option(
                    id = "emigrate",
                    text = Strings["evt_aidar90s_final_choice_opt_emigrate"],
                    emoji = "✈️",
                    next = "ending_emigration",
                    fx = Effect()
                ),
                option(
                    id = "stay_build",
                    text = Strings["evt_aidar90s_final_choice_opt_stay_build"],
                    emoji = "🏗️",
                    next = "ending_business",
                    fx = Effect()
                ),
                option(
                    id = "retire_early",
                    text = Strings["evt_aidar90s_final_choice_opt_retire_early"],
                    emoji = "🏖️",
                    next = "ending_freedom",
                    fx = Effect()
                )
            )
        ))

        // ── КОНЦОВКИ ─────────────────────────────────────────────────────
        put("ending_freedom", event(
            id = "ending_freedom",
            message = Strings["hardcoded_aidar90s_011_msg"],
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            options = emptyList()
        ))

        put("ending_business", event(
            id = "ending_business",
            message = Strings["hardcoded_aidar90s_012_msg"],
            flavor = "🏢",
            isEnding = true,
            endingType = EndingType.WEALTH,
            options = emptyList()
        ))

        put("ending_emigration", event(
            id = "ending_emigration",
            message = Strings["hardcoded_aidar90s_013_msg"],
            flavor = "✈️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            options = emptyList()
        ))

        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            message = Strings["hardcoded_aidar90s_014_msg"],
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            options = emptyList()
        ))

        put("ending_paycheck", event(
            id = "ending_paycheck",
            message = Strings["hardcoded_aidar90s_015_msg"],
            flavor = "😐",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            options = emptyList()
        ))

        // ── HUB: Обычная жизнь ───────────────────────────────────────────
        put("normal_life", event(
            id = "normal_life",
            message = Strings["hardcoded_aidar90s_016_msg"],
            flavor = "📺",
            poolWeight = 20,
            options = listOf(
                option(
                    id = "buy_usd",
                    text = Strings["evt_aidar90s_normal_life_opt_buy_usd"],
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, investmentsDelta = 10_000L, knowledgeDelta = 2)
                ),
                option(
                    id = "study_books",
                    text = Strings["evt_aidar90s_normal_life_opt_study_books"],
                    emoji = "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -2)
                ),
                option(
                    id = "do_nothing",
                    text = Strings["evt_aidar90s_normal_life_opt_do_nothing"],
                    emoji = "😐",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 2)
                ),
                option(
                    id = "network_friends",
                    text = Strings["evt_aidar90s_normal_life_opt_network_friends"],
                    emoji = "🍺",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3, capitalDelta = -3_000L, stressDelta = -5)
                )
            )
        ))

        // ── ДОПОЛНИТЕЛЬНЫЕ СОБЫТИЯ ПУЛА ──────────────────────────────────
        put("job_offer", event(
            id = "job_offer",
            message = Strings["hardcoded_aidar90s_017_msg"],
            flavor = "💼",
            poolWeight = 10,
            tags = setOf("career"),
            options = listOf(
                option(
                    id = "take_job",
                    text = Strings["evt_aidar90s_job_offer_opt_take_job"],
                    emoji = "✅",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 10_000L, stressDelta = 10)
                ),
                option(
                    id = "decline_job",
                    text = Strings["evt_aidar90s_job_offer_opt_decline_job"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3, stressDelta = -5)
                )
            )
        ))

        put("health_issue", event(
            id = "health_issue",
            message = Strings["hardcoded_aidar90s_018_msg"],
            flavor = "🏥",
            poolWeight = 8,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "treat_health",
                    text = Strings["evt_aidar90s_health_issue_opt_treat_health"],
                    emoji = "💊",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, stressDelta = -15)
                ),
                option(
                    id = "ignore_health",
                    text = Strings["evt_aidar90s_health_issue_opt_ignore_health"],
                    emoji = "🤷",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = -5)
                )
            )
        ))

        put("friend_investment", event(
            id = "friend_investment",
            message = Strings["hardcoded_aidar90s_019_msg"],
            flavor = "🤝",
            poolWeight = 12,
            tags = setOf("investment", "scam"),
            options = listOf(
                option(
                    id = "invest_friend",
                    text = Strings["evt_aidar90s_friend_investment_opt_invest_friend"],
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, riskDelta = 15)
                ),
                option(
                    id = "decline_friend",
                    text = Strings["evt_aidar90s_friend_investment_opt_decline_friend"],
                    emoji = "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = 5)
                )
            )
        ))

        put("black_market", event(
            id = "black_market",
            message = Strings["hardcoded_aidar90s_020_msg"],
            flavor = "🌑",
            poolWeight = 10,
            tags = setOf("investment", "adventure"),
            options = listOf(
                option(
                    id = "buy_black_usd",
                    text = Strings["evt_aidar90s_black_market_opt_buy_black_usd"],
                    emoji = "🤫",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000L, investmentsDelta = 18_000L, riskDelta = 25)
                ),
                option(
                    id = "buy_official_usd",
                    text = Strings["evt_aidar90s_black_market_opt_buy_official_usd"],
                    emoji = "🏦",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -18_000L, investmentsDelta = 18_000L, knowledgeDelta = 3)
                )
            )
        ))

        put("family_celebration", event(
            id = "family_celebration",
            message = Strings["hardcoded_aidar90s_021_msg"],
            flavor = "🎉",
            poolWeight = 15,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "give_gift",
                    text = Strings["evt_aidar90s_family_celebration_opt_give_gift"],
                    emoji = "🎁",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -5_000L, stressDelta = -5, knowledgeDelta = 2)
                ),
                option(
                    id = "skip_celebration",
                    text = Strings["evt_aidar90s_family_celebration_opt_skip_celebration"],
                    emoji = "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = -3)
                )
            )
        ))

        put("utility_bills", event(
            id = "utility_bills",
            message = Strings["hardcoded_aidar90s_022_msg"],
            flavor = "💡",
            poolWeight = 18,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_bills",
                    text = Strings["evt_aidar90s_utility_bills_opt_pay_bills"],
                    emoji = "💳",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 8_000L, stressDelta = -5)
                ),
                option(
                    id = "pay_partial",
                    text = Strings["evt_aidar90s_utility_bills_opt_pay_partial"],
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 4_000L, stressDelta = 5)
                ),
                option(
                    id = "skip_bills",
                    text = Strings["evt_aidar90s_utility_bills_opt_skip_bills"],
                    emoji = "⚠️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 15, riskDelta = 10)
                )
            )
        ))

        put("education_opportunity", event(
            id = "education_opportunity",
            message = Strings["hardcoded_aidar90s_023_msg"],
            flavor = "🎓",
            poolWeight = 10,
            tags = setOf("career"),
            options = listOf(
                option(
                    id = "take_course",
                    text = Strings["evt_aidar90s_education_opportunity_opt_take_course"],
                    emoji = "📚",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, knowledgeDelta = 25, incomeDelta = 5_000L)
                ),
                option(
                    id = "skip_course",
                    text = Strings["evt_aidar90s_education_opportunity_opt_skip_course"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -3)
                )
            )
        ))

        put("car_purchase", event(
            id = "car_purchase",
            message = Strings["hardcoded_aidar90s_024_msg"],
            flavor = "🚗",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_car",
                    text = Strings["evt_aidar90s_car_purchase_opt_buy_car"],
                    emoji = "🔑",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -150_000L, incomeDelta = 15_000L, riskDelta = 10)
                ),
                option(
                    id = "skip_car",
                    text = Strings["evt_aidar90s_car_purchase_opt_skip_car"],
                    emoji = "🚶",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3)
                )
            )
        ))

        put("apartment_rent", event(
            id = "apartment_rent",
            message = Strings["hardcoded_aidar90s_025_msg"],
            flavor = "🏠",
            poolWeight = 15,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "accept_raise",
                    text = Strings["evt_aidar90s_apartment_rent_opt_accept_raise"],
                    emoji = "😤",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 5_000L, stressDelta = 10)
                ),
                option(
                    id = "move_out",
                    text = Strings["evt_aidar90s_apartment_rent_opt_move_out"],
                    emoji = "📦",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, expensesDelta = -2_000L, stressDelta = 15)
                )
            )
        ))

        put("winter_prep", event(
            id = "winter_prep",
            message = Strings["hardcoded_aidar90s_026_msg"],
            flavor = "❄️",
            poolWeight = 12,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "buy_fuel",
                    text = Strings["evt_aidar90s_winter_prep_opt_buy_fuel"],
                    emoji = "🪵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000L, stressDelta = -10)
                ),
                option(
                    id = "risk_cold",
                    text = Strings["evt_aidar90s_winter_prep_opt_risk_cold"],
                    emoji = "🥶",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 20, riskDelta = 15)
                )
            )
        ))

        put("tax_inspection", event(
            id = "tax_inspection",
            message = Strings["hardcoded_aidar90s_027_msg"],
            flavor = "📋",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_fine",
                    text = Strings["evt_aidar90s_tax_inspection_opt_pay_fine"],
                    emoji = "💳",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, knowledgeDelta = 10)
                ),
                option(
                    id = "bribe_tax",
                    text = Strings["evt_aidar90s_tax_inspection_opt_bribe_tax"],
                    emoji = "🤫",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, riskDelta = 30, stressDelta = 10)
                )
            )
        ))

        put("lottery_win", event(
            id = "lottery_win",
            message = Strings["hardcoded_aidar90s_028_msg"],
            flavor = "🎰",
            poolWeight = 5,
            tags = setOf("windfall"),
            options = listOf(
                option(
                    id = "save_win",
                    text = Strings["evt_aidar90s_lottery_win_opt_save_win"],
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 10_000L, knowledgeDelta = 3)
                ),
                option(
                    id = "spend_win",
                    text = Strings["evt_aidar90s_lottery_win_opt_spend_win"],
                    emoji = "🎁",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -10)
                ),
                option(
                    id = "invest_win",
                    text = Strings["evt_aidar90s_lottery_win_opt_invest_win"],
                    emoji = "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 10_000L, knowledgeDelta = 5)
                )
            )
        ))

        put("old_friend_return", event(
            id = "old_friend_return",
            message = Strings["hardcoded_aidar90s_029_msg"],
            flavor = "👋",
            poolWeight = 8,
            tags = setOf("career", "investment"),
            options = listOf(
                option(
                    id = "partner_friend",
                    text = Strings["evt_aidar90s_old_friend_return_opt_partner_friend"],
                    emoji = "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, incomeDelta = 10_000L, riskDelta = 15)
                ),
                option(
                    id = "decline_friend",
                    text = Strings["evt_aidar90s_old_friend_return_opt_decline_friend"],
                    emoji = "🙏",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = -5)
                )
            )
        ))

        put("child_birth", event(
            id = "child_birth",
            message = Strings["hardcoded_aidar90s_030_msg"],
            flavor = "👶",
            poolWeight = 4,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "accept_child",
                    text = Strings["evt_aidar90s_child_birth_opt_accept_child"],
                    emoji = "💝",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 10_000L, stressDelta = 10, knowledgeDelta = 5)
                ),
                option(
                    id = "plan_budget",
                    text = Strings["evt_aidar90s_child_birth_opt_plan_budget"],
                    emoji = "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 10_000L, knowledgeDelta = 10)
                )
            )
        ))

        put("theft_victim", event(
            id = "theft_victim",
            message = Strings["hardcoded_aidar90s_031_msg"],
            flavor = "🚨",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "report_theft",
                    text = Strings["evt_aidar90s_theft_victim_opt_report_theft"],
                    emoji = "👮",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, stressDelta = 10)
                ),
                option(
                    id = "accept_loss",
                    text = Strings["evt_aidar90s_theft_victim_opt_accept_loss"],
                    emoji = "😔",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, knowledgeDelta = 5)
                )
            )
        ))

        put("currency_exchange", event(
            id = "currency_exchange",
            message = Strings["hardcoded_aidar90s_032_msg"],
            flavor = "💱",
            poolWeight = 12,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_usd_now",
                    text = Strings["evt_aidar90s_currency_exchange_opt_buy_usd_now"],
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, investmentsDelta = 20_000L)
                ),
                option(
                    id = "wait_dip",
                    text = Strings["evt_aidar90s_currency_exchange_opt_wait_dip"],
                    emoji = "⏳",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5, stressDelta = 5)
                )
            )
        ))

        put("business_partner_betray", event(
            id = "business_partner_betray",
            message = Strings["hardcoded_aidar90s_033_msg"],
            flavor = "😡",
            poolWeight = 5,
            tags = setOf("crisis", "scam"),
            options = listOf(
                option(
                    id = "hunt_partner",
                    text = Strings["evt_aidar90s_business_partner_betray_opt_hunt_partner"],
                    emoji = "🔍",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -10_000L, stressDelta = 25, riskDelta = 20)
                ),
                option(
                    id = "write_off",
                    text = Strings["evt_aidar90s_business_partner_betray_opt_write_off"],
                    emoji = "📝",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, knowledgeDelta = 15)
                )
            )
        ))

        put("government_subsidy", event(
            id = "government_subsidy",
            message = Strings["hardcoded_aidar90s_034_msg"],
            flavor = "🏛️",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "apply_subsidy",
                    text = Strings["evt_aidar90s_government_subsidy_opt_apply_subsidy"],
                    emoji = "📋",
                    next = MONTHLY_TICK,
                    fx = Effect(debtDelta = 100_000L, capitalDelta = 100_000L, knowledgeDelta = 10)
                ),
                option(
                    id = "skip_subsidy",
                    text = Strings["evt_aidar90s_government_subsidy_opt_skip_subsidy"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)
                )
            )
        ))

        put("medical_emergency", event(
            id = "medical_emergency",
            message = Strings["hardcoded_aidar90s_035_msg"],
            flavor = "🚑",
            poolWeight = 4,
            tags = setOf("crisis", "family"),
            options = listOf(
                option(
                    id = "pay_medical",
                    text = Strings["evt_aidar90s_medical_emergency_opt_pay_medical"],
                    emoji = "💝",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000L, stressDelta = -20, setFlags = setOf("saved_relative"))
                ),
                option(
                    id = "partial_help",
                    text = Strings["evt_aidar90s_medical_emergency_opt_partial_help"],
                    emoji = "🤲",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, stressDelta = 10)
                )
            )
        ))

        put("new_year_bonus", event(
            id = "new_year_bonus",
            message = Strings["hardcoded_aidar90s_036_msg"],
            flavor = "🎄",
            poolWeight = 10,
            tags = setOf("windfall"),
            options = listOf(
                option(
                    id = "save_bonus",
                    text = Strings["evt_aidar90s_new_year_bonus_opt_save_bonus"],
                    emoji = "🛡️",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = 30_000L, knowledgeDelta = 5)
                ),
                option(
                    id = "celebrate",
                    text = Strings["evt_aidar90s_new_year_bonus_opt_celebrate"],
                    emoji = "🎉",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -15_000L, stressDelta = -20)
                ),
                option(
                    id = "invest_bonus",
                    text = Strings["evt_aidar90s_new_year_bonus_opt_invest_bonus"],
                    emoji = "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 30_000L, knowledgeDelta = 8)
                )
            )
        ))

        put("apartment_purchase", event(
            id = "apartment_purchase",
            message = Strings["hardcoded_aidar90s_037_msg"],
            flavor = "🏠",
            poolWeight = 6,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_apartment",
                    text = Strings["evt_aidar90s_apartment_purchase_opt_buy_apartment"],
                    emoji = "🔑",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -500_000L, debtDelta = 1_500_000L, debtPaymentDelta = 50_000L, investmentsDelta = 2_000_000L)
                ),
                option(
                    id = "wait_apartment",
                    text = Strings["evt_aidar90s_apartment_purchase_opt_wait_apartment"],
                    emoji = "⏳",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        put("political_change", event(
            id = "political_change",
            message = Strings["hardcoded_aidar90s_038_msg"],
            flavor = "🗳️",
            poolWeight = 8,
            tags = setOf("era"),
            options = listOf(
                option(
                    id = "adapt_law",
                    text = Strings["evt_aidar90s_political_change_opt_adapt_law"],
                    emoji = "📜",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 15, incomeDelta = -5_000L)
                ),
                option(
                    id = "go_shadow",
                    text = Strings["evt_aidar90s_political_change_opt_go_shadow"],
                    emoji = "🌑",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 10_000L, riskDelta = 30)
                )
            )
        ))

        put("newsstand_digest", event(
            id = "newsstand_digest",
            message = Strings["hardcoded_story_013_msg"],
            flavor = "🗞️",
            poolWeight = 9,
            tags = setOf("world", "reflection"),
            options = listOf(
                option(
                    id = "buy_newspaper_world_page",
                    text = Strings["evt_aidar90s_newsstand_digest_opt_buy_newspaper_world_page"],
                    emoji = "📰",
                    next = "newsstand_world_page",
                    fx = Effect(capitalDelta = -300L, knowledgeDelta = 4)
                ),
                option(
                    id = "ask_trader_about_news",
                    text = Strings["evt_aidar90s_newsstand_digest_opt_ask_trader_about_news"],
                    emoji = "🧳",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 3, stressDelta = 1)
                ),
                option(
                    id = "walk_past_newsstand",
                    text = Strings["evt_aidar90s_newsstand_digest_opt_walk_past_newsstand"],
                    emoji = "🚶",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -2)
                )
            )
        ))

        put("newsstand_world_page", event(
            id = "newsstand_world_page",
            message = Strings["hardcoded_story_014_msg"],
            flavor = "📚",
            tags = setOf("world", "reflection"),
            options = listOf(
                option(
                    id = "keep_following_context",
                    text = Strings["evt_aidar90s_newsstand_world_page_opt_keep_following_context"],
                    emoji = "🧠",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 6)
                ),
                option(
                    id = "close_newspaper_and_focus",
                    text = Strings["evt_aidar90s_newsstand_world_page_opt_close_newspaper_and_focus"],
                    emoji = "🛠️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -3)
                )
            )
        ))

        put("child_education", event(
            id = "child_education",
            message = Strings["hardcoded_aidar90s_039_msg"],
            flavor = "🎓",
            poolWeight = 10,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "private_school",
                    text = Strings["evt_aidar90s_child_education_opt_private_school"],
                    emoji = "🏫",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 50_000L, knowledgeDelta = 10)
                ),
                option(
                    id = "public_school",
                    text = Strings["evt_aidar90s_child_education_opt_public_school"],
                    emoji = "🏛️",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 5, knowledgeDelta = -5)
                )
            )
        ))

        put("retirement_planning", event(
            id = "retirement_planning",
            message = Strings["hardcoded_aidar90s_040_msg"],
            flavor = "👴",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "save_retirement",
                    text = Strings["evt_aidar90s_retirement_planning_opt_save_retirement"],
                    emoji = "🐖",
                    next = MONTHLY_TICK,
                    fx = Effect(investmentsDelta = 1_500L, capitalDelta = -1_500L, knowledgeDelta = 10)
                ),
                option(
                    id = "no_retirement",
                    text = Strings["evt_aidar90s_retirement_planning_opt_no_retirement"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)
                )
            )
        ))

        put("inflation_spike", event(
            id = "inflation_spike",
            message = Strings["hardcoded_aidar90s_041_msg"],
            flavor = "📈",
            poolWeight = 10,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "convert_all",
                    text = Strings["evt_aidar90s_inflation_spike_opt_convert_all"],
                    emoji = "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, investmentsDelta = 30_000L)
                ),
                option(
                    id = "buy_goods",
                    text = Strings["evt_aidar90s_inflation_spike_opt_buy_goods"],
                    emoji = "🛒",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -20_000L, stressDelta = -10)
                )
            )
        ))

        put("gang_protection", event(
            id = "gang_protection",
            message = Strings["hardcoded_aidar90s_042_msg"],
            flavor = "🚔",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_protection",
                    text = Strings["evt_aidar90s_gang_protection_opt_pay_protection"],
                    emoji = "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(expensesDelta = 10_000L, stressDelta = -15, riskDelta = 10)
                ),
                option(
                    id = "refuse_protection",
                    text = Strings["evt_aidar90s_gang_protection_opt_refuse_protection"],
                    emoji = "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(riskDelta = 40, stressDelta = 20)
                )
            )
        ))

        put("export_opportunity", event(
            id = "export_opportunity",
            message = Strings["hardcoded_aidar90s_043_msg"],
            flavor = "🌍",
            poolWeight = 5,
            tags = setOf("investment", "career"),
            options = listOf(
                option(
                    id = "pursue_export",
                    text = Strings["evt_aidar90s_export_opportunity_opt_pursue_export"],
                    emoji = "📋",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000L, incomeDelta = 50_000L, knowledgeDelta = 20)
                ),
                option(
                    id = "skip_export",
                    text = Strings["evt_aidar90s_export_opportunity_opt_skip_export"],
                    emoji = "❌",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -5)
                )
            )
        ))

        put("bank_collapse", event(
            id = "bank_collapse",
            message = Strings["hardcoded_aidar90s_044_msg"],
            flavor = "🏦",
            poolWeight = 4,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "accept_loss",
                    text = Strings["evt_aidar90s_bank_collapse_opt_accept_loss"],
                    emoji = "😔",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -100_000L, knowledgeDelta = 20)
                ),
                option(
                    id = "protest_bank",
                    text = Strings["evt_aidar90s_bank_collapse_opt_protest_bank"],
                    emoji = "📢",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 15, knowledgeDelta = 5)
                )
            )
        ))

        put("wedding_expense", event(
            id = "wedding_expense",
            message = Strings["hardcoded_aidar90s_045_msg"],
            flavor = "💒",
            poolWeight = 3,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "big_wedding",
                    text = Strings["evt_aidar90s_wedding_expense_opt_big_wedding"],
                    emoji = "🎉",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -200_000L, stressDelta = -20, knowledgeDelta = 5)
                ),
                option(
                    id = "small_wedding",
                    text = Strings["evt_aidar90s_wedding_expense_opt_small_wedding"],
                    emoji = "🏠",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, stressDelta = 15)
                )
            )
        ))

        put("corruption_demand", event(
            id = "corruption_demand",
            message = Strings["hardcoded_aidar90s_046_msg"],
            flavor = "🤫",
            poolWeight = 6,
            tags = setOf("crisis"),
            options = listOf(
                option(
                    id = "pay_bribe",
                    text = Strings["evt_aidar90s_corruption_demand_opt_pay_bribe"],
                    emoji = "💰",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -30_000L, riskDelta = 20)
                ),
                option(
                    id = "report_corruption",
                    text = Strings["evt_aidar90s_corruption_demand_opt_report_corruption"],
                    emoji = "📢",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 25, knowledgeDelta = 10, riskDelta = 30)
                )
            )
        ))

        put("stock_market_90s", event(
            id = "stock_market_90s",
            message = Strings["hardcoded_aidar90s_047_msg"],
            flavor = "📈",
            poolWeight = 8,
            tags = setOf("investment"),
            options = listOf(
                option(
                    id = "buy_stocks",
                    text = Strings["evt_aidar90s_stock_market_90s_opt_buy_stocks"],
                    emoji = "📊",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, riskDelta = 25)
                ),
                option(
                    id = "avoid_stocks",
                    text = Strings["evt_aidar90s_stock_market_90s_opt_avoid_stocks"],
                    emoji = "🎰",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 5)
                )
            )
        ))

        put("family_immigration", event(
            id = "family_immigration",
            message = Strings["hardcoded_aidar90s_048_msg"],
            flavor = "✈️",
            poolWeight = 5,
            tags = setOf("family"),
            options = listOf(
                option(
                    id = "join_immigration",
                    text = Strings["evt_aidar90s_family_immigration_opt_join_immigration"],
                    emoji = "🧳",
                    next = "ending_emigration",
                    fx = Effect(capitalDelta = -500_000L)
                ),
                option(
                    id = "stay_kazakhstan",
                    text = Strings["evt_aidar90s_family_immigration_opt_stay_kazakhstan"],
                    emoji = "🇰🇿",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 10, knowledgeDelta = 5)
                )
            )
        ))

        put("millennium_eve", event(
            id = "millennium_eve",
            message = Strings["hardcoded_aidar90s_049_msg"],
            flavor = "🎆",
            poolWeight = 3,
            tags = setOf("ending"),
            options = listOf(
                option(
                    id = "reflect_wisdom",
                    text = Strings["evt_aidar90s_millennium_eve_opt_reflect_wisdom"],
                    emoji = "📚",
                    next = "final_choice",
                    fx = Effect(knowledgeDelta = 20)
                ),
                option(
                    id = "reflect_money",
                    text = Strings["evt_aidar90s_millennium_eve_opt_reflect_money"],
                    emoji = "💰",
                    next = "final_choice",
                    fx = Effect(riskDelta = 20)
                )
            )
        ))
    }

    override val conditionalEvents: List<GameEvent> = listOf(
        // ОБЯЗАТЕЛЬНО: Триггер банкротства
        event(
            id = "bankruptcy_trigger",
            message = Strings["evt_aidar90s_bankruptcy_trigger_msg"],
            flavor = "💀",
            priority = 90,
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            options = listOf(
                option("accept_bankruptcy", Strings["evt_aidar90s_bankruptcy_trigger_opt_accept_bankruptcy"], "💔", next = "ending_bankruptcy", fx = Effect())
            )
        ),
        // Долговой кризис
        event(
            id = "debt_crisis",
            message = Strings["hardcoded_aidar90s_072_msg"],
            flavor = "🚨",
            priority = 100,
            conditions = listOf(cond(DEBT, GT, 100_000L)),
            tags = setOf("crisis"),
            options = listOf(
                option("debt_collect", Strings["evt_aidar90s_debt_crisis_opt_debt_collect"], "🔫", next = MONTHLY_TICK, fx = Effect(debtDelta = -50_000L, stressDelta = 30, capitalDelta = -20_000L)),
                option("debt_wait", Strings["evt_aidar90s_debt_crisis_opt_debt_wait"], "⏳", next = MONTHLY_TICK, fx = Effect(stressDelta = 10))
            )
        ),
        // Повторный скам на родителей
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
                option(
                    "stop_parents_hard",
                    Strings["evt_aidar90s_parents_scam_again_conditional_opt_stop_parents_hard"],
                    "🛑",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 20, knowledgeDelta = 10, setFlags = setOf("parents_scam_stopped"))
                ),
                option(
                    "let_them_try",
                    Strings["evt_aidar90s_parents_scam_again_conditional_opt_let_them_try"],
                    "🎲",
                    next = "parents_lost_money_2",
                    fx = Effect(stressDelta = 10, setFlags = setOf("parents_scam_stopped"))
                )
            )
        ),
        // Выгорание
        event(
            id = "burnout_90s",
            message = Strings["hardcoded_aidar90s_073_msg"],
            flavor = "🔥",
            priority = 60,
            conditions = listOf(cond(STRESS, GT, 75L)),
            cooldownMonths = 6,
            options = listOf(
                option("rest_home", Strings["evt_aidar90s_burnout_90s_opt_rest_home"], "🏠", next = MONTHLY_TICK, fx = Effect(expensesDelta = -5_000L, stressDelta = -15, incomeDelta = -5_000L)),
                option("drink_friends", Strings["evt_aidar90s_burnout_90s_opt_drink_friends"], "🍺", next = MONTHLY_TICK, fx = Effect(capitalDelta = -5_000L, stressDelta = -10, knowledgeDelta = -2))
            )
        ),
        // Введение тенге (эра-событие)
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
                option(
                    "buy_usd_tenge",
                    Strings["evt_aidar90s_era_tenge_introduced_opt_buy_usd_tenge"],
                    "💵",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        capitalDelta = -10_000_000L,
                        investmentsDelta = 10_000_000L,
                        knowledgeDelta = 5,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1L,
                            denominator = 500L
                        )
                    )
                ),
                option(
                    "hold_tenge",
                    Strings["evt_aidar90s_era_tenge_introduced_opt_hold_tenge"],
                    "🇰🇿",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        stressDelta = 10,
                        knowledgeDelta = 3,
                        monetaryReform = MonetaryReform(
                            from = CurrencyCode.RUB,
                            to = CurrencyCode.KZT,
                            numerator = 1L,
                            denominator = 500L
                        )
                    )
                )
            )
        ),
        // Конституция 1995
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
                option("buy_land", Strings["evt_aidar90s_era_constitution_1995_opt_buy_land"], "🌾", next = MONTHLY_TICK, fx = Effect(capitalDelta = -150_000L, investmentsDelta = 150_000L, knowledgeDelta = 20)),
                option("skip_land", Strings["evt_aidar90s_era_constitution_1995_opt_skip_land"], "❌", next = MONTHLY_TICK, fx = Effect(knowledgeDelta = 5))
            )
        ),
        // Кризис 1998
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
                option("hedge_currency", Strings["evt_aidar90s_era_russia_crisis_1998_opt_hedge_currency"], "💵", next = MONTHLY_TICK, fx = Effect(capitalDelta = -50_000L, investmentsDelta = 50_000L, stressDelta = -10)),
                option("cut_business", Strings["evt_aidar90s_era_russia_crisis_1998_opt_cut_business"], "✂️", next = MONTHLY_TICK, fx = Effect(incomeDelta = -20_000L, capitalDelta = 30_000L, stressDelta = 10))
            )
        ),
        // Ловушка зарплата-в-зарплату
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
                option("learn_more", Strings["evt_aidar90s_trap_warning_opt_learn_more"], "📚", next = MONTHLY_TICK, fx = Effect(knowledgeDelta = 10, stressDelta = 5)),
                option("ignore_warning", Strings["evt_aidar90s_trap_warning_opt_ignore_warning"], "😶", next = MONTHLY_TICK, fx = Effect(stressDelta = 5))
            )
        ),
        // Бонус за успех
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
                option("reinvest_bonus", Strings["evt_aidar90s_bonus_received_opt_reinvest_bonus"], "📈", next = MONTHLY_TICK, fx = Effect(investmentsDelta = 100_000L, capitalDelta = -100_000L, knowledgeDelta = 5)),
                option("celebrate_bonus", Strings["evt_aidar90s_bonus_received_opt_celebrate_bonus"], "🎊", next = MONTHLY_TICK, fx = Effect(capitalDelta = -50_000L, stressDelta = -20))
            )
        )
    )

    override val eventPool: List<PoolEntry> = buildList {
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
}
