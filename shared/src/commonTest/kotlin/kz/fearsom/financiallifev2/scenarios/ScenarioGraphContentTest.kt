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
        ScenarioGraphFactory.forCharacter("aidar", "kz_2015"),
        ScenarioGraphFactory.forCharacter("aidar", "kz_2024"),
        ScenarioGraphFactory.forCharacter("asan", "kz_2024"),
        ScenarioGraphFactory.forCharacter("dana", "kz_2005"),
        ScenarioGraphFactory.forCharacter("dana", "kz_2015"),
        ScenarioGraphFactory.forCharacter("dana", "kz_2024"),
        ScenarioGraphFactory.forCharacter("erbolat", "kz_2015"),
        ScenarioGraphFactory.forCharacter("erbolat", "kz_2024")
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

    private fun allGraphEvents(graph: ScenarioGraph): List<GameEvent> =
        graph.events.values + graph.conditionalEvents
}
