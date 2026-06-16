package kz.fearsom.financiallifev2.scenarios

import kz.fearsom.financiallifev2.model.EndingType
import kz.fearsom.financiallifev2.model.GameEvent
import kz.fearsom.financiallifev2.model.MONTHLY_TICK
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ScenarioGraphContentTest {

    // Centralized in ScenarioTestCatalog so a new graph is enrolled by editing one list.
    private val allGraphs = ScenarioTestCatalog.graphs()
    private val emptyShellGraphs = ScenarioTestCatalog.emptyShellGraphs()
    private val authoredGraphs = ScenarioTestCatalog.authoredGraphs()

    @Test
    fun `each graph exposes intro event`() {
        allGraphs.forEach { graph ->
            assertNotNull(graph.findEvent("intro"), "Missing intro for ${graph.combo()}")
        }
    }

    @Test
    fun `empty era graphs contain only terminal intro`() {
        emptyShellGraphs.forEach { graph ->
            val intro = graph.findEvent("intro")!!
            assertEquals(setOf("intro"), graph.events.keys)
            assertTrue(intro.isEnding, "Intro should terminate empty scenario for ${graph.combo()}")
            assertTrue(intro.options.isEmpty(), "Empty scenario intro must not expose choices for ${graph.combo()}")
            assertTrue(graph.conditionalEvents.isEmpty(), "Empty scenario must not define conditionals for ${graph.combo()}")
            assertTrue(graph.eventPool.isEmpty(), "Empty scenario must not define pool entries for ${graph.combo()}")
        }
    }

    @Test
    fun `all option transitions resolve to existing events or monthly tick`() {
        allGraphs.forEach { graph ->
            allGraphEvents(graph).forEach { event ->
                event.options.forEach { option ->
                    if (option.next == MONTHLY_TICK) return@forEach
                    assertNotNull(
                        graph.findEvent(option.next),
                        "Missing next event '${option.next}' from '${event.id}' in ${graph.combo()}"
                    )
                }
            }
        }
    }

    @Test
    fun `all scheduled events resolve to existing events`() {
        allGraphs.forEach { graph ->
            allGraphEvents(graph).forEach { event ->
                event.options.mapNotNull { it.effects.scheduleEvent?.eventId }.forEach { scheduledId ->
                    assertNotNull(
                        graph.findEvent(scheduledId),
                        "Missing scheduled event '$scheduledId' in ${graph.combo()}"
                    )
                }
            }
        }
    }

    @Test
    fun `ending events are terminal`() {
        allGraphs.forEach { graph ->
            allGraphEvents(graph)
                .filter { it.isEnding }
                .forEach { ending ->
                    assertTrue(ending.options.isEmpty(), "Ending '${ending.id}' must not expose choices")
                }
        }
    }

    @Test
    fun `authored graphs define a playable normal_life fallback`() {
        authoredGraphs.forEach { graph ->
            val normal = graph.findEvent("normal_life")
            assertNotNull(normal, "Authored graph ${graph.combo()} must define 'normal_life' (engine hard fallback)")
            assertTrue(
                !normal.isEnding && normal.options.isNotEmpty(),
                "'normal_life' must be a non-terminal event with options in ${graph.combo()}"
            )
        }
    }

    @Test
    fun `authored graphs cover all five ending types`() {
        authoredGraphs.forEach { graph ->
            val endingTypes = allGraphEvents(graph)
                .filter { it.isEnding }
                .mapNotNull { it.endingType }
                .toSet()
            assertEquals(
                EndingType.entries.toSet(),
                endingTypes,
                "Authored graph ${graph.combo()} must cover all 5 EndingTypes"
            )
        }
    }

    private fun allGraphEvents(graph: ScenarioGraph): List<GameEvent> =
        graph.events.values + graph.conditionalEvents

    private fun ScenarioGraph.combo(): String =
        "${initialPlayerState.characterId}/${initialPlayerState.eraId}"
}
