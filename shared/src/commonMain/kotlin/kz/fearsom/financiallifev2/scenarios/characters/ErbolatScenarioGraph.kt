package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.ErbolatEraAmounts
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.commonErbolatConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.erbolatEraAmounts
import kz.fearsom.financiallifev2.scenarios.arcs.erbolatEndingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.erbolatEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.erbolatHubArc
import kz.fearsom.financiallifev2.scenarios.arcs.erbolatInitialState
import kz.fearsom.financiallifev2.scenarios.arcs.erbolatMainArc

class ErbolatScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    private val amounts: ErbolatEraAmounts = erbolatEraAmounts[eraId]
        ?: error("No ErbolatEraAmounts for eraId=$eraId")

    override val initialPlayerState: PlayerState = erbolatInitialState(eraId)

    override val events: Map<String, GameEvent> = listOf(
        erbolatMainArc(eraId, amounts),
        erbolatHubArc(),
        erbolatEndingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = commonErbolatConditionals()

    override val eventPool: List<PoolEntry> = erbolatEventPool()
}
