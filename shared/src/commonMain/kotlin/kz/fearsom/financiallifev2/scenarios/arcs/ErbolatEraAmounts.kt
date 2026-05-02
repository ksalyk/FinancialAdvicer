package kz.fearsom.financiallifev2.scenarios.arcs

import kz.fearsom.financiallifev2.model.PlayerState

data class ErbolatEraAmounts(
    // ── intro ────────────────────────────────────────────────────────────────
    val introCloseIncome: Long,
    val introCloseExpenses: Long,
    val introCloseStress: Int,
    val introCloseKnowledge: Int,
    val introFightCapital: Long,
    val introFightIncome: Long,
    val introFightStress: Int,

    // ── franchise_offer ──────────────────────────────────────────────────────
    val franchiseTakeCapital: Long,
    val franchiseTakeDebt: Long,
    val franchiseTakeIncome: Long,
    val franchiseTakeExpenses: Long,
    val franchiseTakeKnowledge: Int,
    val franchiseNegotiateCapital: Long,
    val franchiseNegotiateDebt: Long,
    val franchiseNegotiateIncome: Long,

    // ── franchise_result ─────────────────────────────────────────────────────
    val resultThirdDebt: Long,
    val resultThirdIncome: Long,
    val resultThirdExpenses: Long,
    val resultThirdStress: Int,
    val resultMasterDebt: Long,
    val resultMasterIncome: Long,
    val resultMasterStress: Int,
    val resultHoldStress: Int,
    val resultHoldKnowledge: Int,

    // ── ecommerce_pivot ──────────────────────────────────────────────────────
    val kaspiCapital: Long,
    val kaspiIncome: Long,
    val kaspiStress: Int,
    val kaspiKnowledge: Int,
    val websiteCapital: Long,
    val websiteIncome: Long,
    val websiteKnowledge: Int,
    val bothCapital: Long,
    val bothIncome: Long,
    val bothStress: Int,
)

val erbolatEraAmounts: Map<String, ErbolatEraAmounts> = mapOf(
    "kz_2015" to ErbolatEraAmounts(
        introCloseIncome = -220_000L,
        introCloseExpenses = -180_000L,
        introCloseStress = -8,
        introCloseKnowledge = 4,
        introFightCapital = -180_000L,
        introFightIncome = 50_000L,
        introFightStress = 12,

        franchiseTakeCapital = -1_500_000L,
        franchiseTakeDebt = 2_000_000L,
        franchiseTakeIncome = 180_000L,
        franchiseTakeExpenses = 50_000L,
        franchiseTakeKnowledge = 8,
        franchiseNegotiateCapital = -900_000L,
        franchiseNegotiateDebt = 1_000_000L,
        franchiseNegotiateIncome = 90_000L,

        resultThirdDebt = 3_000_000L,
        resultThirdIncome = 220_000L,
        resultThirdExpenses = 180_000L,
        resultThirdStress = 22,
        resultMasterDebt = 8_000_000L,
        resultMasterIncome = 420_000L,
        resultMasterStress = 30,
        resultHoldStress = -6,
        resultHoldKnowledge = 4,

        kaspiCapital = -150_000L,
        kaspiIncome = 150_000L,
        kaspiStress = 4,
        kaspiKnowledge = 10,
        websiteCapital = -350_000L,
        websiteIncome = 90_000L,
        websiteKnowledge = 14,
        bothCapital = -650_000L,
        bothIncome = 220_000L,
        bothStress = 24,
    ),
    "kz_2024" to ErbolatEraAmounts(
        introCloseIncome = -260_000L,
        introCloseExpenses = -220_000L,
        introCloseStress = -10,
        introCloseKnowledge = 5,
        introFightCapital = -250_000L,
        introFightIncome = 60_000L,
        introFightStress = 14,

        franchiseTakeCapital = -2_000_000L,
        franchiseTakeDebt = 2_500_000L,
        franchiseTakeIncome = 220_000L,
        franchiseTakeExpenses = 80_000L,
        franchiseTakeKnowledge = 10,
        franchiseNegotiateCapital = -1_100_000L,
        franchiseNegotiateDebt = 1_200_000L,
        franchiseNegotiateIncome = 110_000L,

        resultThirdDebt = 3_500_000L,
        resultThirdIncome = 280_000L,
        resultThirdExpenses = 230_000L,
        resultThirdStress = 24,
        resultMasterDebt = 10_000_000L,
        resultMasterIncome = 520_000L,
        resultMasterStress = 32,
        resultHoldStress = -8,
        resultHoldKnowledge = 5,

        kaspiCapital = -180_000L,
        kaspiIncome = 170_000L,
        kaspiStress = 2,
        kaspiKnowledge = 12,
        websiteCapital = -550_000L,
        websiteIncome = 120_000L,
        websiteKnowledge = 16,
        bothCapital = -800_000L,
        bothIncome = 260_000L,
        bothStress = 26,
    ),
)

fun erbolatInitialState(eraId: String): PlayerState = when (eraId) {
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
