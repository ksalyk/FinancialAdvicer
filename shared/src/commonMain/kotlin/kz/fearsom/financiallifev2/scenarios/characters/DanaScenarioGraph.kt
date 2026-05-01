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

class DanaScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    override val initialPlayerState = when (eraId) {
        "kz_2005" -> PlayerState(
            capital = 320_000L,
            income = 110_000L,
            expenses = 95_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.05,
            stress = 42,
            financialKnowledge = 18,
            riskLevel = 8,
            month = 1,
            year = 2005,
            characterId = "dana",
            eraId = eraId
        )
        "kz_2015" -> PlayerState(
            capital = 900_000L,
            income = 260_000L,
            expenses = 225_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.06,
            stress = 44,
            financialKnowledge = 22,
            riskLevel = 10,
            month = 1,
            year = 2015,
            characterId = "dana",
            eraId = eraId
        )
        else -> PlayerState(
            capital = 1_100_000L,
            income = 390_000L,
            expenses = 310_000L,
            debt = 0L,
            debtPaymentMonthly = 0L,
            investments = 0L,
            investmentReturnRate = 0.06,
            stress = 46,
            financialKnowledge = 26,
            riskLevel = 12,
            month = 1,
            year = 2024,
            characterId = "dana",
            eraId = eraId
        )
    }

    override val events: Map<String, GameEvent> = when (eraId) {
        "kz_2005" -> dana2005Events()
        "kz_2015" -> dana2015Events()
        else -> dana2024Events()
    }

    override val conditionalEvents: List<GameEvent> = when (eraId) {
        "kz_2005" -> commonDanaConditionals(
            burnoutIntro = Strings["evt_dana_kz2005_burnout_label"],
            investmentIntro = Strings["evt_dana_kz2005_investment_label"]
        )
        "kz_2015" -> commonDanaConditionals(
            burnoutIntro = Strings["evt_dana_kz2015_burnout_label"],
            investmentIntro = Strings["evt_dana_kz2015_investment_label"]
        )
        else -> commonDanaConditionals(
            burnoutIntro = Strings["evt_dana_kz2024_burnout_label"],
            investmentIntro = Strings["evt_dana_kz2024_investment_label"]
        )
    }

    override val eventPool: List<PoolEntry> = listOf(
        PoolEntry("normal_life", 18),
        PoolEntry("husband_layoff", 8),
        PoolEntry("child_education", 7)
    ) + ScamEventLibrary.poolEntries

    private fun dana2005Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "👩‍🏫",
            message = Strings["evt_dana_2005_intro_msg"],
            options = listOf(
                option("start_tutoring", Strings["evt_dana_2005_intro_opt_start_tutoring"], "✏️", "tutoring_platform",
                    Effect(knowledgeDelta = 3)),
                option("explore_mortgage", Strings["evt_dana_2005_intro_opt_explore_mortgage"], "🏠", "mortgage_decision",
                    Effect(knowledgeDelta = 5))
            )
        ))
        put("tutoring_platform", event(
            id = "tutoring_platform",
            flavor = "📚",
            message = Strings["evt_dana_2005_tutoring_platform_msg"],
            options = listOf(
                option("join_platform", Strings["evt_dana_2005_tutoring_platform_opt_join_platform"], "💻", "tutoring_growth",
                    Effect(incomeDelta = 35_000L, stressDelta = 10, knowledgeDelta = 5)),
                option("skip_platform", Strings["evt_dana_2005_tutoring_platform_opt_skip_platform"], "👧", "mortgage_decision",
                    Effect(stressDelta = -5))
            )
        ))
        put("tutoring_growth", event(
            id = "tutoring_growth",
            flavor = "🪜",
            message = Strings["evt_dana_2005_tutoring_growth_msg"],
            options = listOf(
                option("launch_school", Strings["evt_dana_2005_tutoring_growth_opt_launch_school"], "🏫", MONTHLY_TICK,
                    Effect(capitalDelta = -120_000L, incomeDelta = 70_000L, stressDelta = 18, knowledgeDelta = 8,
                        setFlags = setOf("dana.school.started"))),
                option("stay_quiet", Strings["evt_dana_2005_tutoring_growth_opt_stay_quiet"], "🛡️", "mortgage_decision",
                    Effect(stressDelta = -4, knowledgeDelta = 4))
            )
        ))
        put("mortgage_decision", event(
            id = "mortgage_decision",
            flavor = "🏡",
            message = Strings["evt_dana_2005_mortgage_decision_msg"],
            options = listOf(
                option("take_mortgage", Strings["evt_dana_2005_mortgage_decision_opt_take_mortgage"], "🔑", "child_education",
                    Effect(capitalDelta = -1_000_000L, debtDelta = 4_500_000L, expensesDelta = 55_000L, stressDelta = 18, knowledgeDelta = 6,
                        setFlags = setOf("dana.has.mortgage"))),
                option("save_more", Strings["evt_dana_2005_mortgage_decision_opt_save_more"], "⏳", "child_education",
                    Effect(knowledgeDelta = 5, stressDelta = -4)),
                option("rent_forever", Strings["evt_dana_2005_mortgage_decision_opt_rent_forever"], "📦", "child_education",
                    Effect(investmentsDelta = 20_000L, capitalDelta = -20_000L, knowledgeDelta = 5))
            )
        ))
        put("child_education", commonChildEducationEvent(
            """
            Дочь подрастает, и Дана впервые чувствует, как все её финансовые решения складываются в один большой вопрос: какое детство они могут ей позволить не на словах, а на практике.
            """.trimIndent()
        ))
        commonDanaPoolAndEndings("2005")
    }

    private fun dana2015Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "📱",
            message = Strings["evt_dana_2015_intro_msg"],
            options = listOf(
                option("start_tutoring", Strings["evt_dana_2015_intro_opt_start_tutoring"], "💻", "tutoring_platform",
                    Effect(knowledgeDelta = 3)),
                option("explore_mortgage", Strings["evt_dana_2015_intro_opt_explore_mortgage"], "🏠", "mortgage_decision",
                    Effect(knowledgeDelta = 5))
            )
        ))
        put("tutoring_platform", event(
            id = "tutoring_platform",
            flavor = "💻",
            message = Strings["evt_dana_2015_tutoring_platform_msg"],
            options = listOf(
                option("join_platform", Strings["evt_dana_2015_tutoring_platform_opt_join_platform"], "✅", "tutoring_growth",
                    Effect(capitalDelta = -15_000L, incomeDelta = 60_000L, stressDelta = 14, knowledgeDelta = 6)),
                option("skip_platform", Strings["evt_dana_2015_tutoring_platform_opt_skip_platform"], "👧", "mortgage_decision",
                    Effect(stressDelta = -5))
            )
        ))
        put("tutoring_growth", event(
            id = "tutoring_growth",
            flavor = "📈",
            message = Strings["evt_dana_2015_tutoring_growth_msg"],
            options = listOf(
                option("launch_school", Strings["evt_dana_2015_tutoring_growth_opt_launch_school"], "🎥", MONTHLY_TICK,
                    Effect(capitalDelta = -180_000L, incomeDelta = 95_000L, stressDelta = 18, knowledgeDelta = 10,
                        setFlags = setOf("dana.school.started"))),
                option("stay_quiet", Strings["evt_dana_2015_tutoring_growth_opt_stay_quiet"], "🛡️", "mortgage_decision",
                    Effect(stressDelta = -3, knowledgeDelta = 3))
            )
        ))
        put("mortgage_decision", event(
            id = "mortgage_decision",
            flavor = "🏠",
            message = Strings["evt_dana_2015_mortgage_decision_msg"],
            options = listOf(
                option("take_mortgage", Strings["evt_dana_2015_mortgage_decision_opt_take_mortgage"], "🔑", "child_education",
                    Effect(capitalDelta = -3_000_000L, debtDelta = 12_000_000L, expensesDelta = 95_000L, stressDelta = 24, knowledgeDelta = 8,
                        setFlags = setOf("dana.has.mortgage"))),
                option("save_more", Strings["evt_dana_2015_mortgage_decision_opt_save_more"], "⏳", "child_education",
                    Effect(knowledgeDelta = 6, stressDelta = -4)),
                option("rent_forever", Strings["evt_dana_2015_mortgage_decision_opt_rent_forever"], "📊", "child_education",
                    Effect(capitalDelta = -60_000L, investmentsDelta = 60_000L, knowledgeDelta = 8))
            )
        ))
        put("child_education", commonChildEducationEvent(
            """
            Время выбирать не только школу, а общую модель жизни семьи: платить больше сейчас ради уверенности ребёнка или признать, что любовь родителей не всегда измеряется самой дорогой опцией.
            """.trimIndent()
        ))
        commonDanaPoolAndEndings("2015")
    }

    private fun dana2024Events(): Map<String, GameEvent> = buildMap {
        put("intro", event(
            id = "intro",
            flavor = "🎙️",
            message = Strings["evt_dana_2024_intro_msg"],
            options = listOf(
                option("start_tutoring", Strings["evt_dana_2024_intro_opt_start_tutoring"], "💻", "tutoring_platform",
                    Effect(knowledgeDelta = 4)),
                option("explore_mortgage", Strings["evt_dana_2024_intro_opt_explore_mortgage"], "🏠", "mortgage_decision",
                    Effect(knowledgeDelta = 5))
            )
        ))
        put("tutoring_platform", event(
            id = "tutoring_platform",
            flavor = "📲",
            message = Strings["evt_dana_2024_tutoring_platform_msg"],
            options = listOf(
                option("join_platform", Strings["evt_dana_2024_tutoring_platform_opt_join_platform"], "🚀", "tutoring_growth",
                    Effect(capitalDelta = -30_000L, incomeDelta = 80_000L, stressDelta = 16, knowledgeDelta = 8)),
                option("skip_platform", Strings["evt_dana_2024_tutoring_platform_opt_skip_platform"], "🌿", "mortgage_decision",
                    Effect(stressDelta = -6))
            )
        ))
        put("tutoring_growth", event(
            id = "tutoring_growth",
            flavor = "🎥",
            message = Strings["evt_dana_2024_tutoring_growth_msg"],
            options = listOf(
                option("launch_school", Strings["evt_dana_2024_tutoring_growth_opt_launch_school"], "🏫", MONTHLY_TICK,
                    Effect(capitalDelta = -260_000L, incomeDelta = 140_000L, stressDelta = 20, knowledgeDelta = 12,
                        setFlags = setOf("dana.school.started"))),
                option("stay_quiet", Strings["evt_dana_2024_tutoring_growth_opt_stay_quiet"], "🛡️", "mortgage_decision",
                    Effect(stressDelta = -5, knowledgeDelta = 4))
            )
        ))
        put("mortgage_decision", event(
            id = "mortgage_decision",
            flavor = "🔑",
            message = Strings["evt_dana_2024_mortgage_decision_msg"],
            options = listOf(
                option("take_mortgage", Strings["evt_dana_2024_mortgage_decision_opt_take_mortgage"], "🏡", "child_education",
                    Effect(capitalDelta = -3_400_000L, debtDelta = 14_500_000L, expensesDelta = 120_000L, stressDelta = 22, knowledgeDelta = 8,
                        setFlags = setOf("dana.has.mortgage"))),
                option("save_more", Strings["evt_dana_2024_mortgage_decision_opt_save_more"], "⏳", "child_education",
                    Effect(knowledgeDelta = 6, stressDelta = -5)),
                option("rent_forever", Strings["evt_dana_2024_mortgage_decision_opt_rent_forever"], "📈", "child_education",
                    Effect(capitalDelta = -100_000L, investmentsDelta = 100_000L, knowledgeDelta = 9))
            )
        ))
        put("child_education", commonChildEducationEvent(
            """
            Теперь образование дочери для Даны не просто расход. Это зеркало всего, что она строила столько лет: выдержит ли их семейная система следующий уровень ответственности.
            """.trimIndent()
        ))
        commonDanaPoolAndEndings("2024")
    }

    private fun commonChildEducationEvent(introParagraph: String) = event(
        id = "child_education",
        flavor = "👧",
        message = Strings["evt_dana_2024_child_education_msg"],
        options = listOf(
            option("private_school", Strings["evt_dana_2024_child_education_opt_private_school"], "🎓", MONTHLY_TICK,
                Effect(expensesDelta = 80_000L, stressDelta = 10, knowledgeDelta = 2)),
            option("state_tutor", Strings["evt_dana_2024_child_education_opt_state_tutor"], "✏️", MONTHLY_TICK,
                Effect(expensesDelta = 35_000L, stressDelta = 5, knowledgeDelta = 3)),
            option("state_school", Strings["evt_dana_2024_child_education_opt_state_school"], "🏫", MONTHLY_TICK,
                Effect(stressDelta = -3))
        )
    )

    private fun MutableMap<String, GameEvent>.commonDanaPoolAndEndings(eraLabel: String) {
        put("husband_layoff", event(
            id = "husband_layoff",
            flavor = "😰",
            poolWeight = 10,
            tags = setOf("family", "crisis"),
            message = Strings["evt_dana_2024_husband_layoff_msg"],
            options = listOf(
                option("cut_expenses", Strings["evt_dana_2024_husband_layoff_opt_cut_expenses"], "✂️", MONTHLY_TICK,
                    Effect(expensesDelta = -60_000L, stressDelta = 14, knowledgeDelta = 5)),
                option("use_savings", Strings["evt_dana_2024_husband_layoff_opt_use_savings"], "🐷", MONTHLY_TICK,
                    Effect(capitalDelta = -180_000L, stressDelta = 8)),
                option("more_tutoring", Strings["evt_dana_2024_husband_layoff_opt_more_tutoring"], "💪", MONTHLY_TICK,
                    Effect(incomeDelta = 55_000L, stressDelta = 22))
            )
        ))
        put("normal_life", event(
            id = "normal_life",
            flavor = "🍵",
            poolWeight = 20,
            message = Strings["evt_dana_2024_normal_life_msg"],
            options = listOf(
                option("family_savings", Strings["evt_dana_2024_normal_life_opt_family_savings"], "🐷", MONTHLY_TICK,
                    Effect(stressDelta = -4, knowledgeDelta = 2)),
                option("read_finance", Strings["evt_dana_2024_normal_life_opt_read_finance"], "📖", MONTHLY_TICK,
                    Effect(knowledgeDelta = 7, stressDelta = -2)),
                option("invest_conservative", Strings["evt_dana_2024_normal_life_opt_invest_conservative"], "🏦", MONTHLY_TICK,
                    Effect(capitalDelta = -40_000L, investmentsDelta = 40_000L, knowledgeDelta = 4)),
                option("family_trip", Strings["evt_dana_2024_normal_life_opt_family_trip"], "🌿", MONTHLY_TICK,
                    Effect(capitalDelta = -25_000L, stressDelta = -15))
            )
        ))
        put("ending_own_school", event(
            id = "ending_own_school",
            flavor = "🏆",
            isEnding = true,
            endingType = EndingType.WEALTH,
            message = Strings["evt_dana_2024_ending_own_school_msg"],
            options = emptyList()
        ))
        put("ending_stable_family", event(
            id = "ending_stable_family",
            flavor = "❤️",
            isEnding = true,
            endingType = EndingType.FINANCIAL_STABILITY,
            message = Strings["evt_dana_2024_ending_stable_family_msg"],
            options = emptyList()
        ))
        put("ending_freedom", event(
            id = "ending_freedom",
            flavor = "🌅",
            isEnding = true,
            endingType = EndingType.FINANCIAL_FREEDOM,
            message = Strings["evt_dana_2024_ending_freedom_msg"],
            options = emptyList()
        ))
        put("ending_debt_trap", event(
            id = "ending_debt_trap",
            flavor = "😞",
            isEnding = true,
            endingType = EndingType.PAYCHECK_TO_PAYCHECK,
            message = Strings["evt_dana_2024_ending_debt_trap_msg"],
            options = emptyList()
        ))
        put("ending_bankruptcy", event(
            id = "ending_bankruptcy",
            flavor = "💀",
            isEnding = true,
            endingType = EndingType.BANKRUPTCY,
            message = Strings["evt_dana_2024_ending_bankruptcy_msg"],
            options = emptyList()
        ))
    }

    private fun commonDanaConditionals(
        burnoutIntro: String,
        investmentIntro: String
    ): List<GameEvent> = listOf(
        event(
            id = "debt_crisis",
            priority = 10,
            flavor = "🚨",
            conditions = listOf(cond(DEBT, GT, 0L), cond(CAPITAL, LTE, 120_000L)),
            message = Strings["evt_dana_2024_debt_crisis_msg"],
            options = listOf(
                option("sell_investments", Strings["evt_dana_2024_debt_crisis_opt_sell_investments"], "📉", MONTHLY_TICK,
                    Effect(debtDelta = -200_000L, investmentsDelta = -160_000L, stressDelta = 18)),
                option("ask_parents", Strings["evt_dana_2024_debt_crisis_opt_ask_parents"], "👵", MONTHLY_TICK,
                    Effect(capitalDelta = 150_000L, stressDelta = 22))
            )
        ),
        event(
            id = "burnout_warning",
            priority = 8,
            flavor = "😮‍💨",
            conditions = listOf(cond(STRESS, GTE, 72L)),
            message = Strings["evt_dana_2024_burnout_warning_msg"],
            options = listOf(
                option("take_break", Strings["evt_dana_2024_burnout_warning_opt_take_break"], "🌿", MONTHLY_TICK,
                    Effect(capitalDelta = -90_000L, incomeDelta = -40_000L, stressDelta = -32)),
                option("reduce_tutoring", Strings["evt_dana_2024_burnout_warning_opt_reduce_tutoring"], "📉", MONTHLY_TICK,
                    Effect(incomeDelta = -30_000L, stressDelta = -20))
            )
        ),
        event(
            id = "investment_unlock",
            priority = 5,
            flavor = "💡",
            unique = true,
            conditions = listOf(cond(KNOWLEDGE, GTE, 38L)),
            message = Strings["evt_dana_2024_investment_unlock_msg"],
            options = listOf(
                option("buy_bonds", Strings["evt_dana_2024_investment_unlock_opt_buy_bonds"], "📊", MONTHLY_TICK,
                    Effect(capitalDelta = -200_000L, investmentsDelta = 200_000L, knowledgeDelta = 5)),
                option("skip_bonds", Strings["evt_dana_2024_investment_unlock_opt_skip_bonds"], "🏦", MONTHLY_TICK,
                    Effect())
            )
        ),
        event(
            id = "ending_wealth_trigger",
            priority = 2,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 14_000_000L), Condition.HasFlag("dana.school.started")),
            message = Strings["evt_dana_2024_ending_wealth_trigger_msg"],
            options = listOf(option("claim_wealth", Strings["evt_dana_2024_ending_wealth_trigger_opt_claim_wealth"], "🏆", "ending_own_school"))
        ),
        event(
            id = "ending_freedom_trigger",
            priority = 3,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 7_000_000L), cond(KNOWLEDGE, GTE, 55L)),
            message = Strings["evt_dana_2024_ending_freedom_trigger_msg"],
            options = listOf(option("claim_freedom", Strings["evt_dana_2024_ending_freedom_trigger_opt_claim_freedom"], "🌅", "ending_freedom"))
        ),
        event(
            id = "ending_stability_trigger",
            priority = 4,
            unique = true,
            conditions = listOf(cond(CAPITAL, GTE, 2_500_000L), cond(STRESS, LTE, 48L)),
            message = Strings["evt_dana_2024_ending_stability_trigger_msg"],
            options = listOf(option("claim_stability", Strings["evt_dana_2024_ending_stability_trigger_opt_claim_stability"], "❤️", "ending_stable_family"))
        ),
        event(
            id = "ending_bankruptcy_trigger",
            priority = 100,
            conditions = listOf(cond(CAPITAL, LTE, 0L), cond(STRESS, GTE, 90L)),
            message = Strings["evt_dana_2024_ending_bankruptcy_trigger_msg"],
            options = listOf(option("claim_bankruptcy", Strings["evt_dana_2024_ending_bankruptcy_trigger_opt_claim_bankruptcy"], "💀", "ending_bankruptcy"))
        ),
        event(
            id = "ending_paycheck_trigger",
            priority = 1,
            unique = true,
            conditions = listOf(cond(DEBT, GT, 2_000_000L), cond(CAPITAL, LTE, 100_000L)),
            message = Strings["evt_dana_2024_ending_paycheck_trigger_msg"],
            options = listOf(option("claim_debt_trap", Strings["evt_dana_2024_ending_paycheck_trigger_opt_claim_debt_trap"], "😞", "ending_debt_trap"))
        )
    )
}
