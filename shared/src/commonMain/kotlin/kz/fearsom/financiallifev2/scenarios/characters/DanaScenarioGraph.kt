package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.endingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.regularLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.storyConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.storyEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.StoryBalance
import kz.fearsom.financiallifev2.scenarios.arcs.zhanar2015StoryArc

class DanaScenarioGraph(eraId: String = "kz_2015") : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 620_000L,
        income = 260_000L,
        expenses = 190_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.08,
        stress = 42,
        financialKnowledge = 28,
        riskLevel = 18,
        month = 6,
        year = 2015,
        characterId = "dana",
        eraId = "kz_2015"
    )

    override val events: Map<String, GameEvent> = listOf(
        zhanar2015StoryArc(),
        regularLifeArc("kz_2015"),
        endingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = storyConditionals(
        StoryBalance(
            stabilityCapital = 1_100_000L,
            freedomCapital = 3_200_000L,
            wealthCapital = 8_000_000L,
            debtCrisisDebt = 1_500_000L,
            debtCrisisCapital = 220_000L,
            investmentTicket = 150_000L
        )
    )

    override val eventPool: List<PoolEntry> = storyEventPool("kz_2015")
}
