package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.amir2024StoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.endingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.regularLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.storyConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.storyEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.StoryBalance

class AsanScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 260_000L,
        income = 520_000L,
        expenses = 255_000L,
        debt = 180_000L,
        debtPaymentMonthly = 30_000L,
        investments = 0L,
        investmentReturnRate = 0.10,
        stress = 38,
        financialKnowledge = 24,
        riskLevel = 22,
        month = 1,
        year = 2024,
        characterId = "asan",
        eraId = "kz_2024"
    )

    override val events: Map<String, GameEvent> = listOf(
        amir2024StoryArc(),
        regularLifeArc("kz_2024"),
        endingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = storyConditionals(
        StoryBalance(
            stabilityCapital = 1_500_000L,
            freedomCapital = 4_500_000L,
            wealthCapital = 12_000_000L,
            debtCrisisDebt = 1_400_000L,
            debtCrisisCapital = 180_000L,
            investmentTicket = 180_000L
        )
    )

    override val eventPool: List<PoolEntry> = storyEventPool("kz_2024")
}
