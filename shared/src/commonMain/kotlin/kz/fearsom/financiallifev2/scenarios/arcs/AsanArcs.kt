package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.i18n.Strings
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.CAPITAL
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.DEBT
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.KNOWLEDGE
import kz.fearsom.financiallifev2.model.Condition.Stat.Field.MONTH
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

fun asanStoryArc(): EventArc = EventArc { map ->
    map["intro"] = event(
        id = "intro",
        message = Strings["hardcoded_story_015_msg"],
        flavor = "😰",
        options = listOf(
            option("crypto_in", Strings["evt_asan_intro_opt_crypto_in"], "🚀", "crypto_result",
                Effect(capitalDelta = -100_000, riskDelta = 20, stressDelta = 10)),
            option("pay_debt", Strings["evt_asan_intro_opt_pay_debt"], "💳", "debt_paid",
                Effect(capitalDelta = -120_000, debtDelta = -120_000, debtPaymentDelta = -15_000, stressDelta = -8, knowledgeDelta = 5)),
            option("emergency_fund", Strings["evt_asan_intro_opt_emergency_fund"], "🛡️", "has_cushion",
                Effect(capitalDelta = -60_000, stressDelta = -4, knowledgeDelta = 6, setFlags = setOf("has_emergency_fund"))),
            option("do_nothing", Strings["evt_asan_intro_opt_do_nothing"], "😶", MONTHLY_TICK,
                Effect(stressDelta = 3))
        )
    )
    map["crypto_result"] = event(
        id = "crypto_result",
        message = Strings["hardcoded_story_016_msg"],
        flavor = "😬",
        options = listOf(
            option("double_down", Strings["evt_asan_crypto_result_opt_double_down"], "📉", "total_loss",
                Effect(capitalDelta = -50_000, stressDelta = 20, riskDelta = 15)),
            option("cut_losses", Strings["evt_asan_crypto_result_opt_cut_losses"], "✂️", "lesson_learned",
                Effect(capitalDelta = 60_000, stressDelta = -10, knowledgeDelta = 18, setFlags = setOf("learned.scam.crypto")))
        )
    )
    map["total_loss"] = event(
        id = "total_loss",
        message = Strings["hardcoded_story_017_msg"],
        flavor = "💀",
        options = listOf(
            option("rebuild", Strings["evt_asan_total_loss_opt_rebuild"], "💪", MONTHLY_TICK,
                Effect(stressDelta = -5, knowledgeDelta = 20, setFlags = setOf("learned.scam.crypto", "lost_money_to_scam")))
        )
    )
    map["lesson_learned"] = event(
        id = "lesson_learned",
        message = Strings["hardcoded_asan_001_msg"],
        flavor = "📚",
        options = listOf(
            option("start_etf", Strings["evt_asan_lesson_learned_opt_start_etf"], "📈", "first_etf_bought",
                Effect(capitalDelta = -50_000, investmentsDelta = 50_000, knowledgeDelta = 10, riskDelta = -5))
        )
    )
    map["debt_paid"] = event(
        id = "debt_paid",
        message = Strings["hardcoded_asan_002_msg"],
        flavor = "🎉",
        options = listOf(
            option("invest_freed", Strings["evt_asan_debt_paid_opt_invest_freed"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 15_000, knowledgeDelta = 8)),
            option("raise_cushion", Strings["evt_asan_debt_paid_opt_raise_cushion"], "🛡️", MONTHLY_TICK,
                Effect(stressDelta = -5, knowledgeDelta = 5, setFlags = setOf("has_emergency_fund"))),
            option("lifestyle_creep", Strings["evt_asan_debt_paid_opt_lifestyle_creep"], "🏠", MONTHLY_TICK,
                Effect(expensesDelta = 40_000, stressDelta = -3, riskDelta = 5))
        )
    )
    map["has_cushion"] = event(
        id = "has_cushion",
        message = Strings["hardcoded_asan_003_msg"],
        flavor = "🛡️",
        options = listOf(
            option("use_cushion_correct", Strings["evt_asan_has_cushion_opt_use_cushion_correct"], "✅", "cushion_worked",
                Effect(capitalDelta = -35_000, stressDelta = -10, knowledgeDelta = 12)),
            option("take_credit_instead", Strings["evt_asan_has_cushion_opt_take_credit_instead"], "💳", "new_credit",
                Effect(debtDelta = 38_000, debtPaymentDelta = 4_000, stressDelta = 5))
        )
    )
    map["cushion_worked"] = event(
        id = "cushion_worked",
        message = Strings["hardcoded_asan_004_msg"],
        flavor = "💪",
        options = listOf(
            option("rebuild_cushion", Strings["evt_asan_cushion_worked_opt_rebuild_cushion"], "📊", MONTHLY_TICK,
                Effect(knowledgeDelta = 5, stressDelta = -3))
        )
    )
    map["new_credit"] = event(
        id = "new_credit",
        message = Strings["hardcoded_asan_005_msg"],
        flavor = "🤔",
        options = listOf(
            option("close_fast", Strings["evt_asan_new_credit_opt_close_fast"], "⚡", MONTHLY_TICK,
                Effect(capitalDelta = -38_000, debtDelta = -38_000, debtPaymentDelta = -4_000, knowledgeDelta = 10)),
            option("pay_minimum", Strings["evt_asan_new_credit_opt_pay_minimum"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 20_000, riskDelta = 5))
        )
    )
    map["job_offer"] = event(
        id = "job_offer",
        message = Strings["hardcoded_story_018_msg"],
        flavor = "💼",
        tags = setOf("career"),
        options = listOf(
            option("take_startup", Strings["evt_asan_job_offer_opt_take_startup"], "🚀", "startup_joined",
                Effect(incomeDelta = 250_000, riskDelta = 20, stressDelta = 15)),
            option("negotiate_current", Strings["evt_asan_job_offer_opt_negotiate_current"], "🤝", "negotiated_raise",
                Effect(incomeDelta = 150_000, stressDelta = -5, knowledgeDelta = 8)),
            option("stay_safe", Strings["evt_asan_job_offer_opt_stay_safe"], "🛡️", "promotion_soon",
                Effect(stressDelta = -8, knowledgeDelta = 3))
        )
    )
    map["startup_joined"] = event(
        id = "startup_joined",
        message = Strings["hardcoded_story_019_msg"],
        flavor = "😰",
        options = listOf(
            option("freelance", Strings["evt_asan_startup_joined_opt_freelance"], "💻", MONTHLY_TICK,
                Effect(incomeDelta = -200_000, knowledgeDelta = 15, riskDelta = -5)),
            option("fast_job", Strings["evt_asan_startup_joined_opt_fast_job"], "🏃", "back_stable",
                Effect(stressDelta = -15))
        )
    )
    map["negotiated_raise"] = event(
        id = "negotiated_raise",
        message = Strings["hardcoded_asan_006_msg"],
        flavor = "🎯",
        options = listOf(
            option("invest_raise", Strings["evt_asan_negotiated_raise_opt_invest_raise"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 120_000, stressDelta = -5, knowledgeDelta = 5)),
            option("lifestyle_raise", Strings["evt_asan_negotiated_raise_opt_lifestyle_raise"], "🏠", MONTHLY_TICK,
                Effect(expensesDelta = 50_000))
        )
    )
    map["promotion_soon"] = event(
        id = "promotion_soon",
        message = Strings["hardcoded_asan_007_msg"],
        flavor = "📈",
        options = listOf(
            option("skill_up", Strings["evt_asan_promotion_soon_opt_skill_up"], "🎓", MONTHLY_TICK,
                Effect(capitalDelta = -150_000, incomeDelta = 100_000, knowledgeDelta = 15, stressDelta = 5)),
            option("invest_extra", Strings["evt_asan_promotion_soon_opt_invest_extra"], "📊", MONTHLY_TICK,
                Effect(investmentsDelta = 100_000, knowledgeDelta = 5))
        )
    )
    map["back_stable"] = event(
        id = "back_stable",
        message = Strings["hardcoded_asan_008_msg"],
        flavor = "💼",
        options = listOf(
            option("rule_50_30_20", Strings["evt_asan_back_stable_opt_rule_50_30_20"], "📊", MONTHLY_TICK,
                Effect(knowledgeDelta = 20, stressDelta = -10, investmentsDelta = 100_000))
        )
    )
    map["first_etf_bought"] = event(
        id = "first_etf_bought",
        message = Strings["hardcoded_asan_009_msg"],
        flavor = "📈",
        tags = setOf("investment"),
        options = listOf(
            option("dca_strategy", Strings["evt_asan_first_etf_bought_opt_dca_strategy"], "📅", MONTHLY_TICK,
                Effect(investmentsDelta = 30_000, knowledgeDelta = 8, stressDelta = -3)),
            option("lump_sum", Strings["evt_asan_first_etf_bought_opt_lump_sum"], "💰", MONTHLY_TICK,
                Effect(capitalDelta = -100_000, investmentsDelta = 100_000, riskDelta = 5))
        )
    )
    map["mortgage_offer"] = event(
        id = "mortgage_offer",
        conditions = listOf(cond(CAPITAL, GTE, 2_800_000L), cond(DEBT, LTE, 200_000L)),
        message = Strings["hardcoded_asan_010_msg"],
        flavor = "🏠",
        options = listOf(
            option("mortgage_yes", Strings["evt_asan_mortgage_offer_opt_mortgage_yes"], "🏠", MONTHLY_TICK,
                Effect(capitalDelta = -3_600_000, debtDelta = 14_400_000, debtPaymentDelta = 115_000,
                    expensesDelta = -130_000, stressDelta = 20, knowledgeDelta = 10)),
            option("save_more", Strings["evt_asan_mortgage_offer_opt_save_more"], "⏳", MONTHLY_TICK,
                Effect(stressDelta = -5, knowledgeDelta = 8)),
            option("rent_invest", Strings["evt_asan_mortgage_offer_opt_rent_invest"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 130_000, knowledgeDelta = 12, riskDelta = -3))
        )
    )
    map["senior_offer"] = event(
        id = "senior_offer",
        message = Strings["hardcoded_story_020_msg"],
        flavor = "🚀",
        tags = setOf("career"),
        options = listOf(
            option("accept_senior", Strings["evt_asan_senior_offer_opt_accept_senior"], "🎯", "financial_freedom_path",
                Effect(incomeDelta = 450_000, stressDelta = 5, knowledgeDelta = 10))
        )
    )
    map["financial_freedom_path"] = event(
        id = "financial_freedom_path",
        message = Strings["hardcoded_asan_011_msg"],
        flavor = "🎯",
        options = listOf(
            option("keep_going", Strings["evt_asan_financial_freedom_path_opt_keep_going"], "🏆", "ending_freedom",
                Effect(knowledgeDelta = 10, stressDelta = -15))
        )
    )
    map["skill_check"] = event(
        id = "skill_check",
        message = Strings["hardcoded_asan_013_msg"],
        flavor = "📝",
        tags = setOf("career"),
        options = listOf(
            option("talk_recruiter", Strings["evt_asan_skill_check_opt_talk_recruiter"], "📞", "senior_offer",
                Effect(knowledgeDelta = 5)),
            option("not_ready", Strings["evt_asan_skill_check_opt_not_ready"], "📚", MONTHLY_TICK,
                Effect(knowledgeDelta = 10, stressDelta = -2))
        )
    )
}

fun asanNormalLifeArc(): EventArc = EventArc { map ->
    map["normal_life"] = event(
        id = "normal_life",
        message = Strings["hardcoded_asan_012_msg"],
        flavor = "☀️",
        options = listOf(
            option("focus_savings", Strings["evt_asan_normal_life_opt_focus_savings"], "💰", MONTHLY_TICK,
                Effect(expensesDelta = -10_000, stressDelta = 2, knowledgeDelta = 2)),
            option("focus_invest", Strings["evt_asan_normal_life_opt_focus_invest"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 50_000, capitalDelta = -50_000, knowledgeDelta = 3)),
            option("focus_skills", Strings["evt_asan_normal_life_opt_focus_skills"], "🎓", "skill_check",
                Effect(knowledgeDelta = 8, stressDelta = 3)),
            option("check_career", Strings["evt_asan_normal_life_opt_check_career"], "🔍", "job_offer",
                Effect())
        )
    )
}

fun asanEndingsArc(): EventArc = EventArc { map ->
    map["ending_freedom"] = event(
        id = "ending_freedom",
        message = Strings["hardcoded_story_021_msg"],
        flavor = "🏆",
        isEnding = true,
        endingType = EndingType.FINANCIAL_FREEDOM,
        options = emptyList()
    )
    map["ending_bankruptcy"] = event(
        id = "ending_bankruptcy",
        message = Strings["hardcoded_story_022_msg"],
        flavor = "💔",
        isEnding = true,
        endingType = EndingType.BANKRUPTCY,
        options = emptyList()
    )
    map["ending_paycheck"] = event(
        id = "ending_paycheck",
        message = Strings["hardcoded_story_023_msg"],
        flavor = "😔",
        isEnding = true,
        endingType = EndingType.PAYCHECK_TO_PAYCHECK,
        options = emptyList()
    )
}

fun asanConditionals(): List<GameEvent> = listOf(
    event(
        id = "debt_crisis",
        message = Strings["hardcoded_asan_014_msg"],
        flavor = "🚨",
        priority = 100,
        conditions = listOf(cond(DEBT, GT, 1_000_000L)),
        tags = setOf("debt", "crisis"),
        options = listOf(
            option("debt_restructure", Strings["evt_asan_debt_crisis_opt_debt_restructure"], "🤝", MONTHLY_TICK,
                Effect(debtPaymentDelta = 20_000, stressDelta = -20, knowledgeDelta = 15)),
            option("sell_assets", Strings["evt_asan_debt_crisis_opt_sell_assets"], "💸", MONTHLY_TICK,
                Effect(investmentsDelta = -300_000, capitalDelta = -200_000, debtDelta = -500_000, stressDelta = -25))
        )
    ),
    event(
        id = "bankruptcy_trigger",
        message = Strings["evt_asan_bankruptcy_trigger_msg"],
        flavor = "💀",
        priority = 90,
        conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
        options = listOf(
            option("accept_bankruptcy", Strings["evt_asan_bankruptcy_trigger_opt_accept_bankruptcy"], "💔", "ending_bankruptcy",
                Effect())
        )
    ),
    event(
        id = "trap_warning",
        message = Strings["hardcoded_asan_015_msg"],
        flavor = "⚠️",
        priority = 50,
        conditions = listOf(cond(CAPITAL, LTE, 50_000L), cond(KNOWLEDGE, LTE, 15L), cond(MONTH, GTE, 6L)),
        tags = setOf("debt"),
        options = listOf(
            option("budget_app", Strings["evt_asan_trap_warning_opt_budget_app"], "📱", MONTHLY_TICK,
                Effect(expensesDelta = -30_000, knowledgeDelta = 15, stressDelta = -5)),
            option("second_income", Strings["evt_asan_trap_warning_opt_second_income"], "💼", MONTHLY_TICK,
                Effect(incomeDelta = 80_000, stressDelta = 8))
        )
    ),
    event(
        id = "bonus_received",
        message = Strings["hardcoded_asan_016_msg"],
        flavor = "🎁",
        priority = 30,
        conditions = listOf(cond(CAPITAL, GTE, 500_000L), cond(KNOWLEDGE, GTE, 30L)),
        cooldownMonths = 12,
        options = listOf(
            option("bonus_invest", Strings["evt_asan_bonus_received_opt_bonus_invest"], "📈", MONTHLY_TICK,
                Effect(investmentsDelta = 200_000, capitalDelta = -200_000, knowledgeDelta = 5)),
            option("bonus_cushion", Strings["evt_asan_bonus_received_opt_bonus_cushion"], "🛡️", MONTHLY_TICK,
                Effect(capitalDelta = 200_000, stressDelta = -8, knowledgeDelta = 3)),
            option("bonus_experience", Strings["evt_asan_bonus_received_opt_bonus_experience"], "🎉", MONTHLY_TICK,
                Effect(stressDelta = -12))
        )
    ),
    event(
        id = "mortgage_unlock",
        message = Strings["hardcoded_asan_017_msg"],
        flavor = "🏠",
        priority = 40,
        conditions = listOf(cond(CAPITAL, GTE, 2_800_000L), cond(DEBT, LTE, 200_000L)),
        unique = true,
        options = listOf(
            option("consider_mortgage", Strings["evt_asan_mortgage_unlock_opt_consider_mortgage"], "🏠", "mortgage_offer",
                Effect()),
            option("keep_investing", Strings["evt_asan_mortgage_unlock_opt_keep_investing"], "📈", MONTHLY_TICK,
                Effect(knowledgeDelta = 8))
        )
    ),
    event(
        id = "burnout_risk",
        message = Strings["hardcoded_asan_018_msg"],
        flavor = "🔥",
        priority = 60,
        conditions = listOf(cond(STRESS, GT, 70L)),
        cooldownMonths = 6,
        options = listOf(
            option("take_vacation", Strings["evt_asan_burnout_risk_opt_take_vacation"], "🌴", MONTHLY_TICK,
                Effect(capitalDelta = -80_000, stressDelta = -30)),
            option("therapy", Strings["evt_asan_burnout_risk_opt_therapy"], "🧠", MONTHLY_TICK,
                Effect(expensesDelta = 20_000, stressDelta = -15, knowledgeDelta = 5)),
            option("push_through", Strings["evt_asan_burnout_risk_opt_push_through"], "💪", MONTHLY_TICK,
                Effect(stressDelta = 5, knowledgeDelta = 3))
        )
    )
)

fun asanEventPool(): List<PoolEntry> = buildList {
    add(PoolEntry("normal_life", 20))
    add(PoolEntry("job_offer", 12))
    add(PoolEntry("mortgage_offer", 5))
    add(PoolEntry("skill_check", 10))
    addAll(ScamEventLibrary.poolEntries)
}
