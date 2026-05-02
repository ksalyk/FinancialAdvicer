package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.model.PlayerState

data class DanaEraAmounts(
    // ── intro ────────────────────────────────────────────────────────────────
    val introStartTutoringKnowledge: Int,

    // ── tutoring_platform ────────────────────────────────────────────────────
    val platformJoinCapital: Long,
    val platformJoinIncome: Long,
    val platformJoinStress: Int,
    val platformJoinKnowledge: Int,

    // ── tutoring_growth ──────────────────────────────────────────────────────
    val growthLaunchCapital: Long,
    val growthLaunchIncome: Long,
    val growthLaunchStress: Int,
    val growthLaunchKnowledge: Int,
    val growthStayStress: Int,
    val growthStayKnowledge: Int,

    // ── mortgage_decision ────────────────────────────────────────────────────
    val mortgageDownpayment: Long,
    val mortgageDebt: Long,
    val mortgageExpenses: Long,
    val mortgageTakeStress: Int,
    val mortgageTakeKnowledge: Int,
    val mortgageSaveMoreKnowledge: Int,
    val mortgageSaveMoreStress: Int,
    val mortgageRentAmount: Long,
    val mortgageRentKnowledge: Int,
)

val danaEraAmounts: Map<String, DanaEraAmounts> = mapOf(
    "kz_2005" to DanaEraAmounts(
        introStartTutoringKnowledge = 3,

        platformJoinCapital = 0L,
        platformJoinIncome = 35_000L,
        platformJoinStress = 10,
        platformJoinKnowledge = 5,

        growthLaunchCapital = -120_000L,
        growthLaunchIncome = 70_000L,
        growthLaunchStress = 18,
        growthLaunchKnowledge = 8,
        growthStayStress = -4,
        growthStayKnowledge = 4,

        mortgageDownpayment = -1_000_000L,
        mortgageDebt = 4_500_000L,
        mortgageExpenses = 55_000L,
        mortgageTakeStress = 18,
        mortgageTakeKnowledge = 6,
        mortgageSaveMoreKnowledge = 5,
        mortgageSaveMoreStress = -4,
        mortgageRentAmount = 20_000L,
        mortgageRentKnowledge = 5,
    ),
    "kz_2015" to DanaEraAmounts(
        introStartTutoringKnowledge = 3,

        platformJoinCapital = -15_000L,
        platformJoinIncome = 60_000L,
        platformJoinStress = 14,
        platformJoinKnowledge = 6,

        growthLaunchCapital = -180_000L,
        growthLaunchIncome = 95_000L,
        growthLaunchStress = 18,
        growthLaunchKnowledge = 10,
        growthStayStress = -3,
        growthStayKnowledge = 3,

        mortgageDownpayment = -3_000_000L,
        mortgageDebt = 12_000_000L,
        mortgageExpenses = 95_000L,
        mortgageTakeStress = 24,
        mortgageTakeKnowledge = 8,
        mortgageSaveMoreKnowledge = 6,
        mortgageSaveMoreStress = -4,
        mortgageRentAmount = 60_000L,
        mortgageRentKnowledge = 8,
    ),
    "kz_2024" to DanaEraAmounts(
        introStartTutoringKnowledge = 4,

        platformJoinCapital = -30_000L,
        platformJoinIncome = 80_000L,
        platformJoinStress = 16,
        platformJoinKnowledge = 8,

        growthLaunchCapital = -260_000L,
        growthLaunchIncome = 140_000L,
        growthLaunchStress = 20,
        growthLaunchKnowledge = 12,
        growthStayStress = -5,
        growthStayKnowledge = 4,

        mortgageDownpayment = -3_400_000L,
        mortgageDebt = 14_500_000L,
        mortgageExpenses = 120_000L,
        mortgageTakeStress = 22,
        mortgageTakeKnowledge = 8,
        mortgageSaveMoreKnowledge = 6,
        mortgageSaveMoreStress = -5,
        mortgageRentAmount = 100_000L,
        mortgageRentKnowledge = 9,
    ),
)

fun danaInitialState(eraId: String): PlayerState = when (eraId) {
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
