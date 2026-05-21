package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.endingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.regularLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.ruslan2005StoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.storyConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.storyEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.StoryBalance

class AidarScenarioGraph(eraId: String = "kz_2005") : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 450_000L,
        income = 170_000L,
        expenses = 105_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.07,
        stress = 34,
        financialKnowledge = 24,
        riskLevel = 26,
        month = 1,
        year = 2005,
        characterId = "aidar",
        eraId = "kz_2005",
        currency = CurrencyCode.KZT
    )

    override val events: Map<String, GameEvent> = listOf(
        ruslan2005StoryArc(),
        regularLifeArc("kz_2005"),
        endingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = storyConditionals(
        StoryBalance(
            stabilityCapital = 900_000L,
            freedomCapital = 2_600_000L,
            wealthCapital = 7_000_000L,
            debtCrisisDebt = 1_200_000L,
            debtCrisisCapital = 180_000L,
            investmentTicket = 120_000L
        )
    )

    override val eventPool: List<PoolEntry> = storyEventPool("kz_2005")
}
