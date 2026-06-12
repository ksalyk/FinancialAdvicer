package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScenarioGraphContentTest {

    private val graphs = listOf(
        ScenarioGraphFactory.forCharacter("aidar_90s", "kz_90s"),
        ScenarioGraphFactory.forCharacter("aidar", "kz_2005"),
        ScenarioGraphFactory.forCharacter("daniyar", "kz_2005"),
        ScenarioGraphFactory.forCharacter("dana", "kz_2015"),
        ScenarioGraphFactory.forCharacter("asan", "kz_2024")
    )

    @Test
    fun `each graph exposes intro event`() {
        graphs.forEach { graph ->
            assertNotNull(graph.findEvent("intro"), "Missing intro for ${graph.initialPlayerState.characterId}/${graph.initialPlayerState.eraId}")
        }
    }

    @Test
    fun `all option transitions resolve to existing events or monthly tick`() {
        graphs.forEach { graph ->
            allGraphEvents(graph).forEach { event ->
                event.options.forEach { option ->
                    if (option.next == MONTHLY_TICK) return@forEach
                    assertNotNull(
                        graph.findEvent(option.next),
                        "Missing next event '${option.next}' from '${event.id}' in ${graph.initialPlayerState.characterId}/${graph.initialPlayerState.eraId}"
                    )
                }
            }
        }
    }

    @Test
    fun `all scheduled events resolve to existing events`() {
        graphs.forEach { graph ->
            allGraphEvents(graph).forEach { event ->
                event.options.mapNotNull { it.effects.scheduleEvent?.eventId }.forEach { scheduledId ->
                    assertNotNull(
                        graph.findEvent(scheduledId),
                        "Missing scheduled event '$scheduledId' in ${graph.initialPlayerState.characterId}/${graph.initialPlayerState.eraId}"
                    )
                }
            }
        }
    }

    @Test
    fun `ending events are terminal`() {
        graphs.forEach { graph ->
            allGraphEvents(graph)
                .filter { it.isEnding }
                .forEach { ending ->
                    assertTrue(ending.options.isEmpty(), "Ending '${ending.id}' must not expose choices")
                }
        }
    }

    @Test
    fun `normal life pool weight stays above individual scam events`() {
        graphs.forEach { graph ->
            val normalLifeWeight = graph.eventPool.first { it.eventId == "normal_life" }.baseWeight
            val maxScamWeight = graph.eventPool
                .filter { entry -> graph.findEvent(entry.eventId)?.tags?.contains("scam") == true }
                .maxOfOrNull { it.baseWeight } ?: 0

            assertTrue(
                normalLifeWeight > maxScamWeight,
                "normal_life weight should exceed any single scam event in ${graph.initialPlayerState.characterId}/${graph.initialPlayerState.eraId}"
            )
        }
    }

    private fun allGraphEvents(graph: ScenarioGraph): List<GameEvent> =
        graph.events.values + graph.conditionalEvents
}
