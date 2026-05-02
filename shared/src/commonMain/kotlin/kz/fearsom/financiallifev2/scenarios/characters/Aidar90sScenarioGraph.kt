// package kept as scenarios (not scenarios.characters) — referenced from Scenarios.kt in same package
package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.arcs.aidar90sConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.aidar90sEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.aidar90sPoolArc
import kz.fearsom.financiallifev2.scenarios.arcs.aidar90sStoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents

class Aidar90sScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
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

    override val events: Map<String, GameEvent> = listOf(
        aidar90sStoryArc(),
        aidar90sPoolArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = aidar90sConditionals()

    override val eventPool: List<PoolEntry> = aidar90sEventPool()
}
