package kz.fearsom.financiallifev2.scenarios.characters

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
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.moneyFormat
import kz.fearsom.financiallifev2.scenarios.ScamEventLibrary
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.cond
import kz.fearsom.financiallifev2.scenarios.event
import kz.fearsom.financiallifev2.scenarios.option
import kz.fearsom.financiallifev2.scenarios.story
import kz.fearsom.financiallifev2.i18n.Strings

// ─── AsanScenarioGraph ────────────────────────────────────────────────────────

/**
 * Narrative graph for "Асан" — 28-year-old Junior Android Dev, Алматы.
 *
 * Convergence example:
 *   job_offer → [startup | stable_job] → MONTHLY_TICK → pool (random or conditional)
 */
class AsanScenarioGraph : ScenarioGraph() {

    override val initialPlayerState = PlayerState(
        capital = 200_000L,
        income = 450_000L,
        expenses = 180_000L,   // rent 130k + food/transport 50k
        debt = 120_000L,
        debtPaymentMonthly = 15_000L,    // ~24% APR credit card (standard KZ consumer loan rate)
        investments = 0L,
        investmentReturnRate = 0.10,       // 10% annual (mixed ETF)
        stress = 25,
        financialKnowledge = 10,
        riskLevel = 15,
        month = 1, year = 2024,
        characterId = "asan",
        eraId = "kz_2024"
    )

    // ─────────────────────────────────────────────────────────────────────────
    // STORY EVENTS
    // ─────────────────────────────────────────────────────────────────────────

    override val events: Map<String, GameEvent> = buildMap {

        // ── INTRO ─────────────────────────────────────────────────────────────
        put(
            "intro", event(
                id = "intro",
                message = Strings["hardcoded_story_015_msg"],
                flavor = "😰",
                options = listOf(
                    option(
                        "crypto_in", Strings["evt_asan_intro_opt_crypto_in"], "🚀",
                        next = "crypto_result",
                        fx = Effect(capitalDelta = -100_000, riskDelta = 20, stressDelta = 10)
                    ),
                    option(
                        "pay_debt", Strings["evt_asan_intro_opt_pay_debt"], "💳",
                        next = "debt_paid",
                        fx = Effect(
                            capitalDelta = -120_000, debtDelta = -120_000,
                            debtPaymentDelta = -15_000, stressDelta = -8, knowledgeDelta = 5
                        )
                    ),
                    option(
                        "emergency_fund", Strings["evt_asan_intro_opt_emergency_fund"], "🛡️",
                        next = "has_cushion",
                        fx = Effect(
                            capitalDelta = -60_000, stressDelta = -4, knowledgeDelta = 6,
                            setFlags = setOf("has_emergency_fund")
                        )
                    ),
                    option(
                        "do_nothing", Strings["evt_asan_intro_opt_do_nothing"], "😶",
                        next = MONTHLY_TICK,
                        fx = Effect(stressDelta = 3)
                    )
                )
            )
        )

        // ── CRYPTO ────────────────────────────────────────────────────────────
        put(
            "crypto_result", event(
                id = "crypto_result",
                message = Strings["hardcoded_story_016_msg"],
                flavor = "😬",
                options = listOf(
                    option(
                        "double_down", Strings["evt_asan_crypto_result_opt_double_down"], "📉",
                        next = "total_loss",
                        fx = Effect(capitalDelta = -50_000, stressDelta = 20, riskDelta = 15)
                    ),
                    option(
                        "cut_losses", Strings["evt_asan_crypto_result_opt_cut_losses"], "✂️",
                        next = "lesson_learned",
                        fx = Effect(
                            capitalDelta = 60_000, stressDelta = -10, knowledgeDelta = 18,
                            setFlags = setOf("learned.scam.crypto")
                        )
                    )
                )
            )
        )

        put(
            "total_loss", event(
                id = "total_loss",
                message = Strings["hardcoded_story_017_msg"],
                flavor = "💀",
                options = listOf(
                    option(
                        "rebuild", Strings["evt_asan_total_loss_opt_rebuild"], "💪",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            stressDelta = -5, knowledgeDelta = 20,
                            setFlags = setOf("learned.scam.crypto", "lost_money_to_scam")
                        )
                    )
                )
            )
        )

        put(
            "lesson_learned", event(
                id = "lesson_learned",
                message = Strings["hardcoded_asan_001_msg"],
                flavor = "📚",
                options = listOf(
                    option(
                        "start_etf", Strings["evt_asan_lesson_learned_opt_start_etf"], "📈",
                        next = "first_etf_bought",
                        fx = Effect(
                            capitalDelta = -50_000, investmentsDelta = 50_000,
                            knowledgeDelta = 10, riskDelta = -5
                        )
                    )
                )
            )
        )

        // ── DEBT PAID ─────────────────────────────────────────────────────────
        put(
            "debt_paid", event(
                id = "debt_paid",
                message = Strings["hardcoded_asan_002_msg"],
                flavor = "🎉",
                options = listOf(
                    option(
                        "invest_freed", Strings["evt_asan_debt_paid_opt_invest_freed"], "📈",
                        next = MONTHLY_TICK,
                        fx = Effect(investmentsDelta = 15_000, knowledgeDelta = 8)
                    ),
                    option(
                        "raise_cushion", Strings["evt_asan_debt_paid_opt_raise_cushion"], "🛡️",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            stressDelta = -5, knowledgeDelta = 5,
                            setFlags = setOf("has_emergency_fund")
                        )
                    ),
                    option(
                        "lifestyle_creep", Strings["evt_asan_debt_paid_opt_lifestyle_creep"], "🏠",
                        next = MONTHLY_TICK,
                        fx = Effect(expensesDelta = 40_000, stressDelta = -3, riskDelta = 5)
                    )
                )
            )
        )

        // ── CUSHION ───────────────────────────────────────────────────────────
        put(
            "has_cushion", event(
                id = "has_cushion",
                message = Strings["hardcoded_asan_003_msg"],
                flavor = "🛡️",
                options = listOf(
                    option(
                        "use_cushion_correct", Strings["evt_asan_has_cushion_opt_use_cushion_correct"], "✅",
                        next = "cushion_worked",
                        fx = Effect(capitalDelta = -35_000, stressDelta = -10, knowledgeDelta = 12)
                    ),
                    option(
                        "take_credit_instead", Strings["evt_asan_has_cushion_opt_take_credit_instead"], "💳",
                        next = "new_credit",
                        fx = Effect(debtDelta = 38_000, debtPaymentDelta = 4_000, stressDelta = 5)
                    )
                )
            )
        )

        put(
            "cushion_worked", event(
                id = "cushion_worked",
                message = Strings["hardcoded_asan_004_msg"],
                flavor = "💪",
                options = listOf(
                    option(
                        "rebuild_cushion",
                        Strings["evt_asan_cushion_worked_opt_rebuild_cushion"],
                        "📊",
                        next = MONTHLY_TICK,
                        fx = Effect(knowledgeDelta = 5, stressDelta = -3)
                    )
                )
            )
        )

        put(
            "new_credit", event(
                id = "new_credit",
                message = Strings["hardcoded_asan_005_msg"],
                flavor = "🤔",
                options = listOf(
                    option(
                        "close_fast", Strings["evt_asan_new_credit_opt_close_fast"], "⚡",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            capitalDelta = -38_000, debtDelta = -38_000,
                            debtPaymentDelta = -4_000, knowledgeDelta = 10
                        )
                    ),
                    option(
                        "pay_minimum", Strings["evt_asan_new_credit_opt_pay_minimum"], "📈",
                        next = MONTHLY_TICK,
                        fx = Effect(investmentsDelta = 20_000, riskDelta = 5)
                    )
                )
            )
        )

        // ── JOB OFFER ─────────────────────────────────────────────────────────
        put(
            "job_offer", event(
                id = "job_offer",
                message = Strings["hardcoded_story_018_msg"],
                flavor = "💼",
                tags = setOf("career"),
                options = listOf(
                    option(
                        "take_startup", Strings["evt_asan_job_offer_opt_take_startup"], "🚀",
                        next = "startup_joined",
                        fx = Effect(incomeDelta = 250_000, riskDelta = 20, stressDelta = 15)
                    ),
                    option(
                        "negotiate_current", Strings["evt_asan_job_offer_opt_negotiate_current"], "🤝",
                        next = "negotiated_raise",
                        fx = Effect(incomeDelta = 150_000, stressDelta = -5, knowledgeDelta = 8)
                    ),
                    option(
                        "stay_safe", Strings["evt_asan_job_offer_opt_stay_safe"], "🛡️",
                        next = "promotion_soon",
                        fx = Effect(stressDelta = -8, knowledgeDelta = 3)
                    )
                )
            )
        )

        put(
            "startup_joined", event(
                id = "startup_joined",
                message = Strings["hardcoded_story_019_msg"],
                flavor = "😰",
                options = listOf(
                    option(
                        "freelance", Strings["evt_asan_startup_joined_opt_freelance"], "💻",
                        next = MONTHLY_TICK,
                        fx = Effect(incomeDelta = -200_000, knowledgeDelta = 15, riskDelta = -5)
                    ),
                    option(
                        "fast_job", Strings["evt_asan_startup_joined_opt_fast_job"], "🏃",
                        next = "back_stable",
                        fx = Effect(stressDelta = -15)
                    )
                )
            )
        )

        put(
            "negotiated_raise", event(
                id = "negotiated_raise",
                message = Strings["hardcoded_asan_006_msg"],
                flavor = "🎯",
                options = listOf(
                    option(
                        "invest_raise", Strings["evt_asan_negotiated_raise_opt_invest_raise"], "📈",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            investmentsDelta = 120_000,
                            stressDelta = -5,
                            knowledgeDelta = 5
                        )
                    ),
                    option(
                        "lifestyle_raise", Strings["evt_asan_negotiated_raise_opt_lifestyle_raise"], "🏠",
                        next = MONTHLY_TICK,
                        fx = Effect(expensesDelta = 50_000)
                    )
                )
            )
        )

        put(
            "promotion_soon", event(
                id = "promotion_soon",
                message = Strings["hardcoded_asan_007_msg"],
                flavor = "📈",
                options = listOf(
                    option(
                        "skill_up", Strings["evt_asan_promotion_soon_opt_skill_up"], "🎓",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            capitalDelta = -150_000, incomeDelta = 100_000,
                            knowledgeDelta = 15, stressDelta = 5
                        )
                    ),
                    option(
                        "invest_extra", Strings["evt_asan_promotion_soon_opt_invest_extra"], "📊",
                        next = MONTHLY_TICK,
                        fx = Effect(investmentsDelta = 100_000, knowledgeDelta = 5)
                    )
                )
            )
        )

        put(
            "back_stable", event(
                id = "back_stable",
                message = Strings["hardcoded_asan_008_msg"],
                flavor = "💼",
                options = listOf(
                    option(
                        "rule_50_30_20", Strings["evt_asan_back_stable_opt_rule_50_30_20"], "📊",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            knowledgeDelta = 20, stressDelta = -10,
                            investmentsDelta = 100_000
                        )
                    )
                )
            )
        )

        // ── ETF & INVESTING ───────────────────────────────────────────────────
        put(
            "first_etf_bought", event(
                id = "first_etf_bought",
                message = Strings["hardcoded_asan_009_msg"],
                flavor = "📈",
                tags = setOf("investment"),
                options = listOf(
                    option(
                        "dca_strategy", Strings["evt_asan_first_etf_bought_opt_dca_strategy"], "📅",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            investmentsDelta = 30_000, knowledgeDelta = 8,
                            stressDelta = -3
                        )
                    ),
                    option(
                        "lump_sum", Strings["evt_asan_first_etf_bought_opt_lump_sum"], "💰",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            capitalDelta = -100_000, investmentsDelta = 100_000,
                            riskDelta = 5
                        )
                    )
                )
            )
        )

        // ── MORTGAGE ──────────────────────────────────────────────────────────
        put(
            "mortgage_offer", event(
                id = "mortgage_offer",
                conditions = listOf(cond(CAPITAL, GTE, 2_800_000L), cond(DEBT, LTE, 200_000L)),
                message = Strings["hardcoded_asan_010_msg"],
                flavor = "🏠",
                options = listOf(
                    option(
                        "mortgage_yes", Strings["evt_asan_mortgage_offer_opt_mortgage_yes"], "🏠",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            capitalDelta = -3_600_000, debtDelta = 14_400_000,
                            debtPaymentDelta = 115_000, expensesDelta = -130_000,
                            stressDelta = 20, knowledgeDelta = 10
                        )
                    ),
                    option(
                        "save_more", Strings["evt_asan_mortgage_offer_opt_save_more"], "⏳",
                        next = MONTHLY_TICK,
                        fx = Effect(stressDelta = -5, knowledgeDelta = 8)
                    ),
                    option(
                        "rent_invest", Strings["evt_asan_mortgage_offer_opt_rent_invest"], "📈",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            investmentsDelta = 130_000, knowledgeDelta = 12,
                            riskDelta = -3
                        )
                    )
                )
            )
        )

        // ── SENIOR / FINANCIAL FREEDOM ────────────────────────────────────────
        put(
            "senior_offer", event(
                id = "senior_offer",
                message = Strings["hardcoded_story_020_msg"],
                flavor = "🚀",
                tags = setOf("career"),
                options = listOf(
                    option(
                        "accept_senior", Strings["evt_asan_senior_offer_opt_accept_senior"], "🎯",
                        next = "financial_freedom_path",
                        fx = Effect(
                            incomeDelta = 450_000, stressDelta = 5,
                            knowledgeDelta = 10
                        )
                    )
                )
            )
        )

        put(
            "financial_freedom_path", event(
                id = "financial_freedom_path",
                message = Strings["hardcoded_asan_011_msg"],
                flavor = "🎯",
                options = listOf(
                    option(
                        "keep_going", Strings["evt_asan_financial_freedom_path_opt_keep_going"], "🏆",
                        next = "ending_freedom",
                        fx = Effect(knowledgeDelta = 10, stressDelta = -15)
                    )
                )
            )
        )

        // ── NORMAL LIFE (monthly tick convergence hub) ────────────────────────
        put(
            "normal_life", event(
                id = "normal_life",
                message = Strings["hardcoded_asan_012_msg"],
                flavor = "☀️",
                options = listOf(
                    option(
                        "focus_savings", Strings["evt_asan_normal_life_opt_focus_savings"], "💰",
                        next = MONTHLY_TICK,
                        fx = Effect(expensesDelta = -10_000, stressDelta = 2, knowledgeDelta = 2)
                    ),
                    option(
                        "focus_invest", Strings["evt_asan_normal_life_opt_focus_invest"], "📈",
                        next = MONTHLY_TICK,
                        fx = Effect(
                            investmentsDelta = 50_000, capitalDelta = -50_000,
                            knowledgeDelta = 3
                        )
                    ),
                    option(
                        "focus_skills", Strings["evt_asan_normal_life_opt_focus_skills"], "🎓",
                        next = "skill_check",
                        fx = Effect(knowledgeDelta = 8, stressDelta = 3)
                    ),
                    option(
                        "check_career", Strings["evt_asan_normal_life_opt_check_career"], "🔍",
                        next = "job_offer",
                        fx = Effect()
                    )
                )
            )
        )

        put(
            "skill_check", event(
                id = "skill_check",
                message = Strings["hardcoded_asan_013_msg"],
                flavor = "📝",
                tags = setOf("career"),
                options = listOf(
                    option(
                        "talk_recruiter", Strings["evt_asan_skill_check_opt_talk_recruiter"], "📞",
                        next = "senior_offer",
                        fx = Effect(knowledgeDelta = 5)
                    ),
                    option(
                        "not_ready", Strings["evt_asan_skill_check_opt_not_ready"], "📚",
                        next = MONTHLY_TICK,
                        fx = Effect(knowledgeDelta = 10, stressDelta = -2)
                    )
                )
            )
        )

        // ── ENDINGS ───────────────────────────────────────────────────────────
        put(
            "ending_freedom", event(
                id = "ending_freedom",
                message = Strings["hardcoded_story_021_msg"],
                flavor = "🏆",
                isEnding = true,
                endingType = EndingType.FINANCIAL_FREEDOM,
                options = emptyList()
            )
        )

        put(
            "ending_bankruptcy", event(
                id = "ending_bankruptcy",
                message = Strings["hardcoded_story_022_msg"],
                flavor = "💔",
                isEnding = true,
                endingType = EndingType.BANKRUPTCY,
                options = emptyList()
            )
        )

        put(
            "ending_paycheck", event(
                id = "ending_paycheck",
                message = Strings["hardcoded_story_023_msg"],
                flavor = "😔",
                isEnding = true,
                endingType = EndingType.PAYCHECK_TO_PAYCHECK,
                options = emptyList()
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONDITIONAL EVENTS  (priority-checked after every monthly tick)
    // ─────────────────────────────────────────────────────────────────────────

    override val conditionalEvents: List<GameEvent> = listOf(

        // CRISIS: debt > 1 000 000 — highest priority
        event(
            id = "debt_crisis",
            message = Strings["hardcoded_asan_014_msg"],
            flavor = "🚨",
            priority = 100,
            conditions = listOf(cond(DEBT, GT, 1_000_000L)),
            tags = setOf("debt", "crisis"),
            options = listOf(
                option(
                    "debt_restructure", Strings["evt_asan_debt_crisis_opt_debt_restructure"], "🤝",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        debtPaymentDelta = 20_000, stressDelta = -20,
                        knowledgeDelta = 15
                    )
                ),
                option(
                    "sell_assets", Strings["evt_asan_debt_crisis_opt_sell_assets"], "💸",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        investmentsDelta = -300_000, capitalDelta = -200_000,
                        debtDelta = -500_000, stressDelta = -25
                    )
                )
            )
        ),

        // BANKRUPTCY TRIGGER: capital = 0, stress >= 90
        event(
            id = "bankruptcy_trigger",
            message = Strings["evt_asan_bankruptcy_trigger_msg"],
            flavor = "💀",
            priority = 90,
            conditions = listOf(
                cond(CAPITAL, LTE, 0L),
                cond(STRESS, GTE, 90L)
            ),
            options = listOf(
                option(
                    "accept_bankruptcy", Strings["evt_asan_bankruptcy_trigger_opt_accept_bankruptcy"], "💔",
                    next = "ending_bankruptcy",
                    fx = Effect()
                )
            )
        ),

        // PAYCHECK-TO-PAYCHECK WARNING
        event(
            id = "trap_warning",
            message = Strings["hardcoded_asan_015_msg"],
            flavor = "⚠️",
            priority = 50,
            conditions = listOf(
                cond(CAPITAL, LTE, 50_000L),
                cond(KNOWLEDGE, LTE, 15L),
                cond(MONTH, GTE, 6L)
            ),
            tags = setOf("debt"),
            options = listOf(
                option(
                    "budget_app", Strings["evt_asan_trap_warning_opt_budget_app"], "📱",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        expensesDelta = -30_000, knowledgeDelta = 15,
                        stressDelta = -5
                    )
                ),
                option(
                    "second_income", Strings["evt_asan_trap_warning_opt_second_income"], "💼",
                    next = MONTHLY_TICK,
                    fx = Effect(incomeDelta = 80_000, stressDelta = 8)
                )
            )
        ),

        // WINDFALL: annual bonus
        event(
            id = "bonus_received",
            message = Strings["hardcoded_asan_016_msg"],
            flavor = "🎁",
            priority = 30,
            conditions = listOf(
                cond(CAPITAL, GTE, 500_000L),
                cond(KNOWLEDGE, GTE, 30L)
            ),
            cooldownMonths = 12,
            options = listOf(
                option(
                    "bonus_invest", Strings["evt_asan_bonus_received_opt_bonus_invest"], "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        investmentsDelta = 200_000,
                        capitalDelta = -200_000,
                        knowledgeDelta = 5
                    )
                ),
                option(
                    "bonus_cushion", Strings["evt_asan_bonus_received_opt_bonus_cushion"], "🛡️",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        capitalDelta = 200_000,
                        stressDelta = -8,
                        knowledgeDelta = 3
                    )
                ),
                option(
                    "bonus_experience", Strings["evt_asan_bonus_received_opt_bonus_experience"], "🎉",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = -12)
                )
            )
        ),

        // MORTGAGE UNLOCK: capital >= 2.8M, low debt
        event(
            id = "mortgage_unlock",
            message = Strings["hardcoded_asan_017_msg"],
            flavor = "🏠",
            priority = 40,
            conditions = listOf(
                cond(CAPITAL, GTE, 2_800_000L),
                cond(DEBT, LTE, 200_000L)
            ),
            unique = true,
            options = listOf(
                option(
                    "consider_mortgage", Strings["evt_asan_mortgage_unlock_opt_consider_mortgage"], "🏠",
                    next = "mortgage_offer",
                    fx = Effect()
                ),
                option(
                    "keep_investing", Strings["evt_asan_mortgage_unlock_opt_keep_investing"], "📈",
                    next = MONTHLY_TICK,
                    fx = Effect(knowledgeDelta = 8)
                )
            )
        ),

        // HIGH STRESS: stress > 70
        event(
            id = "burnout_risk",
            message = Strings["hardcoded_asan_018_msg"],
            flavor = "🔥",
            priority = 60,
            conditions = listOf(cond(STRESS, GT, 70L)),
            cooldownMonths = 6,
            options = listOf(
                option(
                    "take_vacation", Strings["evt_asan_burnout_risk_opt_take_vacation"], "🌴",
                    next = MONTHLY_TICK,
                    fx = Effect(capitalDelta = -80_000, stressDelta = -30)
                ),
                option(
                    "therapy", Strings["evt_asan_burnout_risk_opt_therapy"], "🧠",
                    next = MONTHLY_TICK,
                    fx = Effect(
                        expensesDelta = 20_000, stressDelta = -15,
                        knowledgeDelta = 5
                    )
                ),
                option(
                    "push_through", Strings["evt_asan_burnout_risk_opt_push_through"], "💪",
                    next = MONTHLY_TICK,
                    fx = Effect(stressDelta = 5, knowledgeDelta = 3)
                )
            )
        )
    )

    // ─────────────────────────────────────────────────────────────────────────
    // EVENT POOL  (weighted pool drawn after a monthly tick)
    // ─────────────────────────────────────────────────────────────────────────

    override val eventPool: List<PoolEntry> = buildList {
        // Character-specific story events
        add(PoolEntry("normal_life", 20))
        add(PoolEntry("job_offer", 12))
        add(PoolEntry("mortgage_offer", 5))
        add(PoolEntry("skill_check", 10))
        // Shared scam events — gated by their own conditions + NotFlag
        addAll(ScamEventLibrary.poolEntries)
    }
}
