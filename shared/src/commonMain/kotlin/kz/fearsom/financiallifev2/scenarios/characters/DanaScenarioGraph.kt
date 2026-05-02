package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.DanaEraAmounts
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.commonDanaConditionals
import kz.fearsom.financiallifev2.scenarios.arcs.danaEraAmounts
import kz.fearsom.financiallifev2.scenarios.arcs.danaEndingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.danaEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.danaInitialState
import kz.fearsom.financiallifev2.scenarios.arcs.danaMainArc
import kz.fearsom.financiallifev2.scenarios.arcs.danaPoolArc

class DanaScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    private val amounts: DanaEraAmounts = danaEraAmounts[eraId]
        ?: error("No DanaEraAmounts for eraId=$eraId")

    override val initialPlayerState: PlayerState = danaInitialState(eraId)

    override val events: Map<String, GameEvent> = listOf(
        danaMainArc(eraId, amounts),
        danaPoolArc(),
        danaEndingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = commonDanaConditionals()

    override val eventPool: List<PoolEntry> = danaEventPool()
}
