package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScenarioGraphContentTest {

    // Centralized in ScenarioTestCatalog so a new graph is enrolled in every
    // scenario suite (content + simulation) by editing a single list.
    private val graphs = ScenarioTestCatalog.graphs()

    @Test
    fun `each graph exposes intro event`() {
        graphs.forEach { graph ->
            assertNotNull(graph.findEvent("intro"), "Missing intro for ${graph.initialPlayerState.characterId}/${graph.initialPlayerState.eraId}")
        }
    }

    @Test
    fun `empty era graphs contain only terminal intro`() {
        graphs.forEach { graph ->
            val intro = graph.findEvent("intro")!!
            assertEquals(setOf("intro"), graph.events.keys)
            assertTrue(intro.isEnding, "Intro should terminate empty scenario for ${graph.initialPlayerState.eraId}")
            assertTrue(intro.options.isEmpty(), "Empty scenario intro must not expose choices for ${graph.initialPlayerState.eraId}")
            assertTrue(graph.conditionalEvents.isEmpty(), "Empty scenario must not define conditionals for ${graph.initialPlayerState.eraId}")
            assertTrue(graph.eventPool.isEmpty(), "Empty scenario must not define pool entries for ${graph.initialPlayerState.eraId}")
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

    private fun allGraphEvents(graph: ScenarioGraph): List<GameEvent> =
        graph.events.values + graph.conditionalEvents
}
