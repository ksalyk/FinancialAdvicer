package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.admin.ScenarioGraphDto
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.PlayerState
import kz.fearsom.financiallifev2.model.PoolEntry

/**
 * Adapts a DB-backed [ScenarioGraphDto] (a published story) into a [ScenarioGraph]
 * so it runs through the exact same [kz.fearsom.financiallifev2.engine.GameEngine]
 * machinery as code-authored graphs.
 *
 * The DTO already carries the four members [ScenarioGraph] exposes, so this is a
 * straight mapping — the only reshape is `events` list → id-keyed map. Published
 * stories are validated self-contained at publish time (see AdminStoryRoutes +
 * analyzeScenario `selfContained`), so `findEvent("intro")` is guaranteed present.
 */
class DtoScenarioGraph(dto: ScenarioGraphDto) : ScenarioGraph() {
    override val initialPlayerState: PlayerState = dto.initialPlayerState
    override val events: Map<String, GameEvent> = dto.events.associateBy { it.id }
    override val conditionalEvents: List<GameEvent> = dto.conditionalEvents
    override val eventPool: List<PoolEntry> = dto.eventPool
}
