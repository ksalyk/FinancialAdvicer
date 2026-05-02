package kz.fearsom.financiallifev2.scenarios.characters

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry
import kz.fearsom.financiallifev2.scenarios.ScenarioGraph
import kz.fearsom.financiallifev2.scenarios.arcs.AidarEraAmounts
import kz.fearsom.financiallifev2.scenarios.arcs.aidarEndingsArc
import kz.fearsom.financiallifev2.scenarios.arcs.aidarEraAmounts
import kz.fearsom.financiallifev2.scenarios.arcs.aidarEventPool
import kz.fearsom.financiallifev2.scenarios.arcs.aidarInitialState
import kz.fearsom.financiallifev2.scenarios.arcs.aidarMainArc
import kz.fearsom.financiallifev2.scenarios.arcs.aidarPoolArc
import kz.fearsom.financiallifev2.scenarios.arcs.buildEvents
import kz.fearsom.financiallifev2.scenarios.arcs.commonAidarConditionals

class AidarScenarioGraph(private val eraId: String = "kz_2024") : ScenarioGraph() {

    private val amounts: AidarEraAmounts = aidarEraAmounts[eraId]
        ?: error("No AidarEraAmounts for eraId=$eraId")

    override val initialPlayerState: PlayerState = aidarInitialState(eraId)

    override val events: Map<String, GameEvent> = listOf(
        aidarMainArc(eraId, amounts),
        aidarPoolArc(),
        aidarEndingsArc()
    ).buildEvents()

    override val conditionalEvents: List<GameEvent> = commonAidarConditionals()

    override val eventPool: List<PoolEntry> = aidarEventPool(eraId)
}
