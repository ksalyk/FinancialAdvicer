// package kept as scenarios (not scenarios.characters) — referenced from Scenarios.kt in same package
package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.CurrencyCode
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.endingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.marat90sStoryArc
import kz.fearsom.financiallifev2.scenarios.arcs.regularLifeArc
import kz.fearsom.financiallifev2.scenarios.arcs.storyConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.storyEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.StoryBalance

class Aidar90sScenarioGraph : ScenarioGraph() {

    override val initialPlayerState: PlayerState = PlayerState(
        capital = 25_000_000L,
        income = 7_500_000L,
        expenses = 6_000_000L,
        debt = 0L,
        debtPaymentMonthly = 0L,
        investments = 0L,
        investmentReturnRate = 0.05,
        stress = 55,
        financialKnowledge = 18,
        riskLevel = 35,
        month = 10,
        year = 1993,
        characterId = "aidar_90s",
        eraId = "kz_90s",
        currency = CurrencyCode.RUB,
        flags = setOf()
    )

    override val events: Map<String, GameEvent> = listOf(
        marat90sStoryArc(),
        regularLifeArc("kz_90s"),
        endingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = storyConditionals(
        StoryBalance(
            stabilityCapital = 180_000L,
            freedomCapital = 650_000L,
            wealthCapital = 1_500_000L,
            debtCrisisDebt = 250_000L,
            debtCrisisCapital = 70_000L,
            investmentTicket = 60_000L
        )
    )

    override val eventPool: List<PoolEntry> = storyEventPool("kz_90s")
}
