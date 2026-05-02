package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.asanConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.asanEndingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.asanEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.asanNormalLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.asanStoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents

class AsanScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 200_000L,
        income = 450_000L,
        expenses = 180_000L,
        debt = 120_000L,
        debtPaymentMonthly = 15_000L,
        investments = 0L,
        investmentReturnRate = 0.10,
        stress = 25,
        financialKnowledge = 10,
        riskLevel = 15,
        month = 1,
        year = 2024,
        characterId = "asan",
        eraId = "kz_2024"
    )

    override val events: Map<String, GameEvent> = listOf(
        asanStoryArc(),
        asanNormalLifeArc(),
        asanEndingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = asanConditionals()

    override val eventPool: List<PoolEntry> = asanEventPool()
}
